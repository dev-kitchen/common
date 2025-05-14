package com.linkedout.common.type;

import lombok.Getter;

@Getter
public enum UnitEnum {
  G("g"),
  KG("kg"),
  ML("ml"),
  L("l"),
  PIECE("piece"), // 개
  SLICE("slice"), // 조각
  SHEET("sheet"), // 장
  HEAD("head"), // 마리
  STEM("stem"), // 줄기
  GRAIN("grain"), // 알
  ROOT("root"), // 뿌리
  TEASPOON("teaspoon"), // 작은술
  TABLESPOON("tablespoon"), // 큰술
  SPOON("spoon"), // 숟가락
  CUP("cup"), // 컵
  CLOVE("clove"), // 쪽
  HANDFUL("handful"), // 줌
  CAN("can"), // 통
  FRUIT("fruit"), // 과
  PORTION("portion"), // 배
  BUNCH("bunch"); // 송이

  private final String value;

  UnitEnum(String value) {
    this.value = value;
  }
}
