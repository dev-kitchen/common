package com.linkedout.common.messaging.apiClient.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * DataBufferConverter 클래스는 DataBuffer 객체 리스트를 하나의 문자열로 연결하여 변환하는 역할을 합니다.
 * 각 버퍼의 내용을 UTF-8로 인코딩된 문자열로 변환하는 동안 메모리 누수를 방지하기 위해
 * 버퍼가 해제되도록 보장합니다.
 */
@Slf4j
@Component
public class DataBufferConverter {

	/**
	 * DataBuffer 객체 리스트를 하나의 String으로 변환합니다.
	 * 각 DataBuffer의 내용을 읽고, 메모리 누수 방지를 위해 버퍼를 해제한 후,
	 * UTF-8로 인코딩된 문자열로 변환하여 연결합니다.
	 *
	 * @param dataBuffers 변환할 DataBuffer 객체 리스트.
	 *                    리스트가 null이거나 비어있는 경우 빈 문자열을 반환합니다.
	 * @return 연결된 문자열
	 */
	public String convertToString(List<DataBuffer> dataBuffers) {
		if (dataBuffers == null || dataBuffers.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		dataBuffers.forEach(
			buffer -> {
				byte[] bytes = new byte[buffer.readableByteCount()];
				buffer.read(bytes);
				DataBufferUtils.release(buffer); // 중요: 메모리 누수 방지를 위해 버퍼 해제
				sb.append(new String(bytes, StandardCharsets.UTF_8));
			});

		return sb.toString();
	}
}