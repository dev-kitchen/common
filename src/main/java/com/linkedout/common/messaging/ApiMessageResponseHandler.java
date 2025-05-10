package com.linkedout.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ApiResponseData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 마이크로서비스 응답을 Redis Pub/Sub만을 통해 비동기적으로 처리하는 서비스
 *
 * <p>이 서비스는 RabbitMQ를 통해 마이크로서비스로부터 받은 응답을 처리하고, Redis Pub/Sub을 사용하여 해당 응답을 기다리고 있는 모든 클라이언트 요청과
 * 연결시켜 주는 역할을 합니다. 모든 상태 관리는 Redis에서만 이루어집니다.
 */
@Slf4j
@Service
public class ApiMessageResponseHandler {

	/**
	 * Redis 템플릿 - 메시지 발행을 위해 사용
	 */
	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	/**
	 * Redis Pub/Sub 리스너 컨테이너
	 */
	private final ReactiveRedisMessageListenerContainer listenerContainer;

	/**
	 * ObjectMapper - JSON 직렬화/역직렬화를 위해 사용
	 */
	private final ObjectMapper objectMapper;

	/**
	 * Redis Pub/Sub 채널 접두사
	 */
	private static final String REDIS_CHANNEL_PREFIX = "api-gateway:response-channel:";

	/**
	 * 응답 만료 시간 (초)
	 */
	private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

	/**
	 * 생성자
	 */
	public ApiMessageResponseHandler(
		ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
		ReactiveRedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper) {
		this.reactiveRedisTemplate = reactiveRedisTemplate;
		this.listenerContainer = new ReactiveRedisMessageListenerContainer(connectionFactory);
		this.objectMapper = objectMapper;
	}

	/**
	 * 초기화 메서드 - 서비스 시작 시 호출
	 */
	@PostConstruct
	public void init() {
		log.info("MessageResponseHandlerService 초기화 완료");
	}

	/**
	 * 종료 메서드 - 서비스 종료 시 호출
	 */
	@PreDestroy
	public void destroy() {
		listenerContainer.destroy();
		log.info("MessageResponseHandlerService 종료");
	}

	/**
	 * 특정 correlationId에 대한 응답을 기다리는 Mono를 반환하는 메서드
	 *
	 * <p>이 메서드는 Redis Pub/Sub 채널을 구독하고, 응답이 도착하면 해당 응답을 받는 Mono를 반환합니다. 모든 상태는 Redis Pub/Sub 시스템 내에서만
	 * 관리됩니다.
	 *
	 * @param correlationId 요청과 응답을 연결하는 상관관계 ID
	 * @return 응답 데이터를 포함하는 Mono
	 */
	public Mono<ApiResponseData> awaitResponse(String correlationId) {

		// 고유한 리스너 ID 생성 (여러 인스턴스에서 동일한 correlationId에 대한 구독을 구분하기 위함)
		String listenerId = UUID.randomUUID().toString();
		String channelName = REDIS_CHANNEL_PREFIX + correlationId;
		ChannelTopic topic = new ChannelTopic(channelName);

		log.info("응답 대기 시작: correlationId={}, listenerId={}", correlationId, listenerId);

		// Sink 생성 - 결과를 전달하기 위한 리액티브 컴포넌트
		Sinks.One<ApiResponseData> sink = Sinks.one();

		// 채널 구독 설정
		var disposable =
			listenerContainer
				.receive(topic)
				.mapNotNull(
					message -> {
						try {
							// Redis 메시지를 ApiResponseData로 변환
							String json = message.getMessage();
							return objectMapper.readValue(json, ApiResponseData.class);
						} catch (JsonProcessingException e) {
							log.error("JSON 파싱 오류: {}", e.getMessage(), e);
							return null;
						}
					})
				.filter(Objects::nonNull)
				.take(1) // 첫 번째 메시지만 처리
				.subscribe(
					responseData -> {
						log.info(
							"채널에서 응답 수신: correlationId={}, listenerId={}", correlationId, listenerId);
						sink.tryEmitValue(responseData);
					});

		// 타임아웃 설정 및 리소스 정리 설정
		return sink.asMono()
			.timeout(RESPONSE_TIMEOUT)
			.doFinally(
				signalType -> {
					log.info(
						"응답 처리 완료: correlationId={}, listenerId={}, signalType={}",
						correlationId,
						listenerId,
						signalType);
					// 구독 취소
					disposable.dispose();
				});
	}

	/**
	 * RabbitMQ 응답 큐에서 메시지를 수신하고 처리하는 메서드
	 *
	 * <p>이 메서드는 RabbitMQ의 응답 큐에서 메시지를 수신하고, Redis Pub/Sub 채널에 응답을 발행(publish)합니다.
	 *
	 * @param responseData 마이크로서비스로부터 받은 응답 데이터
	 */
	@RabbitListener(queues = RabbitMQConstants.GATEWAY_QUEUE)
	@ConditionalOnProperty(
		name = "spring.rabbitmq.enabled",
		havingValue = "true",
		matchIfMissing = true)
	public void handleResponse(ApiResponseData responseData) {
		String correlationId = responseData.getCorrelationId();
		String channelName = REDIS_CHANNEL_PREFIX + correlationId;

		log.info(
			"응답 수신: correlationId={}, statusCode={}, body={}",
			correlationId,
			responseData.getStatusCode(),
			responseData.getBody());

		try {
			// Redis Pub/Sub 채널에 응답 발행
			String jsonResponse = objectMapper.writeValueAsString(responseData);
			reactiveRedisTemplate
				.convertAndSend(channelName, jsonResponse)
				.subscribe(
					subscriberCount ->
						log.info(
							"Redis 채널에 응답 발행 완료: channel={}, subscribers={}",
							channelName,
							subscriberCount));
		} catch (JsonProcessingException e) {
			log.error("응답 직렬화 오류: {}", e.getMessage(), e);
		}
	}
}
