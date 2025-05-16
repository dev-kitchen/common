package com.linkedout.common.model.schema;

import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleOAuthResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구글 OAuth 응답 래퍼")
public class GoogleOAuthResponseSchema extends BaseApiResponse<GoogleOAuthResponseDTO> {
	@Schema(hidden = true)
	@Override
	public GoogleOAuthResponseDTO getError() {
		return super.getError();
	}
}