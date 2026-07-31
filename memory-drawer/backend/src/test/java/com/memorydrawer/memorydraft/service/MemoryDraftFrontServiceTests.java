package com.memorydrawer.memorydraft.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.api.ConfirmFrontRequest;
import com.memorydrawer.memorydraft.api.FrontImageMode;
import com.memorydrawer.memorydraft.api.LetterConfirmedFront;
import com.memorydrawer.memorydraft.api.ReceiptConfirmedFront;
import com.memorydrawer.memorydraft.api.TicketConfirmedFront;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@ExtendWith(MockitoExtension.class)
class MemoryDraftFrontServiceTests {

	@Mock
	private MemoryDraftRepository memoryDraftRepository;

	private ObjectMapper objectMapper;
	private MemoryDraftFrontService service;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
			.findAndRegisterModules()
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		service = new MemoryDraftFrontService(memoryDraftRepository, objectMapper);
	}

	@Test
	void confirmsReceiptFrontWithUsersFinalValues() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.RECEIPT);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		var response = service.confirm(
			ownerId,
			draft.getId(),
			request(
				LocalDate.of(2026, 7, 25),
				"""
					{
					  "storeName": " 서면카페 "
					}
					"""
			)
		);

		assertThat(response.documentType()).isEqualTo(DocumentType.RECEIPT);
		assertThat(response.memoryDate()).isEqualTo(LocalDate.of(2026, 7, 25));
		assertThat(response.front()).isEqualTo(new ReceiptConfirmedFront("서면카페"));
		assertThat(response.draftStatus()).isEqualTo(DraftStatus.FRONT_CONFIRMED);
		assertThat(response.nextAction()).isEqualTo("WRITE_BACK");
		assertThat(draft.getFrontCandidate())
			.contains("\"memoryDate\":\"2026-07-25\"")
			.contains("\"storeName\":\"서면카페\"");
		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.FRONT_CONFIRMED);
		verify(memoryDraftRepository).saveAndFlush(draft);
	}

	@Test
	void reconfirmsFrontWithUpdatedValuesBeforeCardIsSaved() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.RECEIPT);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		service.confirm(
			ownerId,
			draft.getId(),
			request(
				LocalDate.of(2026, 7, 25),
				"""
					{
					  "storeName": "처음 확인한 가게명"
					}
					"""
			)
		);

		var response = service.confirm(
			ownerId,
			draft.getId(),
			request(
				LocalDate.of(2026, 7, 26),
				"""
					{
					  "storeName": "수정한 가게명"
					}
					"""
			)
		);

		assertThat(response.memoryDate()).isEqualTo(LocalDate.of(2026, 7, 26));
		assertThat(response.front()).isEqualTo(new ReceiptConfirmedFront("수정한 가게명"));
		assertThat(response.draftStatus()).isEqualTo(DraftStatus.FRONT_CONFIRMED);
		assertThat(draft.getFrontCandidate())
			.contains("\"memoryDate\":\"2026-07-26\"")
			.contains("\"storeName\":\"수정한 가게명\"")
			.doesNotContain("처음 확인한 가게명");
		verify(memoryDraftRepository, org.mockito.Mockito.times(2)).saveAndFlush(draft);
	}

	@Test
	void confirmsTicketFrontAndKeepsMissingSeatNull() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.TICKET);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		var response = service.confirm(
			ownerId,
			draft.getId(),
			request(
				LocalDate.of(2026, 7, 25),
				"""
					{
					  "eventName": " 흠뻑쇼 ",
					  "venue": " 부산아시아드주경기장 ",
					  "seat": null
					}
					"""
			)
		);

		assertThat(response.front()).isEqualTo(
			new TicketConfirmedFront("흠뻑쇼", "부산아시아드주경기장", null)
		);
		assertThat(draft.getFrontCandidate()).contains("\"seat\":null");
		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.FRONT_CONFIRMED);
	}

	@Test
	void confirmsLetterFrontWithOriginalImageFallback() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.LETTER);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		var response = service.confirm(
			ownerId,
			draft.getId(),
			request(
				LocalDate.of(2026, 3, 18),
				"""
					{
					  "ocrText": " 오늘 함께해 줘서 정말 고마워. "
					}
					"""
			)
		);

		assertThat(response.front()).isEqualTo(
			new LetterConfirmedFront(
				"오늘 함께해 줘서 정말 고마워.",
				FrontImageMode.ORIGINAL
			)
		);
		assertThat(draft.getFrontCandidate()).contains("\"frontImageMode\":\"ORIGINAL\"");
		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.FRONT_CONFIRMED);
	}

	@Test
	void rejectsMissingRequiredReceiptField() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.RECEIPT);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		assertApiError(
			() -> service.confirm(
				ownerId,
				draft.getId(),
				request(LocalDate.of(2026, 7, 25), "{}")
			),
			ErrorCode.VALIDATION_001
		);

		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
		verify(memoryDraftRepository, never()).saveAndFlush(draft);
	}

	@Test
	void rejectsFieldsThatDoNotBelongToConfirmedDocumentType() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.LETTER);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		assertApiError(
			() -> service.confirm(
				ownerId,
				draft.getId(),
				request(
					LocalDate.of(2026, 3, 18),
					"""
						{
						  "ocrText": "편지 본문",
						  "frontImageMode": "BACKGROUND_REMOVED"
						}
						"""
				)
			),
			ErrorCode.VALIDATION_001
		);

		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
		verify(memoryDraftRepository, never()).saveAndFlush(draft);
	}

	@Test
	void rejectsNonTextTicketSeat() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.TICKET);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		assertApiError(
			() -> service.confirm(
				ownerId,
				draft.getId(),
				request(
					LocalDate.of(2026, 7, 25),
					"""
						{
						  "eventName": "흠뻑쇼",
						  "venue": "부산아시아드주경기장",
						  "seat": 10
						}
						"""
				)
			),
			ErrorCode.VALIDATION_001
		);
		verify(memoryDraftRepository, never()).saveAndFlush(draft);
	}

	@Test
	void rejectsMissingMemoryDate() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = frontPendingDraft(ownerId, DocumentType.RECEIPT);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		assertApiError(
			() -> service.confirm(
				ownerId,
				draft.getId(),
				request(null, """
					{
					  "storeName": "서면카페"
					}
					""")
			),
			ErrorCode.VALIDATION_001
		);
		verify(memoryDraftRepository, never()).saveAndFlush(draft);
	}

	@Test
	void rejectsMissingDraft() throws Exception {
		UUID draftId = UUID.randomUUID();
		when(memoryDraftRepository.findById(draftId)).thenReturn(Optional.empty());

		assertApiError(
			() -> service.confirm(
				UUID.randomUUID(),
				draftId,
				request(LocalDate.of(2026, 7, 25), "{}")
			),
			ErrorCode.DRAFT_001
		);
	}

	@Test
	void rejectsAnotherUsersDraft() throws Exception {
		MemoryDraft draft = frontPendingDraft(UUID.randomUUID(), DocumentType.RECEIPT);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		assertApiError(
			() -> service.confirm(
				UUID.randomUUID(),
				draft.getId(),
				request(
					LocalDate.of(2026, 7, 25),
					"""
						{
						  "storeName": "서면카페"
						}
						"""
				)
			),
			ErrorCode.DRAFT_004
		);
		verify(memoryDraftRepository, never()).saveAndFlush(draft);
	}

	@Test
	void rejectsDraftThatIsNotFrontPending() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = analyzedDraft(ownerId);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		assertApiError(
			() -> service.confirm(
				ownerId,
				draft.getId(),
				request(
					LocalDate.of(2026, 7, 25),
					"""
						{
						  "storeName": "서면카페"
						}
						"""
				)
			),
			ErrorCode.DRAFT_002
		);
		verify(memoryDraftRepository, never()).saveAndFlush(draft);
	}

	private ConfirmFrontRequest request(LocalDate memoryDate, String frontJson) {
		try {
			JsonNode front = objectMapper.readTree(frontJson);
			return new ConfirmFrontRequest(memoryDate, front);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("테스트 요청 JSON을 만들 수 없습니다.", exception);
		}
	}

	private MemoryDraft frontPendingDraft(UUID ownerId, DocumentType documentType)
		throws Exception {
		MemoryDraft draft = analyzedDraft(ownerId);
		draft.confirmDocumentType(
			documentType,
			objectMapper.writeValueAsString(objectMapper.createObjectNode())
		);
		return draft;
	}

	private MemoryDraft analyzedDraft(UUID ownerId) throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		return MemoryDraft.analyzed(
			UUID.randomUUID(),
			ownerId,
			"drafts/%s/original.jpg".formatted(UUID.randomUUID()),
			"image/jpeg",
			objectMapper.writeValueAsString(new ParsedContent("문서 본문", "")),
			DocumentType.RECEIPT,
			now,
			now.plus(7, ChronoUnit.DAYS)
		);
	}

	private void assertApiError(Runnable action, ErrorCode errorCode) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(errorCode)
			);
	}
}
