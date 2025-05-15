package com.linkedout.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
 * ServiceMessageResponseHandler 클래스는 비동기 서비스 응답 메시징을 처리합니다.
 * RabbitMQ를 통해 외부 서비스로부터 받은 응답을 Redis 채널에 발행하고
 * 지정된 Redis 채널에서 응답을 수신하여 처리하도록 설계되었습니다.
 * 이를 통해 마이크로서비스 간의 메시지 기반 통신을 용이하게 합니다.
 * <p>
 * 주요 기능:
 * - RabbitMQ 큐에서 서비스 응답 수신
 * - 수신된 응답을 소비자가 접근할 수 있도록 Redis 채널에 발행
 * - Redis 채널에서 응답 대기 및 처리
 */
@Service
@Slf4j
public class ServiceMessageResponseHandler {

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
	private final ReactiveRedisMessageListenerContainer listenerContainer;
	private final ObjectMapper objectMapper;
	private final ServiceIdentifier serviceIdentifier;

	private static final String REDIS_CHANNEL_PREFIX = "service:response-channel:";
	private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

	/**
	 * ServiceMessageResponseHandler 생성자.
	 * 서비스 메시지 처리에 필요한 컴포넌트들을 초기화합니다.
	 * Reactive Redis Template, Redis Message Listener Container, Object Mapper, Service Identifier 등이 포함됩니다.
	 *
	 * @param reactiveRedisTemplate Redis 메시지 발행 및 구독에 사용되는 reactive Redis 템플릿
	 * @param connectionFactory     Redis 연결을 생성하기 위한 커넥션 팩토리
	 * @param objectMapper          객체와 JSON 간 변환에 사용되는 매퍼
	 * @param serviceIdentifier     큐 이름, 라우팅 키 등 서비스별 상세 정보를 제공하는 식별자
	 */
	public ServiceMessageResponseHandler(
		ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
		ReactiveRedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper,
		ServiceIdentifier serviceIdentifier) {
		this.reactiveRedisTemplate = reactiveRedisTemplate;
		this.listenerContainer = new ReactiveRedisMessageListenerContainer(connectionFactory);
		this.objectMapper = objectMapper;
		this.serviceIdentifier = serviceIdentifier;
	}

	/**
	 * 주어진 correlationId로 식별되는 Redis 채널에서 응답 메시지를 대기합니다.
	 * 이 메서드는 지정된 채널을 수신하고 메시지를 역직렬화하여
	 * reactive Mono 객체로 반환합니다. 타임아웃 전까지 메시지가 수신되지 않으면
	 * 타임아웃 에러와 함께 작업이 실패합니다.
	 *
	 * @param correlationId 서비스 요청과 연관된 고유 식별자.
	 *                      응답을 수신할 Redis 채널 이름을 구성하는데 사용됩니다.
	 * @return {@code ServiceMessageDTO<?>} 객체를 감싸는 Mono.
	 * 대상 Redis 채널에서 역직렬화된 응답 메시지를 나타냅니다.
	 */
	public Mono<ServiceMessageDTO<?>> awaitResponse(String correlationId) {
		String listenerId = UUID.randomUUID().toString();
		String channelName = REDIS_CHANNEL_PREFIX + correlationId;
		ChannelTopic topic = new ChannelTopic(channelName);

		log.debug("서비스 응답 대기 시작: correlationId={}, listenerId={}", correlationId, listenerId);


		@SuppressWarnings({"unchecked", "rawtypes"})
		Mono<ServiceMessageDTO<?>> result = (Mono) Mono.fromDirect(
				listenerContainer.receive(topic)
					.mapNotNull(message -> {
						try {
							String json = message.getMessage();
							return objectMapper.readValue(json, ServiceMessageDTO.class);
						} catch (JsonProcessingException e) {
							log.error("JSON 파싱 오류: {}", e.getMessage(), e);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.next()
			)
			.doOnSuccess(responseData -> {
				log.debug("채널에서 서비스 응답 수신: correlationId={}", correlationId);
			})
			.timeout(RESPONSE_TIMEOUT)
			.doFinally(signalType -> {
				log.debug("서비스 응답 처리 완료: correlationId={}, signalType={}",
					correlationId, signalType);
			});

		return result;
	}


	/**
	 * RabbitMQ를 통해 수신된 서비스 응답 메시지를 처리하여
	 * JSON으로 직렬화하고 메시지의 상관 ID를 기반으로
	 * 특정 Redis 채널에 발행합니다.
	 *
	 * @param responseMessage 상관 ID, 송신자 서비스, 작업 유형,
	 *                        페이로드 및 기타 메타데이터가 포함된 서비스 응답 메시지
	 */
	@RabbitListener(queues = "#{serviceIdentifier.responseQueueName}")
	public void handleServiceResponse(ServiceMessageDTO<?> responseMessage) {
		String correlationId = responseMessage.getCorrelationId();
		log.debug("서비스 응답 수신: correlationId={}, sender={}",
			correlationId, responseMessage.getSenderService());

		try {
			String channelName = REDIS_CHANNEL_PREFIX + correlationId;
			String jsonResponse = objectMapper.writeValueAsString(responseMessage);
			reactiveRedisTemplate.convertAndSend(channelName, jsonResponse)
				.subscribe(count ->
					log.debug("Redis 채널에 서비스 응답 발행: channel={}, subscribers={}",
						channelName, count)
				);
		} catch (JsonProcessingException e) {
			log.error("서비스 응답 직렬화 오류: {}", e.getMessage(), e);
		}
	}
}
