package com.linkedout.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.dto.ServiceMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

@Service
@Slf4j
public class ServiceMessageResponseHandler {

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
	private final ReactiveRedisMessageListenerContainer listenerContainer;
	private final ObjectMapper objectMapper;
	private final ServiceIdentifier serviceIdentifier;
	
	private static final String REDIS_CHANNEL_PREFIX = "service:response-channel:";
	private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

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

	// 특정 correlationId에 대한 응답을 기다리는 메서드
	public Mono<ServiceMessageDTO<?>> awaitResponse(String correlationId) {
		String listenerId = UUID.randomUUID().toString();
		String channelName = REDIS_CHANNEL_PREFIX + correlationId;
		ChannelTopic topic = new ChannelTopic(channelName);

		log.debug("서비스 응답 대기 시작: correlationId={}, listenerId={}", correlationId, listenerId);

		Sinks.One<ServiceMessageDTO<?>> sink = Sinks.one();

		var disposable = listenerContainer.receive(topic)
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
			.take(1)
			.subscribe(responseData -> {
				log.debug("채널에서 서비스 응답 수신: correlationId={}", correlationId);
				sink.tryEmitValue(responseData);
			});

		return sink.asMono()
			.timeout(RESPONSE_TIMEOUT)
			.doFinally(signalType -> {
				log.debug("서비스 응답 처리 완료: correlationId={}, signalType={}",
					correlationId, signalType);
				disposable.dispose();
			});
	}

	/**
	 * 현재 서비스의 응답 큐에서 메시지를 수신하고 처리하는 메서드
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