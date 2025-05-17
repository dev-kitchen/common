package com.linkedout.common.messaging.apiClient.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.messaging.apiClient.resolver.HttpStatusResolver;
import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiResponseFactory {

	private final HttpStatusResolver httpStatusResolver;
	private final ObjectMapper objectMapper;


	/**
	 * 제공된 ServiceMessageDTO를 기반으로 BaseApiResponse가 포함된 ResponseEntity 객체를 생성합니다.
	 * 성공 및 오류 응답을 모두 처리하고, 적절한 HTTP 상태 코드를 결정하며, 제공된 경우 헤더를 포함합니다.
	 *
	 * @param <T>      BaseApiResponse에 포함될 데이터의 타입
	 * @param response 페이로드, 오류 상세 정보, HTTP 상태 코드, 헤더와 같은 정보가 포함된 서비스 메시지 DTO
	 * @return 적절한 HTTP 상태, 페이로드 또는 오류 상세 정보, 헤더가 포함된 BaseApiResponse를 담고 있는 ResponseEntity
	 */
	@SuppressWarnings("unchecked")
	public <T> ResponseEntity<BaseApiResponse<T>> createResponseEntity(
		ServiceMessageDTO<?> response) {

		// 기본 HTTP 상태 설정
		String httpMethod = httpStatusResolver.extractHttpMethod(response.getOperation());
		HttpStatus httpStatus = httpStatusResolver.determineStatusCodeByHttpMethod(httpMethod);

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
	 * 제공된 {@link ServiceMessageDTO}를 기반으로 {@link BaseApiResponse} 형태의 표준화된 API 응답을 생성합니다.
	 * 성공 및 오류 응답을 모두 처리하고, JSON 페이로드를 처리하며, 적절한 HTTP 상태를 결정합니다.
	 *
	 * @param <T>          {@link BaseApiResponse}에 포함될 응답 데이터의 타입
	 * @param responseData 페이로드, 헤더, 상태 코드 및 기타 응답 세부 정보가 포함된 서비스 메시지 DTO
	 * @return HTTP 상태, 파싱된 페이로드 또는 오류 상세 정보, 설명 메시지가 포함된 {@link BaseApiResponse}
	 */
	public <T> BaseApiResponse<T> createApiResponse(ServiceMessageDTO<T> responseData) {
		HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());

		// Content-Type 확인
		String contentType = responseData.getHeaders().get("Content-Type");

		// JSON 응답인 경우 파싱 처리
		if (contentType != null && contentType.contains("application/json")) {
			try {
				// JSON 문자열을 Object로 파싱
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
			} catch (Exception e) {
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
	 * 제공된 ServiceMessageDTO와 HTTP 상태를 기반으로 대체 응답을 생성합니다.
	 * HTTP 상태가 성공을 나타내면 성공 응답을 반환하고, 그렇지 않으면 오류 응답을 반환합니다.
	 *
	 * @param <T>          응답 페이로드에 포함된 데이터의 타입
	 * @param responseData 페이로드 및 관련 정보가 포함된 서비스 메시지 DTO
	 * @param httpStatus   응답 유형을 결정하는 HTTP 상태
	 * @return 제공된 HTTP 상태에 따라 성공 또는 오류 응답을 나타내는 BaseApiResponse 객체
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
}