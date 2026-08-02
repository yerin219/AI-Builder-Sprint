package com.memorydrawer.auth.api;

import java.util.UUID;

import com.memorydrawer.auth.token.IssuedAccessToken;

public record LoginResponse(
	UUID userId,
	String accessToken,
	String tokenType,
	long expiresIn
) {

	private static final String BEARER = "Bearer";

	public static LoginResponse of(UUID userId, IssuedAccessToken token) {
		return new LoginResponse(userId, token.value(), BEARER, token.expiresIn());
	}
}
