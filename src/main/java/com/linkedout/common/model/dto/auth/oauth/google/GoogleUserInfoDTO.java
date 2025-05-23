package com.linkedout.common.model.dto.auth.oauth.google;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserInfoDTO {
	private String sub;
	private String name;
	private String given_name;
	private String family_name;
	private String picture;
	private String email;
	private Boolean email_verified;
	private String locale;
}