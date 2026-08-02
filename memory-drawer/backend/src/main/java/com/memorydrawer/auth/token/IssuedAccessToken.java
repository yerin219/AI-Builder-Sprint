package com.memorydrawer.auth.token;

public record IssuedAccessToken(
	String value,
	long expiresIn
) {
}
