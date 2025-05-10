package com.linkedout.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ServiceResponseData implements Serializable {
	private String correlationId;
	
	/**
	 * 응답 본문
	 * <p>
	 * 마이크로서비스가 생성한 응답 내용을 문자열로 저장합니다.
	 * 일반적으로 JSON 형식의 데이터가 포함되지만, 다른 형식(XML, HTML 등)일 수도 있습니다.
	 * API Gateway는 이 내용을 HTTP 응답 본문으로 사용합니다.
	 */
	private String body;
}
