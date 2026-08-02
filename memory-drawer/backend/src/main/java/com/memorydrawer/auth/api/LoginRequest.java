package com.memorydrawer.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
	@NotBlank
	@Email
	@Size(max = 320)
	String email,

	@NotBlank
	@Size(max = 10)
	String password
) {

	public LoginRequest {
		if (email != null) {
			email = email.trim();
		}
	}
}
