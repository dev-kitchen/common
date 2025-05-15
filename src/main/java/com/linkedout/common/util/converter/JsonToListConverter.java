package com.linkedout.common.util.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.io.IOException;
import java.util.List;

@ReadingConverter
public class JsonToListConverter<T> implements Converter<Json, List<T>> {
	private final ObjectMapper objectMapper;
	private final TypeReference<List<T>> typeReference;

	public JsonToListConverter(TypeReference<List<T>> typeReference) {
		this.typeReference = typeReference;

		this.objectMapper = JsonMapper.builder()
			// 필요한 모듈 등록 (Java 8 날짜/시간 타입을 위해)
			.addModule(new JavaTimeModule())
			// 대소문자 구분 없이 Enum 값을 읽도록 설정
			.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
			// 알 수 없는 속성 무시
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();
	}

	@Override
	public List<T> convert(Json source) {
		try {
			return objectMapper.readValue(source.asArray(), typeReference);
		} catch (IOException e) {
			throw new RuntimeException("Error converting JSON to List", e);
		}
	}
}
