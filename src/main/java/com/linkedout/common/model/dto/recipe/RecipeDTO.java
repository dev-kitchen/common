package com.linkedout.common.model.dto.recipe;

import com.linkedout.common.model.type.UnitEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDTO {
	private Long id;
	private String name;
	private String mainImage;
	private String method;
	private String type;
	private String tip;
	private List<IngredientDTO> ingredients;
	private List<SourceDTO> sources;
	private List<ManualStepDTO> manualSteps;
	private String normalizedName;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// 내부 DTO 클래스들
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class IngredientDTO {
		private String name;
		private UnitEnum unit;
		private String quantity;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SourceDTO {
		private String name;
		private UnitEnum unit;
		private String quantity;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ManualStepDTO {
		private Integer id;
		private String text;
		private String image;
	}
}