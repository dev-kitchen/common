package com.linkedout.common.dto.auth.oauth.google;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserInfo {
	private String sub;
	private String name;
	private String given_name;
	private String family_name;
	private String picture;
	private String email;
	private Boolean email_verified;
	private String locale;
}