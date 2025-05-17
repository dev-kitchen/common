package com.linkedout.common.messaging.apiClient.resolver;

import com.linkedout.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class OperationResolver {

	/**
	 * 주어진 경로와 HTTP 메소드를 기반으로 작업 문자열을 생성하고 반환합니다.
	 * 이 메소드는 URL 세그먼트를 처리하고 API 접두사를 처리하며, 세그먼트 패턴(예: ID 또는 일반 리소스 이름)에 따라
	 * 적절한 접미사를 추가합니다.
	 * <p>
	 * 예시:
	 * - GET /api/recipes/123 -> getRecipeById
	 * - GET /api/recipes -> getRecipes
	 * - GET /api/recipes/123/comments -> getRecipeByIdComments
	 * - POST /api/recipes -> postRecipe
	 * - PUT /api/recipes/123 -> putRecipeById
	 * - DELETE /api/recipes/123 -> deleteRecipeById
	 *
	 * @param path   여러 세그먼트를 포함할 수 있는 해석할 URL 경로
	 * @param method GET, POST, PUT, DELETE 등의 HTTP 메소드
	 * @return URL 경로와 HTTP 메소드로부터 생성된 작업 이름 문자열
	 */
	public String resolve(String path, String method) {
		String[] segments = path.split("/");
		List<String> validSegments = Arrays.stream(segments)
			.filter(s -> !s.isEmpty())
			.toList();

		if (validSegments.isEmpty()) {
			return "default";
		}

		// HTTP 메서드를 소문자로 변환 (get, post, put, delete 등)
		String httpMethod = method.toLowerCase();

		// API 접두사 제거 (api와 첫 번째 리소스 이름(auth 또는 account) 제거)
		if (validSegments.get(0).equals("api") && validSegments.size() > 2) {
			// api와 auth/account 모두 제거하고 그 다음부터 시작
			validSegments = validSegments.subList(2, validSegments.size());
		} else if (validSegments.get(0).equals("api") && validSegments.size() > 1) {
			// api만 제거
			validSegments = validSegments.subList(1, validSegments.size());
		}

		// validSegments가 비어있으면 기본값 반환
		if (validSegments.isEmpty()) {
			return httpMethod;
		}

		// 단일 세그먼트인 경우
		if (validSegments.size() == 1) {
			// ID 체크 - 숫자만 있는 경우 "ById" 접미사 사용
			if (validSegments.get(0).matches("\\d+")) {
				return httpMethod + "ById";
			}
			return httpMethod + StringUtils.capitalize(validSegments.get(0));
		}

		// 여러 세그먼트가 있는 경우
		StringBuilder result = new StringBuilder(httpMethod);

		for (int i = 0; i < validSegments.size(); i++) {
			String segment = validSegments.get(i);

			// 숫자만 있는 세그먼트(ID)는 "ById"로 변환
			if (segment.matches("\\d+")) {
				// 이전 세그먼트가 있는 경우에는 이전 세그먼트의 단수형 + "ById"
				if (i > 0) {
					// 이미 ById가 추가되었는지 확인
					if (!result.toString().endsWith("ById")) {
						result.append("ById");
					}
				} else {
					// 첫 번째 세그먼트가 ID인 경우 (드문 경우)
					result.append("ById");
				}
			} else {
				// 일반 세그먼트는 그대로 추가
				result.append(StringUtils.capitalize(segment));
			}
		}

		return result.toString();
	}
}