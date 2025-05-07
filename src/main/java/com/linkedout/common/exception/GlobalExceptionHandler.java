package com.linkedout.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.dto.ApiResponseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler는 Spring Boot 애플리케이션의 컨트롤러에서 발생하는 예외를
 * 중앙 집중식으로 가로채고 처리하는 클래스입니다. 일관된 오류 응답을 보장하고
 * 다양한 유형의 예외를 효과적으로 관리합니다.
 * <p>
 * 이 클래스는 Spring의 {@code @RestControllerAdvice}를 사용하여 모든 컨트롤러에 어드바이스를 적용하고
 * {@code @ExceptionHandler} 어노테이션을 활용하여 특정 예외들을 처리합니다.
 * <p>
 * 다음과 같은 예외들이 처리됩니다:
 * 1. BaseException: HTTP 상태 코드와 메시지를 포함하는 사용자 정의 런타임 예외
 * 2. JsonProcessingException: 요청이나 응답 직렬화 과정에서 발생하는 JSON 파싱 오류를 처리
 * 3. IllegalArgumentException: 잘못된 메소드 인자로 인해 발생하는 유효성 검사 오류를 처리
 * 4. Exception: 처리되지 않은 다른 모든 예외를 포착하여 대체 응답 제공
 * <p>
 * 오류 응답은 {@code ApiResponseData} 객체로 래핑되어 미리 정의된 구조를 유지하며,
 * HTTP 상태 코드, 헤더, 오류 세부 정보가 포함된 직렬화된 JSON 본문을 포함합니다.
 * <p>
 * 의존성:
 * - {@code @Slf4j}: 로깅 지원 제공
 * - {@code ObjectMapper}: JSON 객체의 직렬화 및 역직렬화에 사용
 * - {@code ApiResponseData}: 모든 API 응답에 대한 응답 구조 표현
 * - {@code BaseException}: 구조화된 오류 처리를 지원하는 사용자 정의 예외 클래스
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ObjectMapper objectMapper;

	/**
	 * BaseException을 처리하여 예외 세부 정보를 로깅하고 구조화된 형식의
	 * 오류 응답을 생성합니다.
	 *
	 * @param ex HTTP 상태 코드와 사용자 정의 오류 메시지를 포함하는 BaseException
	 * @return HTTP 상태 코드, 사용자 정의 헤더, 오류 상세 정보를 포함하는
	 * 구조화된 오류 응답이 담긴 ApiResponseData 객체
	 */
	@ExceptionHandler(BaseException.class)
	public ApiResponseData handleBaseException(BaseException ex) {
		log.error("BaseException 발생: {}", ex.getMessage());
		return createErrorResponse(ex.getStatusCode(), ex.getMessage());
	}

	/**
	 * JSON 처리 중 발생하는 {@code JsonProcessingException} 유형의 예외를 처리합니다.
	 * 오류를 로깅하고 클라이언트에게 반환할 구조화된 오류 응답을 생성합니다.
	 *
	 * @param ex JSON 처리 오류에 대한 세부 정보를 포함하는 예외 인스턴스
	 * @return HTTP 상태 코드, 헤더 및 상세 오류 메시지를 포함하는 구조화된
	 * {@code ApiResponseData} 객체를 응답으로 반환
	 */
	@ExceptionHandler(JsonProcessingException.class)
	public ApiResponseData handleJsonProcessingException(JsonProcessingException ex) {
		log.error("JSON 파싱 오류: {}", ex.getMessage());
		return createErrorResponse(400, "잘못된 요청 형식입니다: " + ex.getMessage());
	}

	/**
	 * {@code IllegalArgumentException} 타입의 예외를 처리합니다.
	 * 이 메소드는 잘못된 인자에 대한 오류 세부사항을 로깅하고
	 * 클라이언트에게 반환할 구조화된 오류 응답을 생성합니다.
	 *
	 * @param ex 잘못된 인자 오류의 세부사항을 포함하는 {@code IllegalArgumentException} 인스턴스
	 * @return HTTP 400 상태 코드와 잘못된 인자를 설명하는 오류 메시지를 포함한 구조화된 {@code ApiResponseData}
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ApiResponseData handleIllegalArgumentException(IllegalArgumentException ex) {
		log.error("유효성 오류: {}", ex.getMessage());
		return createErrorResponse(400, ex.getMessage());
	}

	/**
	 * 애플리케이션 내에서 발생하는 일반적인 {@code Exception}을 처리하고,
	 * 오류 세부 정보를 로깅하며, 클라이언트를 위한 구조화된 오류 응답을 생성합니다.
	 *
	 * @param ex 발생한 오류를 나타내는 예외 인스턴스.
	 *           예외 메시지와 스택 트레이스의 상세 정보를 포함합니다.
	 * @return HTTP 500 상태 코드, 헤더, 그리고 내부 서버 오류를 나타내는
	 * 상세 오류 메시지가 포함된 구조화된 {@code ApiResponseData} 객체
	 */
	@ExceptionHandler(Exception.class)
	public ApiResponseData handleException(Exception ex) {
		log.error("서버 내부 오류: {}", ex.getMessage(), ex);
		return createErrorResponse(500, "서버 내부 오류가 발생했습니다: " + ex.getMessage());
	}

	/**
	 * HTTP 상태 코드, 헤더, JSON 형식의 오류 메시지가 포함된 {@code ApiResponseData} 객체로
	 * 구조화된 오류 응답을 생성합니다.
	 *
	 * @param statusCode 오류를 나타내는 HTTP 상태 코드
	 * @param message    응답 본문에 포함될 오류 메시지
	 * @return 상태 코드, 헤더, 오류 상세 정보가 포함된 {@code ApiResponseData} 객체
	 */
	private ApiResponseData createErrorResponse(int statusCode, String message) {
		ApiResponseData response = new ApiResponseData();
		response.setStatusCode(statusCode);
		response.setHeaders(new HashMap<>());
		response.getHeaders().put("Content-Type", "application/json");

		try {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", statusCode);
			errorResponse.put("success", false);
			errorResponse.put("message", message);
			response.setBody(objectMapper.writeValueAsString(errorResponse));
		} catch (JsonProcessingException e) {
			response.setBody("{\"success\":false,\"message\":\"" + message + "\"}");
		}

		return response;
	}
}