package com.linkedout.common.util.converter.recipe;

import com.linkedout.common.model.type.UnitType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class UnitTypeToStringConverter implements Converter<UnitType, String> {
	@Override
	public String convert(UnitType source) {
		return source.getValue();
	}
}