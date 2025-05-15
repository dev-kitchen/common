package com.linkedout.common.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationDTO implements Serializable {
	private String principal;  // 사용자 ID (subject)
	private List<String> authorities;  // 권한 목록
	private Map<String, Object> details;  // 추가 상세 정보 (email, name 등)
}
