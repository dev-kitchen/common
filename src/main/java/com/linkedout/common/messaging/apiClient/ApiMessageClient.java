package com.linkedout.common.messaging.apiClient;

import com.linkedout.common.messaging.apiClient.response.ApiResponseFactory;
import com.linkedout.common.model.dto.BaseApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;


/**
 * ApiMessageClient는 외부 HTTP 요청을 내부 마이크로서비스로 전달하고 그 결과를 반환하는 역할을 합니다.
 * API Gateway가 수신한 요청을 RabbitMQ 메시지로 변환하고, 적합한 내부 서비스로 라우팅 처리합니다.
 * <p>
 * 이 클래스는 다음을 포함하여 요청과 응답을 처리합니다:
 * - HTTP 요청을 기반으로 RabbitMQ 메시지 생성
 * - 메시지를 적합한 내부 마이크로서비스로 전달
 * - 내부 서비스 처리 응답 반환
 * - 발생 가능한 오류 상황 처리
 * <p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiMessageClient {
	private final ApiMessageSender requestSender;
	private final ApiResponseFactory responseFactory;

	/**
	 * API Gateway에서 외부 요청을 내부 마이크로서비스로 전달하는 메서드입니다.
	 * HTTP 요청을 수신하여 RabbitMQ 메시지로 변환하고, 해당하는 내부 서비스로 라우팅합니다.
	 * exchange의 요청 경로와 메타데이터를 분석하여 적절한 마이크로서비스로 자동 라우팅됩니다.
	 *
	 * @param exchange 현재 HTTP 요청과 응답 컨텍스트를 나타내는 ServerWebExchange
	 * @return 내부 서비스의 처리 결과를 포함하는 BaseApiResponse를 담은 ResponseEntity를 감싸는 Mono,
	 * 오류 발생 시 적절한 오류 응답 반환
	 */
	public <T> Mono<ResponseEntity<BaseApiResponse<T>>> sendMessage(ServerWebExchange exchange) {
		return requestSender.prepareAndSendMessage(exchange)
			.map(responseFactory::<T>createResponseEntity)
			.onErrorResume(TimeoutException.class, e -> {
				log.error("서비스 응답 타임아웃", e);
				return Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "서비스 응답 타임아웃"));
			});
	}

	/**
	 * 검증된 요청 본문을 사용하여 메시지를 전송하는 오버로드된 메서드입니다.
	 *
	 * @param exchange      현재 HTTP 요청과 응답 컨텍스트
	 * @param validatedBody 이미 검증된 요청 본문 객체
	 * @return 내부 서비스의 처리 결과
	 */
	public <T, R> Mono<ResponseEntity<BaseApiResponse<T>>> sendMessage(ServerWebExchange exchange, R validatedBody) {
		return requestSender.prepareAndSendMessage(exchange, validatedBody)
			.map(responseFactory::<T>createResponseEntity)
			.onErrorResume(TimeoutException.class, e -> {
				log.error("서비스 응답 타임아웃", e);
				return Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "서비스 응답 타임아웃"));
			});
	}
}