package com.linkedout.common.model.type;

import lombok.Getter;

@Getter
public enum RoleEnum {
	ROLE_USER,
	ROLE_ADMIN;

	// DB에 저장될 값 얻기
	public String getValue() {
		return name();
	}

	// 문자열에서 Enum으로 변환
	public static RoleEnum fromValue(String value) {
		try {
			return RoleEnum.valueOf(value);
		} catch (IllegalArgumentException e) {
			return ROLE_USER; // 기본값
		}
	}
}
