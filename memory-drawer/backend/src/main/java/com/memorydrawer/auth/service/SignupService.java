package com.memorydrawer.auth.service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.memorydrawer.auth.api.SignupRequest;
import com.memorydrawer.auth.api.SignupResponse;
import com.memorydrawer.auth.domain.UserAccount;
import com.memorydrawer.auth.password.PasswordHasher;
import com.memorydrawer.auth.repository.UserAccountRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

@Service
public class SignupService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordHasher passwordHasher;

	public SignupService(
		UserAccountRepository userAccountRepository,
		PasswordHasher passwordHasher
	) {
		this.userAccountRepository = userAccountRepository;
		this.passwordHasher = passwordHasher;
	}

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		if (userAccountRepository.existsByEmail(normalizedEmail)) {
			throw new ApiException(ErrorCode.AUTH_003);
		}

		UserAccount userAccount = UserAccount.register(
			UUID.randomUUID(),
			normalizedEmail,
			passwordHasher.hash(request.password()),
			Instant.now()
		);

		try {
			return SignupResponse.from(userAccountRepository.saveAndFlush(userAccount));
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.AUTH_003, exception);
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
