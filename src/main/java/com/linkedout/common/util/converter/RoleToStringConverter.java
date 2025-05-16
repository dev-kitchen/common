package com.linkedout.common.util.converter;

import com.linkedout.common.model.type.RoleType;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class RoleToStringConverter implements Converter<RoleType, String> {
	@Override
	public String convert(@NonNull RoleType source) {
		return source.name();
	}
}
