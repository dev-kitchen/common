package com.linkedout.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.exception.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceMessageClient {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * 서비스 간 통신을 위한 리액티브 메시지 전송 메서드
	 *
	 * @param queueName    대상 서비스 큐 이름
	 * @param requestData  요청 데이터
	 * @param responseType 응답 타입 클래스
	 * @return 응답 데이터를 포함한 Mono
	 */
	public <T, R> Mono<R> sendMessage(String queueName, T requestData, Class<R> responseType) {
		// 상관관계 ID 생성
		String correlationId = UUID.randomUUID().toString();

		// 비동기 응답 처리를 위한 Sink 생성
		Sinks.One<Object> responseSink = Sinks.one();

		// 응답 핸들러 맵에 저장 (글로벌 응답 핸들러 맵 필요)
		ResponseRegistry.registerHandler(correlationId, responseSink);

		return Mono.fromCallable(() -> {
				// 논블로킹 방식으로 메시지 전송
				rabbitTemplate.convertAndSend(
					queueName,
					requestData,
					message -> {
						message.getMessageProperties().setCorrelationId(correlationId);
						return message;
					}
				);
				return correlationId;
			})
			.subscribeOn(Schedulers.boundedElastic()) // IO 작업은 boundedElastic 스케줄러에서 실행
			.then(responseSink.asMono())
			.map(response -> objectMapper.convertValue(response, responseType))
			.timeout(Duration.ofSeconds(30))
			.doFinally(signal -> ResponseRegistry.removeHandler(correlationId))
			.onErrorResume(ex -> {
				log.error("서비스 간 통신 오류 발생", ex);
				return Mono.error(new InternalServerErrorException("서비스 통신 오류: " + ex.getMessage()));
			});
	}
}