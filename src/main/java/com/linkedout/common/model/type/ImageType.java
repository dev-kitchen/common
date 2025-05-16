package com.linkedout.common.model.type;

public enum ImageType {

	MAIN("recipe/main"),
	STEP("recipe/step"),
	PROFILE("profile"),
	GENERAL("general");

	private final String path;

	ImageType(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public static ImageType fromString(String type) {
		try {
			return ImageType.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("유효하지 않은 이미지 타입 경로: " + type);
		}
	}
}
