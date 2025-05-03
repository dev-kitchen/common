package com.linkedout.common.schema;

import com.linkedout.common.dto.BaseApiResponse;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구글 OAuth 응답 래퍼")
public class GoogleOAuthResponseSchema extends BaseApiResponse<GoogleOAuthResponse> {
	@Schema(hidden = true)  // 이 필드는 스웨거 문서에서 숨김
	@Override
	public GoogleOAuthResponse getError() {
		return super.getError();
	}
}