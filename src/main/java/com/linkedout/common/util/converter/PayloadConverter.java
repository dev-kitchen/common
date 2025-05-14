package com.linkedout.common.util.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 페이로드 객체를 지정된 타입으로 변환하는 유틸리티 클래스
 *
 * <p>이 클래스는 Jackson ObjectMapper를 사용하여 객체를 타입 안전하게 변환합니다. 페이로드가 이미 대상 타입인 경우 직접 캐스팅하고, 그렇지 않은 경우
 * ObjectMapper로 변환합니다.
 *
 * <p>이 클래스는 Spring이 관리하는 컴포넌트이며 필요한 곳에 자동으로 주입됩니다.
 */
@Component
@RequiredArgsConstructor
public class PayloadConverter {

  private final ObjectMapper objectMapper;

  /**
   * 페이로드 객체를 지정된 타입으로 안전하게 변환
   *
   * @param payload 변환할 페이로드 객체
   * @param targetType 변환 대상 타입 클래스
   * @param <T> 변환 대상 타입
   * @return 변환된 객체
   */
  public <T> T convert(Object payload, Class<T> targetType) {
    if (payload == null) {
      return null;
    }

    if (targetType.isInstance(payload)) {
      return targetType.cast(payload);
    }

    return objectMapper.convertValue(payload, targetType);
  }
}
