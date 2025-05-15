package com.linkedout.common.model.dto;

import java.util.Map;

public class EnrichedRequestData<T> {
	private T body;
	private String path;
	private String method;
	private Map<String, String> headers;
	private Map<String, String> queryParams;

	// 기본 생성자
	public EnrichedRequestData() {
	}

	// 모든 필드를 포함한 생성자
	public EnrichedRequestData(T body, String path, String method,
														 Map<String, String> headers,
														 Map<String, String> queryParams) {
		this.body = body;
		this.path = path;
		this.method = method;
		this.headers = headers;
		this.queryParams = queryParams;
	}

	// ID 추출 헬퍼 메서드
	public Long extractIdFromPath() {
		if (path == null || path.isEmpty()) {
			return null;
		}

		String[] segments = path.split("/");
		for (String segment : segments) {
			if (segment.matches("\\d+")) {
				return Long.valueOf(segment);
			}
		}
		return null;
	}

	// 마지막 경로 세그먼트에서 ID 추출
	public Long extractIdFromLastPathSegment() {
		if (path == null || path.isEmpty()) {
			return null;
		}

		String[] segments = path.split("/");
		if (segments.length > 0) {
			String lastSegment = segments[segments.length - 1];
			if (lastSegment.matches("\\d+")) {
				return Long.valueOf(lastSegment);
			}
		}
		return null;
	}

	// body에서 특정 필드 추출 헬퍼 메서드
	@SuppressWarnings("unchecked")
	public <R> R extractFromBody(String fieldName) {
		if (body == null) {
			return null;
		}

		if (body instanceof Map) {
			Map<String, Object> bodyMap = (Map<String, Object>) body;
			return (R) bodyMap.get(fieldName);
		}

		return null;
	}

	// 쿼리 파라미터에서 값 가져오기
	public String getQueryParam(String name) {
		if (queryParams == null) {
			return null;
		}
		return queryParams.get(name);
	}

	// Getter와 Setter 메서드
	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(Map<String, String> queryParams) {
		this.queryParams = queryParams;
	}
}