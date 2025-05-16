package com.linkedout.common.util.converter;

import org.modelmapper.AbstractConverter;
import org.springframework.data.convert.WritingConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WritingConverter
public class LocalDateTimeToStringConverter extends AbstractConverter<LocalDateTime, String> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	protected String convert(LocalDateTime source) {
		if (source == null) {
			return null;
		}

		return source.format(FORMATTER);
	}
}