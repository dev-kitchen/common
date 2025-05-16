package com.linkedout.common.messaging.converter;

import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AuthenticationConverter는 Spring Security 프레임워크의 Authentication 객체를
 * 구조화된 데이터 전송 객체인 AuthenticationDTO로 변환하는 역할을 합니다.
 */
@Component
public class AuthenticationConverter {

	/**
	 * Spring Security의 {@link Authentication} 객체를 {@link AuthenticationDTO}로 변환합니다.
	 * 변환 과정에서 주체명, 권한 목록 및 추가 상세 정보를 제공된 {@link Authentication} 객체에서
	 * 추출합니다.
	 *
	 * @param authentication 변환할 {@link Authentication} 객체. 이 객체는 주체명, 권한
	 *                       컬렉션 및 추가 상세 정보를 포함합니다.
	 * @return 주체명, 권한 목록 및 {@link Authentication} 객체에서 추출한 모든 추가 상세 정보를
	 * 포함하는 {@link AuthenticationDTO}
	 */
	public AuthenticationDTO convert(Authentication authentication) {
		// 권한 목록 추출
		List<String> authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toList());

		// 상세 정보 추출
		Map<String, Object> details = null;
		if (authentication.getDetails() instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> detailsMap = (Map<String, Object>) authentication.getDetails();
			details = new HashMap<>(detailsMap);
		} else {
			details = new HashMap<>();
		}

		// AuthenticationDTO 생성 및 반환
		return AuthenticationDTO.builder()
			.principal(authentication.getName())
			.authorities(authorities)
			.details(details)
			.build();
	}
}