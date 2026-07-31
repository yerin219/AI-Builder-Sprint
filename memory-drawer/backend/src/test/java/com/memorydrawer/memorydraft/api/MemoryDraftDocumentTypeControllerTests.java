package com.memorydrawer.memorydraft.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.extract.InformationExtractClient;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemoryDraftDocumentTypeControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemoryDraftRepository memoryDraftRepository;

	@MockitoBean
	private InformationExtractClient informationExtractClient;

	@Test
	void confirmsLetterTypeWithAuthenticatedOwnerAndPersistsFrontCandidate() throws Exception {
		LoginSession session = signupAndLogin("letter@example.com", "1234567890");
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		MemoryDraft draft = MemoryDraft.analyzed(
			UUID.randomUUID(),
			session.userId(),
			"drafts/%s/original.jpg".formatted(UUID.randomUUID()),
			"image/jpeg",
			objectMapper.writeValueAsString(
				new ParsedContent("오늘 함께해 줘서 정말 고마워.", "")
			),
			DocumentType.TICKET,
			now,
			now.plus(7, ChronoUnit.DAYS)
		);
		memoryDraftRepository.saveAndFlush(draft);

		mockMvc.perform(post(
				"/memory-drafts/{draftId}/document-type/confirm",
				draft.getId()
			)
				.header("Authorization", "Bearer " + session.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "documentType": "LETTER"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("손편지 내용을 추출했습니다."))
			.andExpect(jsonPath("$.data.draftId").value(draft.getId().toString()))
			.andExpect(jsonPath("$.data.documentType").value("LETTER"))
			.andExpect(jsonPath("$.data.frontCandidate.memoryDate").doesNotExist())
			.andExpect(jsonPath("$.data.frontCandidate.ocrText")
				.value("오늘 함께해 줘서 정말 고마워."))
			.andExpect(jsonPath("$.data.emptyFields[0]").value("memoryDate"))
			.andExpect(jsonPath("$.data.draftStatus").value("FRONT_PENDING"))
			.andExpect(jsonPath("$.data.nextAction").value("CONFIRM_FRONT"));

		MemoryDraft saved = memoryDraftRepository.findById(draft.getId()).orElseThrow();
		assertThat(saved.getDocumentType()).isEqualTo(DocumentType.LETTER);
		assertThat(saved.getFrontCandidate()).contains("오늘 함께해 줘서 정말 고마워.");
		assertThat(saved.getDraftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
		verifyNoInteractions(informationExtractClient);
	}

	private LoginSession signupAndLogin(String email, String password) throws Exception {
		mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(email, password)))
			.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials(email, password)))
			.andExpect(status().isOk())
			.andReturn();
		JsonNode data = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.path("data");
		return new LoginSession(
			UUID.fromString(data.path("userId").asText()),
			data.path("accessToken").asText()
		);
	}

	private String credentials(String email, String password) {
		return """
			{
			  "email": "%s",
			  "password": "%s"
			}
			""".formatted(email, password);
	}

	private record LoginSession(UUID userId, String accessToken) {
	}
}
