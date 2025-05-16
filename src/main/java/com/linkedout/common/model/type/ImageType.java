package com.linkedout.common.model.type;

public enum ImageType {

	RECIPE_MAIN("recipe/main"),
	RECIPE_STEP("recipe/step"),
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
			return GENERAL;
		}
	}
}
