package com.linkedout.common.model.dto.recipe.request;

import com.linkedout.common.model.dto.recipe.RecipeDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCreateDTO {
	private String name;
	private String mainImage;
	private String method;
	private String type;
	private String tip;
	private List<RecipeDTO.IngredientDTO> ingredients;
	private List<RecipeDTO.SourceDTO> sources;
	private List<RecipeDTO.ManualStepDTO> manualSteps;
}

