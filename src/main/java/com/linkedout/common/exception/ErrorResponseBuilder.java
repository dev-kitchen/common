package com.linkedout.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.model.dto.ApiResponseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ErrorResponseBuilder는 HTTP와 메시징 유스케이스 모두를 위한 에러 응답 생성을 표준화하고
 * 단순화하도록 설계된 유틸리티 클래스입니다. JSON 객체 매퍼와 통합하여 상세한 에러 메시지를
 * 구조화하고 서로 다른 인터페이스에서 일관되게 포맷팅합니다.
 * <p>
 * 이 클래스의 주요 역할:
 * - 커스터마이징 가능한 HTTP 상태 코드로 예외에 대한 HTTP 에러 응답 생성
 * - 메시지 큐(MQ) 에러 응답 생성
 * - 기존 응답 객체에 에러 정보 채우기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorResponseBuilder {
	private final ObjectMapper objectMapper;


	/**
	 * 제공된 예외와 HTTP 상태 코드를 사용하여 HTTP 에러 응답을 생성합니다.
	 * 이 메서드는 성공 여부 플래그와 예외의 에러 메시지를 포함하는
	 * 구조화된 에러 본문이 담긴 응답 엔티티를 구성합니다.
	 *
	 * @param ex     에러 응답을 발생시킨 예외; 예외의 메시지가 응답 본문을 채우는데 사용됨
	 * @param status 응답에 포함될 HTTP 상태
	 * @return 구조화된 에러 응답 본문과 제공된 HTTP 상태를 포함하는 응답 엔티티
	 */
	public ResponseEntity<Object> buildHttpErrorResponse(Exception ex, HttpStatus status) {
		String message = ex.getMessage();

		Map<String, Object> body = createErrorBody(message);
		return new ResponseEntity<>(body, status);
	}


	/**
	 * 메시지 큐(MQ)용 에러 응답을 제공된 상태 코드와 메시지로 구성합니다.
	 * 이 메서드는 지정된 HTTP 상태 코드, JSON 형식의 에러 본문,
	 * "Content-Type"이 "application/json"으로 설정된 헤더를 포함하는
	 * 구조화된 에러 응답을 생성합니다.
	 *
	 * @param statusCode 에러 응답에 포함될 HTTP 상태 코드
	 * @param message    응답 본문에 포함될 에러 메시지
	 * @return 구조화된 에러 응답이 담긴 ApiResponseData 인스턴스
	 */
	public ApiResponseData buildMqErrorResponse(int statusCode, String message) {

		ApiResponseData response = new ApiResponseData();
		response.setStatusCode(statusCode);
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		try {
			response.setBody(objectMapper.writeValueAsString(createErrorBody(message)));
		} catch (JsonProcessingException e) {
			response.setBody("{\"success\":false,\"message\":\"" + message + "\"}");
		}

		return response;
	}


	/**
	 * 주어진 ApiResponseData 인스턴스에 HTTP 상태 코드, 에러 메시지와
	 * 적절한 헤더를 포함한 에러 정보를 채웁니다.
	 *
	 * @param response   에러 정보가 채워질 ApiResponseData 인스턴스
	 * @param statusCode 응답에 설정될 HTTP 상태 코드
	 * @param message    응답 본문에 포함될 에러 메시지
	 */
	public void populateErrorResponse(ApiResponseData response, int statusCode, String message) {

		response.setStatusCode(statusCode);
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		try {
			response.setBody(objectMapper.writeValueAsString(createErrorBody(message)));
		} catch (JsonProcessingException e) {
			response.setBody("{\"success\":false,\"message\":\"" + message + "\"}");
		}
	}


	/**
	 * 성공 여부 상태와 에러 메시지가 포함된 구조화된 에러 본문 맵을 생성합니다.
	 * 이 메서드는 표준 에러 응답 구조를 생성하기 위해 내부적으로 사용됩니다.
	 *
	 * @param message 에러 본문에 포함될 에러 메시지
	 * @return "success" (false로 설정)와 "message" 키를 포함하는 에러 구조체 맵
	 */
	private Map<String, Object> createErrorBody(String message) {
		Map<String, Object> errorBody = new HashMap<>();
		errorBody.put("success", false);
		errorBody.put("message", message);
		return errorBody;
	}
}
