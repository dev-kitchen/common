package com.linkedout.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthRequest {
	private String code;
	private String redirectUri;
}
