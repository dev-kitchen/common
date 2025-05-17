package com.linkedout.common.messaging.apiClient.resolver;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class HttpStatusResolver {


	/**
	 * 제공된 operation 문자열을 기반으로 HTTP 메소드(GET, POST, PUT, DELETE, PATCH)를
	 * 추출하고 반환합니다. operation 문자열이 알려진 HTTP 메소드와 일치하지 않으면
	 * "UNKNOWN"이 반환됩니다.
	 *
	 * @param operation HTTP 메소드를 결정하는데 사용되는 operation 문자열
	 * @return 해당하는 HTTP 메소드 문자열 또는 일치하는 항목이 없는 경우 "UNKNOWN"
	 */
	public String extractHttpMethod(String operation) {
		if (operation.startsWith("get")) {
			return "GET";
		} else if (operation.startsWith("post")) {
			return "POST";
		} else if (operation.startsWith("put")) {
			return "PUT";
		} else if (operation.startsWith("delete")) {
			return "DELETE";
		} else if (operation.startsWith("patch")) {
			return "PATCH";
		} else {
			return "UNKNOWN";
		}
	}


	/**
	 * 제공된 HTTP 메소드를 기반으로 적절한 HTTP 상태 코드를 결정합니다.
	 *
	 * @param httpMethod 상태 코드를 결정할 HTTP 메소드 (예: "GET", "POST", "DELETE")
	 * @return 주어진 HTTP 메소드에 해당하는 {@link HttpStatus}:
	 * - "POST"의 경우 {@code HttpStatus.CREATED}
	 * - "DELETE"의 경우 {@code HttpStatus.NO_CONTENT}
	 * - 그 외 메소드의 경우 {@code HttpStatus.OK}
	 */
	public HttpStatus determineStatusCodeByHttpMethod(String httpMethod) {
		return switch (httpMethod) {
			case "POST" -> HttpStatus.CREATED;
			case "DELETE" -> HttpStatus.NO_CONTENT;
			default -> HttpStatus.OK;
		};
	}
}