package com.memorydrawer.auth.api;

import java.util.UUID;

import com.memorydrawer.auth.domain.UserAccount;

public record SignupResponse(
	UUID userId,
	String email
) {

	public static SignupResponse from(UserAccount userAccount) {
		return new SignupResponse(userAccount.getId(), userAccount.getEmail());
	}
}
