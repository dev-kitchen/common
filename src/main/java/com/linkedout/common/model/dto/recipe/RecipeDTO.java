package com.linkedout.common.model.dto.recipe;

import com.linkedout.common.model.dto.account.AccountDTO;
import com.linkedout.common.model.type.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "레시피 DTO")
public class RecipeDTO {
	@Schema(description = "레시피 ID", example = "1")
	private Long id;

	@Schema(description = "레시피 이름", example = "김치찌개")
	private String name;

	@Schema(description = "메인 이미지 URL", example = "https://example.com/image.jpg")
	private String mainImage;

	@Schema(description = "조리 방법", example = "끓이기")
	private String method;

	@Schema(description = "음식 종류", example = "한식")
	private String type;

	@Schema(description = "조리 팁", example = "김치는 신김치를 사용하세요")
	private String tip;

	@Schema(description = "출처", example = "백종원의 요리비책")
	private String source;

	@Schema(description = "유튜브 링크", example = "https://youtube.com/watch?v=...")
	private String youtubeLink;

	@Schema(description = "작성자 정보")
	private AccountDTO author;

	@Schema(description = "재료 목록")
	private List<IngredientDTO> ingredients;

	@Schema(description = "양념 목록")
	private List<SaucesDTO> sauces;

	@Schema(description = "조리 단계 목록")
	private List<ManualStepDTO> manualSteps;

	@Schema(description = "정규화된 레시피 이름", example = "kimchijjigae")
	private String normalizedName;

	@Schema(description = "생성일시", example = "2024-01-01 12:00:00")
	private String createdAt;

	@Schema(description = "수정일시", example = "2024-01-01 13:00:00")
	private String updatedAt;

	// 내부 DTO 클래스들
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class IngredientDTO {
		@NotNull
		@NotBlank
		@Schema(description = "재료 이름", example = "돼지고기")
		private String name;

		@Schema(description = "재료 단위", example = "g")
		@NotNull
		private UnitType unit;

		@NotNull
		@NotBlank
		@Schema(description = "재료 수량", example = "300")
		private String quantity;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SaucesDTO {
		@Schema(description = "양념 이름", example = "고추가루")
		private String name;

		@Schema(description = "양념 단위", example = "tablespoon")
		private UnitType unit;

		@Schema(description = "양념 수량", example = "2")
		private String quantity;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ManualStepDTO {
		@Schema(description = "조리 순서 번호", example = "1")
		private Integer id;

		@Schema(description = "조리 방법 설명", example = "돼지고기를 넣고 볶아주세요.")
		private String text;

		@Schema(description = "조리 과정 이미지 URL", example = "https://example.com/step1.jpg")
		private String image;
	}
}