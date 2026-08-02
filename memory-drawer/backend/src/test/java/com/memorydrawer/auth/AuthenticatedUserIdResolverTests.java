package com.memorydrawer.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Principal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

class AuthenticatedUserIdResolverTests {

	private final AuthenticatedUserIdResolver resolver = new AuthenticatedUserIdResolver();

	@Test
	void resolvesUuidFromAuthenticatedPrincipalName() {
		UUID userId = UUID.randomUUID();
		Principal principal = userId::toString;

		assertThat(resolver.resolve(principal)).isEqualTo(userId);
	}

	@Test
	void rejectsMissingOrNonUuidPrincipal() {
		assertAuthError(() -> resolver.resolve(null));
		assertAuthError(() -> resolver.resolve(() -> "user@example.com"));
	}

	private void assertAuthError(Runnable action) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_001)
			);
	}
}
