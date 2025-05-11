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
	 * Redis 데이터 작업을 비동기적으로 수행하기 위한 리액티브 Redis 템플릿입니다.
	 * 이 변수는 리액티브 프로그래밍 패러다임을 지원하여 Redis와의 논블로킹 상호작용을 용이하게 합니다.
	 * Redis Pub/Sub 채널에 메시지를 발행하고, 값을 읽고, 애플리케이션 내에서 다른 Redis 작업을
	 * 수행하는 데 사용됩니다.
	 */
	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	/**
	 * 리액티브 모드에서 Redis Pub/Sub 메시지 구독을 관리하는 데 사용되는 ReactiveRedisMessageListenerContainer
	 * 인스턴스입니다. 이 컨테이너는 Redis 채널을 구독하고 메시지를 리액티브하게 처리할 수 있게 합니다.
	 * 제공된 ReactiveRedisConnectionFactory를 통해 초기화됩니다.
	 */
	private final ReactiveRedisMessageListenerContainer listenerContainer;

	/**
	 * Redis Pub/Sub 채널 이름을 구성하는 데 사용되는 정적 접두사입니다.
	 * <p>
	 * 이 접두사는 상관 ID에 추가되어 마이크로서비스와 대기 중인 클라이언트 간의
	 * 통신을 가능하게 하는 고유한 채널 이름을 생성합니다.
	 * 구성된 채널 이름은 특정 요청-응답 흐름과 관련된 메시지를 발행하고
	 * 구독하는 데 사용됩니다.
	 */
	private static final String REDIS_CHANNEL_PREFIX = "api-gateway:response-channel:";

	/**
	 * 리액티브 Redis Pub/Sub 시스템을 통해 데이터를 기다릴 때
	 * 응답을 기다리는 최대 시간을 정의합니다.
	 * <p>
	 * 이 타임아웃은 특정 상관 ID를 사용하여 비동기적으로 응답을 기다리는
	 * 시나리오에 적용됩니다. 이 기간 내에 응답을 받지 못하면
	 * 대기 작업이 타임아웃 오류와 함께 종료됩니다.
	 * <p>
	 * 이 상수는 시스템이 무기한 대기하는 것을 방지하고
	 * 클라이언트에게 예측 가능한 응답 시간을 제공합니다.
	 */
	private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

	private final ObjectMapper objectMapper;


	/**
	 * ApiMessageResponseHandler 클래스의 생성자입니다.
	 * Redis 템플릿, 리스너 컨테이너, 객체 매퍼를 초기화합니다.
	 *
	 * @param reactiveRedisTemplate Redis 작업에 사용되는 ReactiveRedisTemplate
	 * @param connectionFactory     리스너 컨테이너를 생성하는 데 사용되는 ReactiveRedisConnectionFactory
	 * @param objectMapper          JSON 직렬화 및 역직렬화에 사용되는 ObjectMapper
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
	 * MessageResponseHandlerService를 초기화합니다.
	 * 이 메소드는 빈의 속성이 초기화된 후 자동으로 호출됩니다.
	 * 서비스의 성공적인 초기화를 나타내는 로그 메시지를 기록합니다.
	 */
	@PostConstruct
	public void init() {
		log.info("MessageResponseHandlerService 초기화 완료");
	}


	/**
	 * 서비스가 종료될 때 리소스를 정리하고 필요한 종료 작업을 수행합니다.
	 * 이 메소드는 빈이 애플리케이션 컨텍스트에서 제거되기 전에 호출됩니다.
	 * <p>
	 * 리스너 컨테이너를 제거하고 MessageResponseHandlerService의 종료를
	 * 나타내는 로그 메시지를 기록합니다.
	 */
	@PreDestroy
	public void destroy() {
		listenerContainer.destroy();
		log.info("MessageResponseHandlerService 종료");
	}


	/**
	 * 주어진 correlationId와 관련된 응답을 Redis 채널을 구독하고 메시지를 수신하여 기다립니다.
	 * 응답은 ApiResponseData 객체로 역직렬화되어 reactive Mono로 반환됩니다.
	 *
	 * @param correlationId 요청과 응답을 연결하는데 사용되는 고유 식별자
	 * @return 유효한 메시지가 수신되면 역직렬화된 ApiResponseData 객체를 발행하는 Mono,
	 * 응답 시간이 초과되거나 다른 문제가 발생하면 에러 신호를 발행
	 */
	public Mono<ApiResponseData> awaitResponse(String correlationId) {
		String listenerId = UUID.randomUUID().toString();
		String channelName = REDIS_CHANNEL_PREFIX + correlationId;
		ChannelTopic topic = new ChannelTopic(channelName);

		log.info("응답 대기 시작: correlationId={}, listenerId={}", correlationId, listenerId);

		// 핵심 패턴: 구독을 시작하는 대신 변환 연산자만 사용
		return Mono.fromDirect(
				listenerContainer
					.receive(topic)
					.mapNotNull(message -> {
						try {
							return objectMapper.readValue(message.getMessage(), ApiResponseData.class);
						} catch (JsonProcessingException e) {
							log.error("JSON 파싱 오류: {}", e.getMessage(), e);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.next()
			)
			.doOnSuccess(responseData -> log.info("채널에서 응답 수신: correlationId={}, listenerId={}", correlationId, listenerId))
			.doFinally(signalType -> log.info(
				"응답 처리 완료: correlationId={}, listenerId={}, signalType={}",
				correlationId,
				listenerId,
				signalType))
			.timeout(RESPONSE_TIMEOUT);
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
