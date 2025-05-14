package com.linkedout.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMessageDTO<T> {
  private String correlationId;

  // 발신 서비스 식별자
  private String senderService;

  // 응답 큐 이름
  private String replyTo;

  // 요청/응답 작업 타입
  private String operation;

  // 메시지 타임스탬프
  @Builder.Default private long timestamp = System.currentTimeMillis();

  // 오류 메시지 (있는 경우)
  private String error;

  // 요청/응답 페이로드
  private T payload;

  private Integer statusCode;

  private Map<String, String> headers;
}
