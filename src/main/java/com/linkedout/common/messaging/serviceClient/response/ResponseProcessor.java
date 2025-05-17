package com.linkedout.common.messaging.serviceClient.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.exception.BadRequestException;
import com.linkedout.common.exception.InternalServerErrorException;
import com.linkedout.common.messaging.MessageResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResponseProcessor {
	private final ObjectMapper objectMapper;
	private final MessageResponseHandler messageResponseHandler;

	/**
	 * 서비스 응답을 처리하고 적절한 타입으로 변환합니다.
	 */
	public <R> Mono<R> processResponse(String correlationId, Class<R> responseType) {
		return messageResponseHandler.awaitResponse(correlationId)
			.flatMap(response -> {
				if (response.getError() != null) {
					return Mono.error(new BadRequestException(response.getError()));
				}

				if (response.getPayload() == null) {
					return Mono.empty(); // 값 없이 완료 처리
				}

				try {
					R convertedResponse = objectMapper.convertValue(response.getPayload(), responseType);
					return Mono.just(convertedResponse);
				} catch (Exception e) {
					log.error("응답 변환 오류: {}", e.getMessage(), e);
					return Mono.error(new InternalServerErrorException("응답 변환 중 오류 발생"));
				}
			});
	}
}