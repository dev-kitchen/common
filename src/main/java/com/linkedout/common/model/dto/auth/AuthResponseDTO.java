package com.linkedout.common.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
	private String accessToken;
	private String refreshToken;
	private String email;
	private String name;
	private String profileImage;
}
