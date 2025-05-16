package com.linkedout.common.messaging.resolver;

import org.springframework.stereotype.Component;

@Component
public class ServiceResolver {
	
	/**
	 * 제공된 API 경로를 기반으로 대상 서비스를 판단합니다.
	 *
	 * @param path 대상 서비스를 결정하는데 사용되는 API 경로
	 * @return API 경로에 해당하는 서비스 식별자
	 * @throws IllegalArgumentException 지원되지 않는 API 경로인 경우
	 */
	public String resolveTargetService(String path) {
		if (path.startsWith("/api/auth")) {
			return "auth";
		} else if (path.startsWith("/api/recipes")) {
			return "recipe";
		} else if (path.startsWith("/api/account")) {
			return "account";
		} else {
			throw new IllegalArgumentException("지원하지 않는 API 경로: " + path);
		}
	}
}