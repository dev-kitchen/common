package com.linkedout.common.model.dto.recipe.request;

import com.linkedout.common.model.dto.recipe.RecipeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "레시피 생성 DTO")
public class RecipeCreateDTO {
	@NotNull
	@NotBlank
	@Schema(description = "레시피 이름", example = "김치찌개")
	private String name;

	@NotNull
	@NotBlank
	@Schema(description = "메인 이미지 URL", example = "https://example.com/image.jpg")
	private String mainImage;

	@NotNull
	@NotBlank
	@Schema(description = "조리 방법", example = "끓이기")
	private String method;

	@NotNull
	@NotBlank
	@Schema(description = "음식 종류", example = "한식")
	private String type;

	@Schema(description = "조리 팁", example = "김치는 신김치를 사용하세요")
	private String tip;

	@Schema(description = "출처", example = "백종원의 요리비책")
	private String source;

	@Schema(description = "유튭 링크", example = "https://youtube.com/watch?v=...")
	private String youtubeLink;

	@Valid
	@Schema(description = "재료 목록")
	private List<RecipeDTO.IngredientDTO> ingredients;

	@Valid
	@Schema(description = "양념 목록")
	private List<RecipeDTO.SaucesDTO> sauces;

	@NotNull
	@NotEmpty
	@Valid
	@Schema(description = "조리 단계 목록")
	private List<RecipeDTO.ManualStepDTO> manualSteps;
}

