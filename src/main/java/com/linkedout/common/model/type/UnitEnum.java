package com.linkedout.common.model.type;

import lombok.Getter;

@Getter
public enum UnitEnum {
	g("g"),
	kg("kg"),
	ml("ml"),
	l("l"),
	piece("piece"), // 개
	slice("slice"), // 조각
	sheet("sheet"), // 장
	head("head"), // 마리
	stem("stem"), // 줄기
	grain("grain"), // 알
	root("root"), // 뿌리
	teaspoon("teaspoon"), // 작은술
	tablespoon("tablespoon"), // 큰술
	spoon("spoon"), // 숟가락
	cup("cup"), // 컵
	clove("clove"), // 쪽
	handful("handful"), // 줌
	can("can"), // 통
	fruit("fruit"), // 과
	portion("portion"), // 배
	bunch("bunch"); // 송이

	private final String value;

	UnitEnum(String value) {
		this.value = value;
	}
}
