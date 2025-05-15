package com.linkedout.common.model.dto.account;

import com.linkedout.common.model.type.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
	@NotNull
	private Long id;

	@NotNull
	@NotBlank
	@Email
	private String email;

	@NotNull
	@NotBlank
	private String name;

	private String picture;

	@NotNull
	@NotBlank
	private String providerId;

	@NotNull
	@NotBlank
	private String provider;

	@NotNull
	@NotBlank
	private RoleEnum role;

	@PastOrPresent
	private LocalDateTime createdAt;

	@PastOrPresent
	private LocalDateTime updatedAt;
}
