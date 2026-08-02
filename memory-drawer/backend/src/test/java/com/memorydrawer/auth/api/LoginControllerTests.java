package com.memorydrawer.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.memorydraft.api.AnalyzeMemoryDraftResponse;
import com.memorydrawer.memorydraft.api.TypeCardResponse;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.service.MemoryDraftAnalyzeService;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemoryDraftAnalyzeService memoryDraftAnalyzeService;

	@Test
	void logsInAndUsesBearerTokenForProtectedApi() throws Exception {
		signup("memory@example.com", "1234567890");

		MvcResult loginResult = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(" MEMORY@example.com ", "1234567890")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("로그인되었습니다."))
			.andExpect(jsonPath("$.data.userId").isNotEmpty())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(3600))
			.andReturn();

		JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.path("data");
		UUID userId = UUID.fromString(loginData.path("userId").asText());
		String accessToken = loginData.path("accessToken").asText();

		UUID draftId = UUID.randomUUID();
		given(memoryDraftAnalyzeService.analyze(eq(userId), any(MultipartFile.class)))
			.willReturn(new AnalyzeMemoryDraftResponse(
				draftId,
				DocumentType.RECEIPT,
				new TypeCardResponse(DocumentType.RECEIPT, DocumentType.RECEIPT.getLabel()),
				false,
				DraftStatus.TYPE_PENDING,
				"CONFIRM_DOCUMENT_TYPE"
			));

		MockMultipartFile image = new MockMultipartFile(
			"image",
			"receipt.jpg",
			"image/jpeg",
			new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF}
		);

		mockMvc.perform(multipart("/memory-drafts/analyze")
				.file(image)
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.draftId").value(draftId.toString()));

		verify(memoryDraftAnalyzeService).analyze(eq(userId), any(MultipartFile.class));
	}

	@Test
	void rejectsUnknownEmailWithoutRevealingWhichCredentialFailed() throws Exception {
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("missing@example.com", "1234567890")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("AUTH_002"))
			.andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void rejectsWrongPasswordWithoutRevealingWhichCredentialFailed() throws Exception {
		signup("memory@example.com", "1234567890");

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials("memory@example.com", "wrongpass")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("AUTH_002"))
			.andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void rejectsMissingBearerTokenWithCommonAuthenticationError() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
			"image",
			"receipt.jpg",
			"image/jpeg",
			new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF}
		);

		mockMvc.perform(multipart("/memory-drafts/analyze").file(image))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("AUTH_001"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void rejectsTamperedBearerToken() throws Exception {
		signup("memory@example.com", "1234567890");
		String accessToken = loginAccessToken("memory@example.com", "1234567890");

		MockMultipartFile image = new MockMultipartFile(
			"image",
			"receipt.jpg",
			"image/jpeg",
			new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF}
		);

		mockMvc.perform(multipart("/memory-drafts/analyze")
				.file(image)
				.header("Authorization", "Bearer " + accessToken + "tampered"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("AUTH_001"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	private void signup(String email, String password) throws Exception {
		mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(email, password)))
			.andExpect(status().isCreated());
	}

	private String loginAccessToken(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(email, password)))
			.andExpect(status().isOk())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data")
			.path("accessToken")
			.asText();
	}

	private String credentials(String email, String password) {
		return """
			{
			  "email": "%s",
			  "password": "%s"
			}
			""".formatted(email, password);
	}
}
