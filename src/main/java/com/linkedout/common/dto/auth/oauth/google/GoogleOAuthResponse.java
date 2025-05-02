
package com.linkedout.common.dto.auth.oauth.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthResponse {
	@NotNull
	@NotBlank
	private String accessToken;

	@NotNull
	@NotBlank
	private String refreshToken;

	private String email;
	private String name;
	private String picture;
}
