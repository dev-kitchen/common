package com.linkedout.common.messaging.serviceClient;

import com.linkedout.common.messaging.serviceClient.response.ResponseProcessor;
import com.linkedout.common.exception.BadRequestException;
import com.linkedout.common.exception.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceMessageClient {
	private final ServiceMessageCreator messageCreator;
	private final ServiceMessageSender messageSender;
	private final ResponseProcessor responseProcessor;

	/**
	 * 대상 서비스로 메시지를 전송하고 비동기 응답을 기다립니다.
	 *
	 * @param <T>           요청 페이로드의 타입
	 * @param <R>           응답 페이로드의 타입
	 * @param targetService 메시지를 전송할 대상 서비스의 식별자
	 * @param operation     대상 서비스가 실행해야 할 작업 이름
	 * @param requestData   서비스 메시지의 일부로 전송될 데이터 페이로드
	 * @param responseType  응답 페이로드 타입의 클래스
	 * @return 성공 시 타입 R의 응답 페이로드를 방출하는 Mono.
	 * 응답 과정이나 처리 중에 오류가 발생하면 적절한 오류가 방출됩니다.
	 */
	public <T, R> Mono<R> sendMessage(String targetService, String operation, T requestData, Class<R> responseType) {
		String correlationId = UUID.randomUUID().toString();

		return messageCreator.createMessage(correlationId, targetService, operation, requestData)
			.flatMap(message -> messageSender.send(message, targetService, correlationId))
			.flatMap(sent -> responseProcessor.processResponse(correlationId, responseType))
			.timeout(Duration.ofSeconds(30))
			.onErrorResume(TimeoutException.class, e -> {
				log.error("서비스 응답 타임아웃: targetService={}, operation={}", targetService, operation);
				return Mono.error(new InternalServerErrorException("서비스 응답 타임아웃 발생"));
			})
			.onErrorResume(e -> {
				if (e instanceof BadRequestException || e instanceof InternalServerErrorException) {
					return Mono.error(e);
				}
				log.error("메시지 전송 오류: {}", e.getMessage(), e);
				return Mono.error(new InternalServerErrorException("메시지 전송 중 오류 발생"));
			});
	}
}