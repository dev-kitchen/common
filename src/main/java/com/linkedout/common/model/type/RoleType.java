package com.linkedout.common.model.type;

import lombok.Getter;

@Getter
public enum RoleType {
	ROLE_USER,
	ROLE_ADMIN;

	// DB에 저장될 값 얻기
	public String getValue() {
		return name();
	}

	// 문자열에서 Enum으로 변환
	public static RoleType fromValue(String value) {
		try {
			return RoleType.valueOf(value);
		} catch (IllegalArgumentException e) {
			return ROLE_USER; // 기본값
		}
	}
}
