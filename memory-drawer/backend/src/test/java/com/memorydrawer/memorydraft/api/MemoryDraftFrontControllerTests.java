package com.memorydrawer.memorydraft.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemoryDraftFrontControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemoryDraftRepository memoryDraftRepository;

	@Test
	void confirmsReceiptFrontWithAuthenticatedOwnersFinalValues() throws Exception {
		LoginSession session = signupAndLogin("front@example.com", "1234567890");
		MemoryDraft draft = frontPendingDraft(session.userId(), DocumentType.RECEIPT);
		memoryDraftRepository.saveAndFlush(draft);

		mockMvc.perform(put(
				"/memory-drafts/{draftId}/front/confirm",
				draft.getId()
			)
				.header("Authorization", "Bearer " + session.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "memoryDate": "2026-07-12",
					  "front": {
					    "storeName": "직접 확인한 가게명"
					  }
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("카드 앞면이 확정되었습니다."))
			.andExpect(jsonPath("$.data.draftId").value(draft.getId().toString()))
			.andExpect(jsonPath("$.data.documentType").value("RECEIPT"))
			.andExpect(jsonPath("$.data.memoryDate").value("2026-07-12"))
			.andExpect(jsonPath("$.data.front.storeName").value("직접 확인한 가게명"))
			.andExpect(jsonPath("$.data.draftStatus").value("FRONT_CONFIRMED"))
			.andExpect(jsonPath("$.data.nextAction").value("WRITE_BACK"));

		MemoryDraft saved = memoryDraftRepository.findById(draft.getId()).orElseThrow();
		assertThat(saved.getFrontCandidate())
			.contains("\"memoryDate\":\"2026-07-12\"")
			.contains("\"storeName\":\"직접 확인한 가게명\"");
		assertThat(saved.getDraftStatus()).isEqualTo(DraftStatus.FRONT_CONFIRMED);
	}

	@Test
	void updatesAlreadyConfirmedFrontBeforeFinalSave() throws Exception {
		LoginSession session = signupAndLogin(
			"front-reconfirm@example.com",
			"1234567890"
		);
		MemoryDraft draft = frontPendingDraft(session.userId(), DocumentType.RECEIPT);
		memoryDraftRepository.saveAndFlush(draft);

		confirmReceiptFront(
			session,
			draft.getId(),
			"2026-07-12",
			"처음 확인한 가게명"
		);

		mockMvc.perform(put(
				"/memory-drafts/{draftId}/front/confirm",
				draft.getId()
			)
				.header("Authorization", "Bearer " + session.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "memoryDate": "2026-07-13",
					  "front": {
					    "storeName": "수정한 가게명"
					  }
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.memoryDate").value("2026-07-13"))
			.andExpect(jsonPath("$.data.front.storeName").value("수정한 가게명"))
			.andExpect(jsonPath("$.data.draftStatus").value("FRONT_CONFIRMED"));

		MemoryDraft saved = memoryDraftRepository.findById(draft.getId()).orElseThrow();
		assertThat(saved.getFrontCandidate())
			.contains("\"memoryDate\":\"2026-07-13\"")
			.contains("\"storeName\":\"수정한 가게명\"")
			.doesNotContain("처음 확인한 가게명");
	}

	@Test
	void rejectsInvalidMemoryDateFormat() throws Exception {
		LoginSession session = signupAndLogin("date@example.com", "1234567890");
		MemoryDraft draft = frontPendingDraft(session.userId(), DocumentType.RECEIPT);
		memoryDraftRepository.saveAndFlush(draft);

		mockMvc.perform(put(
				"/memory-drafts/{draftId}/front/confirm",
				draft.getId()
			)
				.header("Authorization", "Bearer " + session.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "memoryDate": "2026.07.12",
					  "front": {
					    "storeName": "서면카페"
					  }
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("VALIDATION_001"));

		MemoryDraft saved = memoryDraftRepository.findById(draft.getId()).orElseThrow();
		assertThat(saved.getDraftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
	}

	@Test
	void rejectsMissingRequiredFrontValue() throws Exception {
		LoginSession session = signupAndLogin("missing@example.com", "1234567890");
		MemoryDraft draft = frontPendingDraft(session.userId(), DocumentType.TICKET);
		memoryDraftRepository.saveAndFlush(draft);

		mockMvc.perform(put(
				"/memory-drafts/{draftId}/front/confirm",
				draft.getId()
			)
				.header("Authorization", "Bearer " + session.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "memoryDate": "2026-07-25",
					  "front": {
					    "eventName": "흠뻑쇼",
					    "seat": null
					  }
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("VALIDATION_001"));

		MemoryDraft saved = memoryDraftRepository.findById(draft.getId()).orElseThrow();
		assertThat(saved.getDraftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
	}

	@Test
	void confirmsLetterFrontWithServerSelectedImageMode() throws Exception {
		LoginSession session = signupAndLogin("letter-front@example.com", "1234567890");
		MemoryDraft draft = frontPendingDraft(session.userId(), DocumentType.LETTER);
		memoryDraftRepository.saveAndFlush(draft);

		mockMvc.perform(put(
				"/memory-drafts/{draftId}/front/confirm",
				draft.getId()
			)
				.header("Authorization", "Bearer " + session.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "memoryDate": "2026-03-18",
					  "front": {
					    "ocrText": "오늘 함께해 줘서 정말 고마워."
					  }
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.documentType").value("LETTER"))
			.andExpect(jsonPath("$.data.memoryDate").value("2026-03-18"))
			.andExpect(jsonPath("$.data.front.ocrText")
				.value("오늘 함께해 줘서 정말 고마워."))
			.andExpect(jsonPath("$.data.front.frontImageMode").value("ORIGINAL"))
			.andExpect(jsonPath("$.data.draftStatus").value("FRONT_CONFIRMED"));
	}

	private MemoryDraft frontPendingDraft(UUID ownerId, DocumentType documentType)
		throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		MemoryDraft draft = MemoryDraft.analyzed(
			UUID.randomUUID(),
			ownerId,
			"drafts/%s/original.jpg".formatted(UUID.randomUUID()),
			"image/jpeg",
			objectMapper.writeValueAsString(new ParsedContent("문서 본문", "")),
			documentType,
			now,
			now.plus(7, ChronoUnit.DAYS)
		);
		draft.confirmDocumentType(documentType, "{}");
		return draft;
	}

	private void confirmReceiptFront(
		LoginSession session,
		UUID draftId,
		String memoryDate,
		String storeName
	) throws Exception {
		mockMvc.perform(put(
				"/memory-drafts/{draftId}/front/confirm",
				draftId
			)
				.header("Authorization", "Bearer " + session.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "memoryDate": "%s",
					  "front": {
					    "storeName": "%s"
					  }
					}
					""".formatted(memoryDate, storeName)))
			.andExpect(status().isOk());
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
