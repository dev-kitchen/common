package com.linkedout.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ApiRequestData;
import com.linkedout.common.dto.ApiResponseData;
import com.linkedout.common.dto.BaseApiResponse;
import com.linkedout.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * ApiMessageClient는 API 요청을 처리하고 RabbitMQ를 사용하여 메시징 기반의 서비스와 상호 작용하기 위한 추상 클래스입니다.
 * 이 클래스는 HTTP 요청을 라우팅 키에 매핑하고, 메시지를 RabbitMQ에 전송한 뒤 비동기적으로 처리된 응답을 반환하는 주요 기능을 가지고 있습니다.
 * 또한, API 응답 데이터를 파싱하여 클라이언트에 응답을 생성하는 유틸리티 메서드를 제공합니다.
 * <p>
 * 주요 기능은 메시지 전송과 응답 생성을 포함하며, 이를 통해 멀티 서비스 간의 비동기 통신을 효과적으로 처리할 수 있습니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class ApiMessageClient {

	protected final RabbitTemplate rabbitTemplate;
	protected final ApiMessageResponseHandler apiMessageResponseHandler;
	protected final JsonUtils jsonUtils;


	/**
	 * HTTP 요청을 받아 적절한 서비스로 라우팅하여 처리합니다.
	 * 요청 경로를 분석하여 해당하는 서비스의 메시지 큐로 RabbitMQ를 통해 전송하며,
	 * 비동기적으로 처리된 결과를 응답으로 반환합니다.
	 *
	 * <p>주요 처리 과정:
	 * 1. 요청 경로를 분석하여 적절한 라우팅 키 결정 (/api/auth -> auth-service 등)
	 * 2. HTTP 요청 데이터를 메시지 형태로 변환
	 * 3. RabbitMQ를 통해 해당 서비스로 메시지 전송
	 * 4. 비동기 응답을 대기하고 결과 반환
	 *
	 * @param <T>      {@code BaseApiResponse} 내에 캡슐화된 응답 본문의 타입
	 * @param exchange HTTP 요청과 응답 정보를 포함하는 {@code ServerWebExchange}
	 * @return 비동기 처리가 완료되면 응답 데이터가 포함된 {@code BaseApiResponse}를 담고 있는
	 * {@code ResponseEntity}를 방출하는 {@code Mono}
	 */
	protected <T> Mono<ResponseEntity<BaseApiResponse<T>>> sendMessage(
		ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();
		String routingKey = determineRoutingKey(path);

		return request
			.getBody()
			.collectList()
			.flatMap(
				dataBuffers -> {
					// 요청 본문을 문자열로 변환
					String requestBody = "";
					if (!dataBuffers.isEmpty()) {
						StringBuilder sb = new StringBuilder();
						dataBuffers.forEach(
							buffer -> {
								byte[] bytes = new byte[buffer.readableByteCount()];
								buffer.read(bytes);
								sb.append(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
							});
						requestBody = sb.toString();
					}

					// 요청 데이터 객체 생성
					ApiRequestData ApiRequestData = new ApiRequestData();
					ApiRequestData.setPath(request.getPath().value());
					ApiRequestData.setMethod(request.getMethod().name());
					ApiRequestData.setHeaders(getHeadersMap(request.getHeaders()));
					ApiRequestData.setBody(requestBody);
					ApiRequestData.setQueryParams(
						exchange.getRequest().getQueryParams().toSingleValueMap());

					// 메시지 상관관계 ID 생성
					String correlationId = UUID.randomUUID().toString();
					// RabbitMQ로 메시지 전송
					rabbitTemplate.convertAndSend(
						RabbitMQConstants.API_EXCHANGE,
						routingKey,
						ApiRequestData,
						message -> {
							message.getMessageProperties().setCorrelationId(correlationId);
							message
								.getMessageProperties()
								.getHeaders()
								.put(AmqpHeaders.CORRELATION_ID, correlationId);
							return message;
						});
					// 비동기 응답 처리
					return apiMessageResponseHandler
						.awaitResponse(correlationId)
						.map(this::createApiResponseEntity);
				});
	}

	/**
	 * API 경로를 기반으로 적절한 RabbitMQ 라우팅 키를 결정합니다.
	 *
	 * <p>이 메서드는 특정 API 경로를 해당하는 메시지 큐의 라우팅 키에 매핑합니다. 경로가 미리 정의된 패턴과 일치하지 않으면 예외가 발생합니다.
	 *
	 * @param path 라우팅 키를 결정하는데 사용되는 API 경로
	 * @return 주어진 API 경로에 해당하는 RabbitMQ 라우팅 키
	 * @throws IllegalArgumentException 제공된 경로가 미리 정의된 패턴과 일치하지 않는 경우
	 */
	private String determineRoutingKey(String path) {
		// 경로 패턴에 따라 라우팅 키 반환
		if (path.startsWith("/api/auth")) {
			return RabbitMQConstants.AUTH_API_ROUTING_KEY;
			//      		} else if (path.startsWith("/api/recipe")) {
			//      			return RabbitMQConstants.RECIPE_API_ROUTING_KEY;
		} else if (path.startsWith("/api/account")) {
			return RabbitMQConstants.ACCOUNT_API_ROUTING_KEY;
		} else {
			// 기본값 또는 예외 처리
			throw new IllegalArgumentException("지원하지 않는 API 경로: " + path);
		}
	}

	/**
	 * ApiResponseData에 포함된 데이터로 BaseApiResponse를 포함하는 ResponseEntity를 생성합니다. HTTP 상태 코드는
	 * ApiResponseData의 상태 코드를 기반으로 설정됩니다.
	 *
	 * @param <T>          BaseApiResponse 내에 포함될 응답 본문의 타입
	 * @param responseData 상태 코드, 헤더, 본문을 포함하는 API 응답 생성에 사용되는 입력 데이터
	 * @return BaseApiResponse 객체와 해당 HTTP 상태 코드를 포함하는 ResponseEntity
	 */
	protected <T> ResponseEntity<BaseApiResponse<T>> createApiResponseEntity(
		ApiResponseData responseData) {
		HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());

		// ApiResponse 객체 생성
		BaseApiResponse<T> apiResponse = createApiResponse(responseData);

		// ResponseEntity에 상태 코드와 함께 ApiResponse 객체를 담아 반환
		return ResponseEntity.status(httpStatus).body(apiResponse);
	}

	/**
	 * ApiResponseData를 사용하여 BaseApiResponse 객체를 생성합니다. 이 메서드는 응답 본문이 JSON 형식인 경우 파싱을 수행합니다. JSON이 아닌
	 * 응답이나 JSON 파싱 실패의 경우 대체 응답을 생성합니다.
	 *
	 * @param <T>          BaseApiResponse에 포함될 응답 객체의 타입
	 * @param responseData HTTP 상태 코드, 헤더, 본문을 포함하는 ApiResponseData 객체
	 * @return 구성된 API 응답을 나타내는 BaseApiResponse 객체
	 */
	protected <T> BaseApiResponse<T> createApiResponse(ApiResponseData responseData) {
		HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());

		// Content-Type 확인
		String contentType = responseData.getHeaders().get("Content-Type");

		// JSON 응답인 경우 파싱 처리
		if (contentType != null && contentType.contains("application/json")) {
			try {
				// JSON 문자열을 Object로 파싱
				ObjectMapper objectMapper = new ObjectMapper();
				Object parsedBody = objectMapper.readValue(responseData.getBody(), Object.class);

				// 타입 캐스팅 추가
				@SuppressWarnings("unchecked")
				T typedBody = (T) parsedBody;

				if (httpStatus.is2xxSuccessful()) {
					return BaseApiResponse.success(
						httpStatus.value(),
						typedBody, // 파싱된 객체 사용
						httpStatus.getReasonPhrase());
				} else {
					return BaseApiResponse.error(httpStatus.value(), typedBody, httpStatus.getReasonPhrase());
				}
			} catch (JsonProcessingException e) {
				// JSON 파싱 실패 시 원본 문자열 사용
				log.error("Failed to parse JSON response", e);
				return fallbackResponse(responseData, httpStatus);
			}
		} else {
			// JSON이 아닌 경우 기존 처리 방식 유지
			return fallbackResponse(responseData, httpStatus);
		}
	}

	/**
	 * API 응답 데이터와 HTTP 상태를 사용하여 대체 응답을 생성합니다. 이 메서드는 HTTP 상태의 성공 또는 실패 여부를 확인하고, 그에 따라 주어진 데이터를 사용하여
	 * 성공 또는 오류 응답을 생성합니다.
	 *
	 * @param <T>          {@code BaseApiResponse}에 포함될 응답 본문의 타입
	 * @param responseData 대체 응답 생성에 사용될 상태 코드, 헤더, 본문을 포함하는 API 응답 데이터
	 * @param httpStatus   대체 응답이 성공인지 오류인지 판단하는 데 사용되는 HTTP 상태
	 * @return HTTP 상태, 본문 데이터 및 상태에서 파생된 메시지를 포함하는 {@code BaseApiResponse} 인스턴스
	 */
	private <T> BaseApiResponse<T> fallbackResponse(
		ApiResponseData responseData, HttpStatus httpStatus) {
		// 타입 캐스팅 추가
		@SuppressWarnings("unchecked")
		T body = (T) responseData.getBody();

		if (httpStatus.is2xxSuccessful()) {
			return BaseApiResponse.success(httpStatus.value(), body, httpStatus.getReasonPhrase());
		} else {
			return BaseApiResponse.error(httpStatus.value(), body, httpStatus.getReasonPhrase());
		}
	}

	/**
	 * HttpHeaders 객체를 Map 형태로 변환합니다. 각 헤더 이름은 해당하는 값(들)과 매핑되며, 여러 값이 있는 경우 쉼표로 구분된 하나의 문자열로 연결됩니다.
	 *
	 * @param headers 변환할 헤더들을 포함하는 HttpHeaders 객체
	 * @return 헤더 이름을 키로, 헤더 값을 문자열로 가지는 Map
	 */
	protected Map<String, String> getHeadersMap(HttpHeaders headers) {
		Map<String, String> headersMap = new HashMap<>();
		headers.forEach(
			(name, values) -> {
				String headerValue = String.join(", ", values);
				headersMap.put(name, headerValue);
			});
		return headersMap;
	}
}
