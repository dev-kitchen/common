package com.linkedout.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringUtilsTest {

	/**
	 * StringUtils 클래스의 capitalize 메서드 테스트
	 * <p>
	 * 이 메서드는 제공된 문자열의 첫 글자를 대문자로 변환합니다.
	 * 문자열이 null이거나 빈 문자열인 경우 입력 문자열을 그대로 반환합니다.
	 */

	@Test
	void givenNullString_whenCapitalize_thenReturnNull() {
		// Arrange
		String input = null;

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertNull(result, "Capitalizing a null string should return null.");
	}

	@Test
	void givenEmptyString_whenCapitalize_thenReturnEmptyString() {
		// Arrange
		String input = "";

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertEquals("", result, "Capitalizing an empty string should return an empty string.");
	}

	@Test
	void givenLowercaseString_whenCapitalize_thenReturnStringWithCapitalizedFirstCharacter() {
		// Arrange
		String input = "hello";

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertEquals("Hello", result, "The first character of 'hello' should be capitalized.");
	}

	@Test
	void givenUppercaseString_whenCapitalize_thenReturnStringUnchanged() {
		// Arrange
		String input = "Hello";

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertEquals("Hello", result, "The string 'Hello' should remain unchanged.");
	}

	@Test
	void givenSingleCharacterLowercaseString_whenCapitalize_thenReturnSingleCharacterUppercaseString() {
		// Arrange
		String input = "a";

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertEquals("A", result, "The single lowercase character 'a' should be capitalized to 'A'.");
	}

	@Test
	void givenSingleCharacterUppercaseString_whenCapitalize_thenReturnSameCharacter() {
		// Arrange
		String input = "A";

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertEquals("A", result, "The single uppercase character 'A' should remain unchanged.");
	}

	@Test
	void givenStringWithSpecialCharacterAtStart_whenCapitalize_thenReturnUnchangedString() {
		// Arrange
		String input = "@hello";

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertEquals("@hello", result, "The string starting with a special character should remain unchanged.");
	}

	@Test
	void givenStringWithNumbers_whenCapitalize_thenReturnUnchangedString() {
		// Arrange
		String input = "123abc";

		// Act
		String result = StringUtils.capitalize(input);

		// Assert
		assertEquals("123abc", result, "The string starting with numbers should remain unchanged.");
	}
}