package com.memorydrawer.auth.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.memorydrawer.auth.api.LoginRequest;
import com.memorydrawer.auth.api.LoginResponse;
import com.memorydrawer.auth.domain.UserAccount;
import com.memorydrawer.auth.password.PasswordHasher;
import com.memorydrawer.auth.repository.UserAccountRepository;
import com.memorydrawer.auth.token.AccessTokenService;
import com.memorydrawer.auth.token.IssuedAccessToken;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

@Service
public class LoginService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordHasher passwordHasher;
	private final AccessTokenService accessTokenService;

	public LoginService(
		UserAccountRepository userAccountRepository,
		PasswordHasher passwordHasher,
		AccessTokenService accessTokenService
	) {
		this.userAccountRepository = userAccountRepository;
		this.passwordHasher = passwordHasher;
		this.accessTokenService = accessTokenService;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
		UserAccount userAccount = userAccountRepository.findByEmail(normalizedEmail)
			.orElseThrow(() -> new ApiException(ErrorCode.AUTH_002));

		if (!passwordHasher.matches(request.password(), userAccount.getPasswordHash())) {
			throw new ApiException(ErrorCode.AUTH_002);
		}

		IssuedAccessToken accessToken = accessTokenService.issue(userAccount.getId());
		return LoginResponse.of(userAccount.getId(), accessToken);
	}
}
