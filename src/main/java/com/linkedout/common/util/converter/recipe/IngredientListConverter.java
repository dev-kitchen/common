package com.linkedout.common.util.converter.recipe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.linkedout.common.model.entity.Recipe;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.io.IOException;
import java.util.List;

@ReadingConverter
public class IngredientListConverter implements Converter<Json, List<Recipe.Ingredient>> {
	private final ObjectMapper objectMapper;

	public IngredientListConverter() {
		this.objectMapper = JsonMapper.builder()
			.addModule(new JavaTimeModule())
			.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();
	}

	@Override
	public List<Recipe.Ingredient> convert(Json source) {
		try {
			return objectMapper.readValue(source.asArray(),
				new TypeReference<List<Recipe.Ingredient>>() {
				});
		} catch (IOException e) {
			throw new RuntimeException("Error converting JSON to Ingredient List", e);
		}
	}
}
