package com.memorydrawer.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.memorydrawer.memorydraft.api.AnalyzeMemoryDraftResponse;
import com.memorydrawer.memorydraft.api.TypeCardResponse;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.service.MemoryDraftAnalyzeService;

@ActiveProfiles({"test", "local"})
@SpringBootTest(properties = {
	"spring.security.user.name=11111111-1111-1111-1111-111111111111",
	"spring.security.user.password=local-test-password"
})
@AutoConfigureMockMvc
class LocalSecurityConfigTests {

	private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
	private static final String PASSWORD = "local-test-password";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemoryDraftAnalyzeService memoryDraftAnalyzeService;

	@Test
	void acceptsLocalBasicAuthenticationAndPassesUuidToApi3() throws Exception {
		UUID draftId = UUID.randomUUID();
		given(memoryDraftAnalyzeService.analyze(any(UUID.class), any(MultipartFile.class)))
			.willReturn(new AnalyzeMemoryDraftResponse(
				draftId,
				DocumentType.TICKET,
				new TypeCardResponse(DocumentType.TICKET, DocumentType.TICKET.getLabel()),
				false,
				DraftStatus.TYPE_PENDING,
				"CONFIRM_DOCUMENT_TYPE"
			));

		MockMultipartFile image = new MockMultipartFile(
			"image",
			"ticket.jpg",
			"image/jpeg",
			new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF}
		);

		mockMvc.perform(multipart("/memory-drafts/analyze")
				.file(image)
				.header("Authorization", basicAuthorization(USER_ID, PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.draftId").value(draftId.toString()))
			.andExpect(jsonPath("$.data.draftStatus").value("TYPE_PENDING"));
	}

	@Test
	void rejectsApi3WithoutLocalBasicAuthentication() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
			"image",
			"ticket.jpg",
			"image/jpeg",
			new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF}
		);

		mockMvc.perform(multipart("/memory-drafts/analyze").file(image))
			.andExpect(status().isUnauthorized());
	}

	private String basicAuthorization(String username, String password) {
		String credentials = username + ":" + password;
		return "Basic " + Base64.getEncoder()
			.encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}
}
