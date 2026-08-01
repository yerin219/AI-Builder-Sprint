package com.memorydrawer.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.memorydrawer.auth.domain.UserAccount;
import com.memorydrawer.auth.repository.UserAccountRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Test
	void signsUpWithoutAuthenticationAndStoresHashedPassword() throws Exception {
		mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "  Memory@Example.com ",
					  "password": "1234567890"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
			.andExpect(jsonPath("$.data.userId").isNotEmpty())
			.andExpect(jsonPath("$.data.email").value("memory@example.com"));

		UserAccount saved = userAccountRepository.findAll().getFirst();
		assertThat(saved.getPasswordHash()).isNotEqualTo("1234567890");
		assertThat(new BCryptPasswordEncoder().matches("1234567890", saved.getPasswordHash())).isTrue();
	}

	@Test
	void rejectsDuplicateEmailIgnoringCaseAndSurroundingWhitespace() throws Exception {
		signup("memory@example.com", "1234567890");

		mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": " MEMORY@example.com ",
					  "password": "simplepass"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("AUTH_003"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void rejectsInvalidEmailWithCommonErrorResponse() throws Exception {
		mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "not-an-email",
					  "password": "simplepass"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("VALIDATION_001"))
			.andExpect(jsonPath("$.message").value("요청값을 확인해주세요."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void rejectsPasswordLongerThanTenCharacters() throws Exception {
		mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "memory@example.com",
					  "password": "12345678901"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("VALIDATION_001"))
			.andExpect(jsonPath("$.message").value("요청값을 확인해주세요."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	private void signup(String email, String password) throws Exception {
		mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "%s",
					  "password": "%s"
					}
					""".formatted(email, password)))
			.andExpect(status().isCreated());
	}
}
