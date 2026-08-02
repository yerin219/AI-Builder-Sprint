package com.memorydrawer.auth;

import java.security.Principal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

@Component
public class AuthenticatedUserIdResolver {

	public UUID resolve(Principal principal) {
		if (principal == null || principal.getName() == null) {
			throw new ApiException(ErrorCode.AUTH_001);
		}
		try {
			return UUID.fromString(principal.getName());
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.AUTH_001, exception);
		}
	}
}
