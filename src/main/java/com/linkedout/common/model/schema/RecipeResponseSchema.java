package com.linkedout.common.model.schema;

import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.recipe.RecipeDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "레시피 응답 래퍼")
public class RecipeResponseSchema extends BaseApiResponse<RecipeDTO> {
	@Schema(hidden = true)
	@Override
	public RecipeDTO getError() {
		return super.getError();
	}
}
