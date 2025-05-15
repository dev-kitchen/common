package com.linkedout.common.messaging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.EnrichedRequestData;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import com.linkedout.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * ApiMessageClient는 API 요청을 처리하고 RabbitMQ를 사용하여 메시징 기반의 서비스와 상호 작용하기 위한 추상 클래스입니다. 이 클래스는 HTTP 요청을
 * 라우팅 키에 매핑하고, 메시지를 RabbitMQ에 전송한 뒤 비동기적으로 처리된 응답을 반환하는 주요 기능을 가지고 있습니다. 또한, API 응답 데이터를 파싱하여
 * 클라이언트에 응답을 생성하는 유틸리티 메서드를 제공합니다.
 *
 * <p>주요 기능은 메시지 전송과 응답 생성을 포함하며, 이를 통해 멀티 서비스 간의 비동기 통신을 효과적으로 처리할 수 있습니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class ApiMessageClient {

	protected final RabbitTemplate rabbitTemplate;
	protected final JsonUtils jsonUtils;
	protected final ServiceMessageResponseHandler serviceMessageResponseHandler;
	protected final ServiceIdentifier serviceIdentifier;


	/**
	 * API Gateway에서 외부 요청을 내부 마이크로서비스로 전달하는 메서드입니다.
	 * HTTP 요청을 수신하여 RabbitMQ 메시지로 변환하고, 해당하는 내부 서비스로 라우팅합니다.
	 * 메시지는 요청 본문, 헤더, 쿼리 파라미터 등의 정보를 포함하며,
	 * 비동기적으로 서비스의 응답을 수신하여 클라이언트에게 전달합니다.
	 *
	 * @param exchange 현재 HTTP 요청과 응답 컨텍스트를 나타내는 ServerWebExchange
	 * @return 내부 서비스의 처리 결과를 포함하는 BaseApiResponse를 담은 ResponseEntity를 감싸는 Mono,
	 * 오류 발생 시 적절한 오류 응답 반환
	 */
	protected <T> Mono<ResponseEntity<BaseApiResponse<T>>> sendMessage(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();

		// 경로에서 타겟 서비스와 작업 결정
		String targetService = determineTargetService(path);
		String operation = determineOperation(path, request.getMethod().name());

		// 메시지 상관관계 ID 생성
		String correlationId = UUID.randomUUID().toString();

		// 기본 서비스 메시지 빌더 생성
		ServiceMessageDTO.ServiceMessageDTOBuilder<Object> messageBuilder = ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation)
			.replyTo(serviceIdentifier.getResponseRoutingKey());

		// 인증 정보 가져오기
		return ReactiveSecurityContextHolder.getContext()
			.map(context -> {
				Authentication authentication = context.getAuthentication();
				if (authentication != null && authentication.isAuthenticated()) {
					// Authentication 객체를 AuthenticationDTO로 변환
					AuthenticationDTO authDTO = convertToAuthenticationDTO(authentication);
					messageBuilder.authentication(authDTO);
				}
				return messageBuilder;
			})
			.defaultIfEmpty(messageBuilder)  // 인증 정보가 없는 경우
			.flatMap(builder ->
				request.getBody()
					.collectList()
					.flatMap(dataBuffers -> {
						String requestBody = convertDataBuffersToString(dataBuffers);

						Object requestData;
						try {
							if (!requestBody.isEmpty()) {
								requestData = jsonUtils.fromJson(requestBody, Object.class);
							} else {
								requestData = new HashMap<>(); // 빈 요청 본문인 경우
							}
						} catch (Exception e) {
							log.error("요청 본문 변환 중 오류 발생", e);
							requestData = requestBody; // 변환 실패 시 원본 문자열 사용
						}

						EnrichedRequestData<Object> enrichedRequest = new EnrichedRequestData<>();
						enrichedRequest.setBody(requestData);
						enrichedRequest.setPath(path);
						enrichedRequest.setMethod(request.getMethod().name());
						enrichedRequest.setHeaders(getHeadersMap(request.getHeaders()));
						enrichedRequest.setQueryParams(exchange.getRequest().getQueryParams().toSingleValueMap());

						// 이제 payload 설정
						ServiceMessageDTO<Object> message = builder
							.payload(enrichedRequest)
							.build();

						Mono<ServiceMessageDTO<?>> responseMono =
							serviceMessageResponseHandler.awaitResponse(correlationId);

						String routingKey = targetService + ".consumer.routing.key";

						// 로그에 인증 정보 포함 여부 표시
						boolean isAuthenticated = message.getAuthentication() != null;
						String userId = isAuthenticated ? message.getAuthentication().getPrincipal() : "anonymous";

						log.info("서비스 메시지 발송: target={}, operation={}, correlationId={}, authenticated={}, userId={}",
							routingKey, operation, correlationId, isAuthenticated, userId);

						rabbitTemplate.convertAndSend(
							RabbitMQConstants.SERVICE_EXCHANGE,
							routingKey,
							message,
							msg -> {
								msg.getMessageProperties().setCorrelationId(correlationId);
								msg.getMessageProperties()
									.getHeaders()
									.put(AmqpHeaders.CORRELATION_ID, correlationId);
								msg.getMessageProperties().setReplyTo(serviceIdentifier.getResponseRoutingKey());
								return msg;
							});

						// 비동기 응답 처리
						return responseMono
							.timeout(Duration.ofSeconds(30))
							.onErrorResume(
								TimeoutException.class,
								e -> {
									log.error("서비스 응답 타임아웃: correlationId={}, path={}", correlationId, path);
									return Mono.error(
										new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "서비스 응답 타임아웃"));
								})
							.map(this::createResponseEntityFromServiceMessage);
					})
			);
	}

	/**
	 * Authentication 객체를 AuthenticationDTO로 변환
	 */
	private AuthenticationDTO convertToAuthenticationDTO(Authentication authentication) {
		// 권한 목록 추출
		List<String> authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toList());

		// 상세 정보 추출
		Map<String, Object> details = null;
		if (authentication.getDetails() instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> detailsMap = (Map<String, Object>) authentication.getDetails();
			details = new HashMap<>(detailsMap);
		} else {
			details = new HashMap<>();
		}

		// AuthenticationDTO 생성 및 반환
		return AuthenticationDTO.builder()
			.principal(authentication.getName())
			.authorities(authorities)
			.details(details)
			.build();
	}

	/**
	 * URL 경로와 HTTP 메서드로부터 작업명을 결정하는 메서드
	 * <p>
	 * 패턴 예시:
	 * - /api/recipes         -> getRecipes (컬렉션 조회)
	 * - /api/recipes/123     -> getRecipeById (ID로 단일 항목 조회)
	 * - /api/users/search    -> getSearch (검색 기능)
	 * - /api/orders/123/items -> getOrderItems (하위 리소스 조회)
	 * - /api/products/123/reviews/456 -> getProductReviewById (중첩 리소스 조회)
	 *
	 * @param path   HTTP 요청 경로
	 * @param method HTTP 메서드(GET, POST, PUT, DELETE 등)
	 * @return 결정된 작업명
	 */
	private String determineOperation(String path, String method) {
		String[] segments = path.split("/");
		List<String> validSegments = Arrays.stream(segments)
			.filter(s -> !s.isEmpty())
			.toList();

		if (validSegments.isEmpty()) {
			return "default";
		}

		// HTTP 메서드를 소문자로 변환 (get, post, put, delete 등)
		String httpMethod = method.toLowerCase();

		// API 접두사 제거 (api와 첫 번째 리소스 이름(auth 또는 account) 제거)
		if (validSegments.get(0).equals("api") && validSegments.size() > 2) {
			// api와 auth/account 모두 제거하고 그 다음부터 시작
			validSegments = validSegments.subList(2, validSegments.size());
		} else if (validSegments.get(0).equals("api") && validSegments.size() > 1) {
			// api만 제거
			validSegments = validSegments.subList(1, validSegments.size());
		}

		// validSegments가 비어있으면 기본값 반환
		if (validSegments.isEmpty()) {
			return httpMethod;
		}

		// 단일 세그먼트인 경우
		if (validSegments.size() == 1) {
			// ID 체크 - 숫자만 있는 경우 "ById" 접미사 사용
			if (validSegments.get(0).matches("\\d+")) {
				return httpMethod + "ById";
			}
			return httpMethod + capitalize(validSegments.get(0));
		}

		// 여러 세그먼트가 있는 경우
		StringBuilder result = new StringBuilder(httpMethod);

		for (int i = 0; i < validSegments.size(); i++) {
			String segment = validSegments.get(i);

			// 숫자만 있는 세그먼트(ID)는 "ById"로 변환
			if (segment.matches("\\d+")) {
				// 이전 세그먼트가 있는 경우에는 이전 세그먼트의 단수형 + "ById"
				if (i > 0) {
					// 이미 ById가 추가되었는지 확인
					if (!result.toString().endsWith("ById")) {
						result.append("ById");
					}
				} else {
					// 첫 번째 세그먼트가 ID인 경우 (드문 경우)
					result.append("ById");
				}
			} else {
				// 일반 세그먼트는 그대로 추가
				result.append(capitalize(segment));
			}
		}

		return result.toString();
	}

	/**
	 * 세그먼트 리스트를 연결하여 camelCase 형태의 문자열로 변환
	 */
	private String concatSegments(List<String> segments) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < segments.size(); i++) {
			String segment = segments.get(i);
			// 숫자만 있는 세그먼트(ID)는 특별히 처리
			if (segment.matches("\\d+")) {
				String previousResource = i > 0 ? segments.get(i - 1) : "";
				if (!previousResource.isEmpty()) {
					result.append(capitalize(singularize(previousResource))).append("ById");
				} else {
					result.append("ById").append(segment);
				}
			} else {
				result.append(capitalize(segment));
			}
		}
		return result.toString();
	}

	/**
	 * 복수형 명사를 단수형으로 변환 (간단한 규칙만 적용)
	 */
	private String singularize(String plural) {
		if (plural == null || plural.isEmpty()) {
			return plural;
		}

		// 간단한 영어 복수형 규칙 적용
		if (plural.endsWith("ies")) {
			return plural.substring(0, plural.length() - 3) + "y";
		} else if (plural.endsWith("es") && (
			plural.endsWith("sses") ||
				plural.endsWith("shes") ||
				plural.endsWith("ches") ||
				plural.endsWith("xes"))) {
			return plural.substring(0, plural.length() - 2);
		} else if (plural.endsWith("s") && !plural.endsWith("ss")) {
			return plural.substring(0, plural.length() - 1);
		}

		// 변환 규칙이 없으면 원래 문자열 반환
		return plural;
	}

	/**
	 * 문자열의 첫 글자를 대문자로 변환합니다.
	 */
	private String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * API 경로에서 대상 서비스를 결정합니다.
	 *
	 * @param path API 경로
	 * @return 서비스 식별자
	 */
	private String determineTargetService(String path) {
		if (path.startsWith("/api/auth")) {
			return "auth";
		} else if (path.startsWith("/api/recipes")) {
			return "recipe";
		} else if (path.startsWith("/api/account")) {
			return "account";
		} else {
			throw new IllegalArgumentException("지원하지 않는 API 경로: " + path);
		}
	}

	/**
	 * ServiceMessageDTO를 ResponseEntity<BaseApiResponse>로 변환합니다.
	 *
	 * @param <T>      BaseApiResponse 내에 포함될 응답 본문의 타입
	 * @param response 서비스에서 받은 ServiceMessageDTO 응답
	 * @return ResponseEntity<BaseApiResponse < T>> 형태로 변환된 HTTP 응답
	 */
	@SuppressWarnings("unchecked")
	protected <T> ResponseEntity<BaseApiResponse<T>> createResponseEntityFromServiceMessage(
		ServiceMessageDTO<?> response) {

		// 기본 HTTP 상태 설정
		HttpStatus httpStatus = HttpStatus.OK;

		if (response.getError() != null) {
			// 오류 응답인 경우
			log.debug("서비스 오류 응답: {}", response.getError());
			httpStatus = HttpStatus.BAD_REQUEST; // 기본 오류 상태

			// 상태 코드가 포함된 경우 사용
			if (response.getStatusCode() != null) {
				try {
					httpStatus = HttpStatus.valueOf(response.getStatusCode());
				} catch (Exception e) {
					log.warn("잘못된 HTTP 상태 코드: {}", response.getStatusCode());
				}
			}

			BaseApiResponse<T> errorResponse =
				BaseApiResponse.error(
					httpStatus.value(), (T) response.getError(), httpStatus.getReasonPhrase());

			return ResponseEntity.status(httpStatus).body(errorResponse);
		}

		// 성공 응답 처리
		if (response.getStatusCode() != null) {
			try {
				httpStatus = HttpStatus.valueOf(response.getStatusCode());
			} catch (Exception e) {
				log.warn("잘못된 HTTP 상태 코드, 기본값 사용: {}", response.getStatusCode());
			}
		}

		// 응답 본문 처리
		T responseBody = null;
		if (response.getPayload() != null) {
			try {
				responseBody = (T) response.getPayload();
			} catch (ClassCastException e) {
				log.error("응답 본문 형식 변환 실패", e);
				// 형식 변환 실패 시 원본 그대로 사용
				responseBody = (T) response.getPayload();
			}
		}

		// 응답 생성
		BaseApiResponse<T> apiResponse =
			BaseApiResponse.success(httpStatus.value(), responseBody, httpStatus.getReasonPhrase());

		// 헤더 설정 (필요한 경우)
		HttpHeaders headers = new HttpHeaders();
		if (response.getHeaders() != null) {
			response.getHeaders().forEach(headers::add);
		}

		return ResponseEntity.status(httpStatus).headers(headers).body(apiResponse);
	}


	/**
	 * DataBuffer 리스트를 문자열로 변환하는 메서드
	 *
	 * @param dataBuffers 변환할 DataBuffer 리스트
	 * @return 변환된 문자열, 데이터가 없으면 빈 문자열 반환
	 */
	private String convertDataBuffersToString(List<DataBuffer> dataBuffers) {
		if (dataBuffers == null || dataBuffers.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		dataBuffers.forEach(
			buffer -> {
				byte[] bytes = new byte[buffer.readableByteCount()];
				buffer.read(bytes);
				DataBufferUtils.release(buffer); // 중요: 메모리 누수 방지를 위해 버퍼 해제
				sb.append(new String(bytes, StandardCharsets.UTF_8));
			});

		return sb.toString();
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
		ServiceMessageDTO<T> responseData) {
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
	protected <T> BaseApiResponse<T> createApiResponse(ServiceMessageDTO<T> responseData) {
		HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());

		// Content-Type 확인
		String contentType = responseData.getHeaders().get("Content-Type");

		// JSON 응답인 경우 파싱 처리
		if (contentType != null && contentType.contains("application/json")) {
			try {
				// JSON 문자열을 Object로 파싱
				ObjectMapper objectMapper = new ObjectMapper();
				Object parsedBody =
					objectMapper.readValue((JsonParser) responseData.getPayload(), Object.class);

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
			} catch (IOException e) {
				throw new RuntimeException(e);
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
		ServiceMessageDTO<T> responseData, HttpStatus httpStatus) {
		// 타입 캐스팅 추가
		T body = responseData.getPayload();

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
