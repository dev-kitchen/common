package com.linkedout.common.util.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.List;

@WritingConverter
public class ListToJsonConverter<T> implements Converter<List<T>, Json> {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Json convert(@NonNull List<T> source) {
		try {
			return Json.of(objectMapper.writeValueAsString(source));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting List to JSON", e);
		}
	}
}
