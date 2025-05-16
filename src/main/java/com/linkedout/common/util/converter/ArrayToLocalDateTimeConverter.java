package com.linkedout.common.util.converter;


import org.modelmapper.AbstractConverter;
import org.springframework.data.convert.ReadingConverter;

import java.time.LocalDateTime;

@ReadingConverter
public class ArrayToLocalDateTimeConverter extends AbstractConverter<Object[], LocalDateTime> {

	@Override
	protected LocalDateTime convert(Object[] source) {
		if (source == null || source.length < 6) {
			return null;
		}

		int year = (int) source[0];
		int month = (int) source[1];
		int day = (int) source[2];
		int hour = (int) source[3];
		int minute = (int) source[4];
		int second = (int) source[5];

		return LocalDateTime.of(year, month, day, hour, minute, second);
	}
}