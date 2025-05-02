package com.linkedout.common.dto.auth.oauth.google;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthRequest {
	@NotNull
	@NotBlank
	private String code;

	@NotNull
	@NotBlank
	private String redirectUri;
}
