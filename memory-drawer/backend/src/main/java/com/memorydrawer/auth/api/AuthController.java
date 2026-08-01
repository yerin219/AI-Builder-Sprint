package com.memorydrawer.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.memorydrawer.auth.service.SignupService;
import com.memorydrawer.auth.service.LoginService;
import com.memorydrawer.common.api.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final SignupService signupService;
	private final LoginService loginService;

	public AuthController(SignupService signupService, LoginService loginService) {
		this.signupService = signupService;
		this.loginService = loginService;
	}

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
		@Valid @RequestBody SignupRequest request
	) {
		SignupResponse response = signupService.signup(request);
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.success("회원가입이 완료되었습니다.", response));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(
		@Valid @RequestBody LoginRequest request
	) {
		LoginResponse response = loginService.login(request);
		return ResponseEntity.ok(ApiResponse.success("로그인되었습니다.", response));
	}
}
