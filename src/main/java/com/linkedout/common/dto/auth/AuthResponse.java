package com.linkedout.common.dto.auth;

import com.linkedout.common.entity.Account;
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
	private Account account;
	private String error;
	private String correlationId;
}
