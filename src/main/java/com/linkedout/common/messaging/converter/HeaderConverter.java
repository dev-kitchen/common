package com.linkedout.common.messaging.converter;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HeaderConverter {

	/**
	 * HttpHeaders 객체를 Map으로 변환합니다.
	 * 각 헤더 이름은 키가 되고, 해당하는 헤더 값들은 하나의 문자열로 결합됩니다.
	 *
	 * @param headers 변환할 HttpHeaders 객체. 각 헤더 이름은 맵의 키가 되고,
	 *                헤더 값들은 ", " 구분자를 사용하여 하나의 문자열로 결합됩니다.
	 * @return 헤더 이름과 값을 포함하는 Map. 키는 헤더 이름이고,
	 * 값은 쉼표로 구분된 헤더 값들입니다.
	 */
	public Map<String, String> toMap(HttpHeaders headers) {
		Map<String, String> headersMap = new HashMap<>();
		headers.forEach(
			(name, values) -> {
				String headerValue = String.join(", ", values);
				headersMap.put(name, headerValue);
			});
		return headersMap;
	}
}