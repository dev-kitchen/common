
package com.linkedout.common.dto.auth.oauth.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthResponse {
	private String accessToken;
	private String refreshToken;
	private String email;
	private String name;
	private String picture;
}
