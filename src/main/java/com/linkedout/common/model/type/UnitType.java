package com.linkedout.common.model.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UnitType {
	g("g"),
	kg("kg"),
	ml("ml"),
	l("l"),
	PIECE("개"),
	SLICE("조각"),
	SHEET("장"),
	HEAD("마리"),
	STEM("줄기"),
	GRAIN("알"),
	ROOT("뿌리"),
	TEASPOON("작은술"),
	TABLESPOON("큰술"),
	SPOON("숟가락"),
	CUP("컵"),
	CLOVE("쪽"),
	HANDFUL("줌"),
	CAN("통"),
	FRUIT("과"),
	PORTION("qo"),
	BUNCH("송이");

	private final String value;

	UnitType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static UnitType fromValue(String value) {
		for (UnitType unitType : UnitType.values()) {
			if (unitType.value.equals(value)) {
				return unitType;
			}
		}
		throw new IllegalArgumentException("Unknown UnitType value: " + value);
	}
}
