package com.linkedout.common.model.dto.auth.oauth.google;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthRequestDTO {
	@NotNull
	@NotBlank
	private String idToken;
}
