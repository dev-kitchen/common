package com.linkedout.common.util.converter;

import com.linkedout.common.type.RoleEnum;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToRoleConverter implements Converter<String, RoleEnum> {
  @Override
  public RoleEnum convert(@NonNull String source) {
    return RoleEnum.fromValue(source);
  }
}
