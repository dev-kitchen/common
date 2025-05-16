package com.linkedout.common.util;

import java.util.List;

public class StringUtils {

	private StringUtils() {
		// 인스턴스화 방지
	}


	/**
	 * 문자열의 첫 번째 문자를 대문자로 변환하고 나머지 문자열은 그대로 유지합니다.
	 *
	 * @param str 대문자화할 입력 문자열; null 또는 빈 문자열 가능
	 * @return 첫 번째 문자가 대문자로 변환된 입력 문자열, 또는 입력이 null이나 빈 문자열인 경우 원래 문자열
	 */
	public static String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}


	/**
	 * 기본 단수화 규칙을 기반으로 영어 복수형 명사를 단수형으로 변환합니다.
	 * <p>
	 * 이 메서드는 다음과 같은 일반적인 복수형에서 단수형으로의 변환 규칙을 적용합니다:
	 * - "ies"로 끝나는 단어를 "y"로 변환
	 * - "sses", "shes", "ches", "xes"와 같은 특정 케이스에서 "es" 제거
	 * - "ss"로 끝나지 않는 단어에서 마지막 "s" 제거
	 * <p>
	 * 입력 문자열이 어떤 변환 규칙과도 일치하지 않는 경우 원래 문자열이 반환됩니다.
	 *
	 * @param plural 변환할 명사의 복수형; null이나 빈 문자열도 가능
	 * @return 명사의 단수형, 또는 규칙이 적용되지 않거나 입력이 null/빈 문자열인 경우 원본 문자열
	 */
	public static String singularize(String plural) {
		if (plural == null || plural.isEmpty()) {
			return plural;
		}

		// 간단한 영어 복수형 규칙 적용
		if (plural.endsWith("ies")) {
			return plural.substring(0, plural.length() - 3) + "y";
		} else if (plural.endsWith("es") && (
			plural.endsWith("sses") ||
				plural.endsWith("shes") ||
				plural.endsWith("ches") ||
				plural.endsWith("xes"))) {
			return plural.substring(0, plural.length() - 2);
		} else if (plural.endsWith("s") && !plural.endsWith("ss")) {
			return plural.substring(0, plural.length() - 1);
		}

		// 변환 규칙이 없으면 원래 문자열 반환
		return plural;
	}


	/**
	 * 문자열 세그먼트 목록을 단일 포맷된 문자열로 연결합니다.
	 * 각 세그먼트는 읽기 쉬운 식별자를 만들기 위해 처리되며, 숫자로만 구성된 세그먼트는
	 * ID로 취급되어 이전 세그먼트의 단수형과 함께 "ById"가 추가됩니다.
	 *
	 * @param segments 연결할 문자열 세그먼트 목록; ID를 나타내는 문자열이나 다른 텍스트 포함 가능
	 * @return 입력 세그먼트를 기반으로 연결되고 포맷된 문자열
	 */
	public static String concatSegments(List<String> segments) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < segments.size(); i++) {
			String segment = segments.get(i);
			// 숫자만 있는 세그먼트(ID)는 특별히 처리
			if (segment.matches("\\d+")) {
				String previousResource = i > 0 ? segments.get(i - 1) : "";
				if (!previousResource.isEmpty()) {
					result.append(capitalize(singularize(previousResource))).append("ById");
				} else {
					result.append("ById").append(segment);
				}
			} else {
				result.append(capitalize(segment));
			}
		}
		return result.toString();
	}
}