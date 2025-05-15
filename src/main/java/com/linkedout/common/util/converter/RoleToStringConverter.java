package com.linkedout.common.util.converter;

import com.linkedout.common.model.type.RoleEnum;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class RoleToStringConverter implements Converter<RoleEnum, String> {
	@Override
	public String convert(@NonNull RoleEnum source) {
		return source.name();
	}
}
