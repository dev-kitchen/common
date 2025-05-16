package com.linkedout.common.util.converter.recipe;

import com.linkedout.common.model.type.UnitType;
import io.micrometer.common.lang.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToUnitTypeConverter implements Converter<String, UnitType> {
	@Override
	public UnitType convert(@NonNull String source) {
		return UnitType.fromValue(source);
	}
}