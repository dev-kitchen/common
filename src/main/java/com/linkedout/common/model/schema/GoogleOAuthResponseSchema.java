package com.linkedout.common.model.schema;

import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.auth.oauth.google.GoogleOAuthResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구글 OAuth 응답 래퍼")
public class GoogleOAuthResponseSchema extends BaseApiResponse<GoogleOAuthResponse> {
	@Schema(hidden = true)
	@Override
	public GoogleOAuthResponse getError() {
		return super.getError();
	}
}