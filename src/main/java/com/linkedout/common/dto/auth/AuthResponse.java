package com.linkedout.common.dto.auth;

import com.linkedout.common.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
	private boolean success;
	private String token;
	private User user;
	private String error;
	private String correlationId;
}
