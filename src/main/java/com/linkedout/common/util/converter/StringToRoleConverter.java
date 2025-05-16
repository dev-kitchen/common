package com.linkedout.common.util.converter;

import com.linkedout.common.model.type.RoleType;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToRoleConverter implements Converter<String, RoleType> {
	@Override
	public RoleType convert(@NonNull String source) {
		return RoleType.fromValue(source);
	}
}
