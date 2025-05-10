package com.linkedout.common.messaging;

import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ApiResponseData;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 마이크로서비스 응답을 비동기적으로 처리하는 서비스
 *
 * <p>이 서비스는 RabbitMQ를 통해 마이크로서비스로부터 받은 응답을 처리하고, 해당 응답을 기다리고 있는 클라이언트 요청과 연결시켜 주는 역할을 합니다. 리액티브
 * 프로그래밍 방식으로 구현되어 있어 비동기 처리가 가능합니다.
 *
 * <p>레디스를 사용하여 분산 환경에서도 응답을 처리할 수 있도록 구현되어 있습니다.
 *
 * <p>{@code @Service}: - Spring이 이 클래스를 서비스 컴포넌트로 인식하고 Bean으로 등록하도록 하는 애노테이션 - 비즈니스 로직을 처리하는 컴포넌트임을
 * 나타냄
 */
@Service
public class MessageResponseHandler {
	/**
	 * 응답 핸들러 맵 (메모리)
	 *
	 * <p>correlationId를 키로 하고, 해당 요청에 대한 응답을 기다리는 Sink를 값으로 가지는 맵입니다. ConcurrentHashMap을 사용하여 멀티스레드
	 * 환경에서의 동시성 문제를 해결합니다.
	 */
	private final Map<String, Sinks.One<ApiResponseData>> responseHandlers =
		new ConcurrentHashMap<>();

	/**
	 * Redis 템플릿 - 분산 환경에서 상태 공유를 위해 사용
	 */
	private final ReactiveRedisTemplate<String, ApiResponseData> reactiveRedisTemplate;

	/**
	 * Redis 키 접두사 - 레디스에 저장될 키의 구분을 위한 접두사
	 */
	private static final String REDIS_KEY_PREFIX = "api-gateway:response:";

	/**
	 * 생성자 - ReactiveRedisTemplate 주입
	 *
	 * @param reactiveRedisTemplate Redis 작업을 위한 리액티브 템플릿
	 */
	public MessageResponseHandler(
		ReactiveRedisTemplate<String, ApiResponseData> reactiveRedisTemplate) {
		this.reactiveRedisTemplate = reactiveRedisTemplate;
	}

	/**
	 * 특정 correlationId에 대한 응답을 기다리는 Mono를 반환하는 메서드
	 *
	 * <p>이 메서드는 클라이언트 요청과 연관된 correlationId로 Sink를 생성하고, 이를 맵에 저장한 후, 해당 Sink를 통해 응답을 기다리는 Mono를
	 * 반환합니다. 또한 레디스를 사용하여 다른 인스턴스에서 응답을 처리할 수 있도록 구독도 설정합니다.
	 *
	 * @param correlationId 요청과 응답을 연결하는 상관관계 ID
	 * @return 응답 데이터를 포함하는 Mono
	 */
	public Mono<ApiResponseData> awaitResponse(String correlationId) {
		// Sinks.one()을 사용하여 단일 값 발행이 가능한 Sink 생성
		Sinks.One<ApiResponseData> sink = Sinks.one();
		// 맵에 correlationId와 Sink를 저장
		responseHandlers.put(correlationId, sink);

		// Redis에서 해당 키에 대한 응답이 이미 있는지 확인
		String redisKey = REDIS_KEY_PREFIX + correlationId;
		Mono<ApiResponseData> redisMono = reactiveRedisTemplate.opsForValue().get(redisKey);

		// Redis에 응답 설정을 위한 Mono와 로컬 Sink의 Mono를 병합
		return Mono.firstWithSignal(
				redisMono
					.publishOn(Schedulers.boundedElastic())
					.doOnNext(
						data -> {
							// Redis에서 응답을 찾았다면 Sink에 발행하고 Redis에서 키 삭제
							sink.tryEmitValue(data);
							reactiveRedisTemplate.delete(redisKey).subscribe();
						}),
				sink.asMono())
			.timeout(Duration.ofSeconds(30))
			.publishOn(Schedulers.boundedElastic())
			.doFinally(
				signalType -> {
					// 처리 완료 후 맵에서 항목 제거 및 Redis 키 삭제
					responseHandlers.remove(correlationId);
					reactiveRedisTemplate.delete(redisKey).subscribe();
				});
	}

	/**
	 * RabbitMQ 응답 큐에서 메시지를 수신하고 처리하는 메서드
	 *
	 * <p>이 메서드는 RabbitMQ의 응답 큐에서 메시지를 수신하고, 해당 메시지의 correlationId를 사용하여 올바른 Sink를 찾아 응답을 전달하거나 Redis에
	 * 저장합니다.
	 *
	 * @param responseData 마이크로서비스로부터 받은 응답 데이터
	 */
	@RabbitListener(queues = RabbitMQConstants.GATEWAY_QUEUE)
	@ConditionalOnProperty(
		name = "spring.rabbitmq.enabled",
		havingValue = "true",
		matchIfMissing = true)
	public void handleResponse(ApiResponseData responseData) {
		// 응답 데이터에서 correlationId 추출
		String correlationId = responseData.getCorrelationId();
		// correlationId를 사용하여 맵에서 해당 Sink 찾기
		Sinks.One<ApiResponseData> sink = responseHandlers.get(correlationId);

		if (sink != null) {
			// 로컬 인스턴스에 Sink가 있으면 응답 데이터 직접 발행
			sink.tryEmitValue(responseData);
		} else {
			// 로컬 인스턴스에 Sink가 없으면 Redis에 저장 (다른 인스턴스용)
			String redisKey = REDIS_KEY_PREFIX + correlationId;
			reactiveRedisTemplate
				.opsForValue()
				.set(redisKey, responseData, Duration.ofSeconds(30))
				.subscribe();
		}
	}
}

