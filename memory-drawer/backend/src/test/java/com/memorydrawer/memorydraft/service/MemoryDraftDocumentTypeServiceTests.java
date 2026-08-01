package com.memorydrawer.memorydraft.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.api.LetterFrontCandidate;
import com.memorydrawer.memorydraft.api.ReceiptFrontCandidate;
import com.memorydrawer.memorydraft.api.TicketFrontCandidate;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.extract.ExtractedFrontFields;
import com.memorydrawer.memorydraft.extract.InformationExtractClient;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;
import com.memorydrawer.receipt.PurchaseItem;

@ExtendWith(MockitoExtension.class)
class MemoryDraftDocumentTypeServiceTests {

	@Mock
	private MemoryDraftRepository memoryDraftRepository;
	@Mock
	private OriginalImageStorage originalImageStorage;
	@Mock
	private InformationExtractClient informationExtractClient;

	private ObjectMapper objectMapper;
	private MemoryDraftDocumentTypeService service;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().findAndRegisterModules();
		service = new MemoryDraftDocumentTypeService(
			memoryDraftRepository,
			originalImageStorage,
			informationExtractClient,
			objectMapper
		);
	}

	@Test
	void extractsAndStoresReceiptFrontCandidate() throws Exception {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = draft(ownerId, "영수증");
		byte[] imageBytes = new byte[] {1, 2, 3};
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(java.util.Optional.of(draft));
		when(originalImageStorage.load(draft.getOriginalImageKey())).thenReturn(imageBytes);
		when(informationExtractClient.extract(DocumentType.RECEIPT, imageBytes, "image/jpeg"))
			.thenReturn(new ExtractedFrontFields(
				"2026-07-25",
				" 서면카페 ",
				List.of(
					new PurchaseItem(" 아이스 아메리카노 ", 2),
					new PurchaseItem("합 계", 1),
					new PurchaseItem("Discount 1,000", 1),
					new PurchaseItem("ICE", 1),
					new PurchaseItem("Extra Shot", 1),
					new PurchaseItem("치즈케이크", 1),
					new PurchaseItem("Card Holder", 1),
					new PurchaseItem("Total Cereal", 1),
					new PurchaseItem("수량 오류", 0)
				),
				null,
				null,
				null
			));

		var response = service.confirm(ownerId, draft.getId(), "RECEIPT");

		assertThat(response.frontCandidate())
			.isEqualTo(new ReceiptFrontCandidate(
				LocalDate.of(2026, 7, 25),
				"서면카페",
				List.of(
					new PurchaseItem("아이스 아메리카노", 2),
					new PurchaseItem("치즈케이크", 1),
					new PurchaseItem("Card Holder", 1),
					new PurchaseItem("Total Cereal", 1)
				)
			));
		assertThat(response.emptyFields()).isEmpty();
		assertThat(response.draftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
		assertThat(response.nextAction()).isEqualTo("CONFIRM_FRONT");
		assertThat(draft.getDocumentType()).isEqualTo(DocumentType.RECEIPT);
		assertThat(draft.getFrontCandidate()).contains("\"storeName\":\"서면카페\"");
		assertThat(draft.getFrontCandidate())
			.contains("\"purchaseItems\":[{\"name\":\"아이스 아메리카노\",\"quantity\":2}")
			.doesNotContain("합 계", "Discount 1,000", "Extra Shot");
		verify(memoryDraftRepository).saveAndFlush(draft);
	}

	@Test
	void normalizesUncertainTicketFieldsToNull() {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = draft(ownerId, "티켓");
		byte[] imageBytes = new byte[] {4, 5, 6};
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(java.util.Optional.of(draft));
		when(originalImageStorage.load(draft.getOriginalImageKey())).thenReturn(imageBytes);
		when(informationExtractClient.extract(DocumentType.TICKET, imageBytes, "image/jpeg"))
			.thenReturn(new ExtractedFrontFields(
				"날짜 불확실",
				null,
				List.of(),
				" 흠뻑쇼 ",
				" ",
				null
			));

		var response = service.confirm(ownerId, draft.getId(), "TICKET");

		assertThat(response.frontCandidate())
			.isEqualTo(new TicketFrontCandidate(null, "흠뻑쇼", null, null));
		assertThat(response.emptyFields())
			.containsExactly("memoryDate", "venue", "seat");
		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
	}

	@Test
	void extractsLetterCorrespondentsAndReusesParsedText() {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = draft(ownerId, "오늘 함께해 줘서 정말 고마워.");
		byte[] imageBytes = new byte[] {7, 8, 9};
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(java.util.Optional.of(draft));
		when(originalImageStorage.load(draft.getOriginalImageKey())).thenReturn(imageBytes);
		when(informationExtractClient.extract(DocumentType.LETTER, imageBytes, "image/jpeg"))
			.thenReturn(new ExtractedFrontFields(
				null, null, List.of(), null, null, null, "엄마", "지은"
			));

		var response = service.confirm(ownerId, draft.getId(), "LETTER");

		assertThat(response.frontCandidate())
			.isEqualTo(new LetterFrontCandidate(
				null, "오늘 함께해 줘서 정말 고마워.", "엄마", "지은"
			));
		assertThat(response.emptyFields()).containsExactly("memoryDate");
		assertThat(draft.getDocumentType()).isEqualTo(DocumentType.LETTER);
		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.FRONT_PENDING);
	}

	@Test
	void rejectsUnsupportedDocumentTypeBeforeLoadingDraft() {
		assertApiError(
			() -> service.confirm(UUID.randomUUID(), UUID.randomUUID(), "PHOTO"),
			ErrorCode.DOCUMENT_002
		);

		verifyNoInteractions(memoryDraftRepository, originalImageStorage, informationExtractClient);
	}

	@Test
	void rejectsMissingDraft() {
		UUID draftId = UUID.randomUUID();
		when(memoryDraftRepository.findById(draftId)).thenReturn(java.util.Optional.empty());

		assertApiError(
			() -> service.confirm(UUID.randomUUID(), draftId, "RECEIPT"),
			ErrorCode.DRAFT_001
		);
		verifyNoInteractions(originalImageStorage, informationExtractClient);
	}

	@Test
	void rejectsAnotherUsersDraft() {
		MemoryDraft draft = draft(UUID.randomUUID(), "티켓");
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(java.util.Optional.of(draft));

		assertApiError(
			() -> service.confirm(UUID.randomUUID(), draft.getId(), "TICKET"),
			ErrorCode.DRAFT_004
		);
		verifyNoInteractions(originalImageStorage, informationExtractClient);
	}

	@Test
	void rejectsDraftThatIsNotTypePending() {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = draft(ownerId, "손편지");
		draft.confirmDocumentType(
			DocumentType.LETTER,
			"{\"memoryDate\":null,\"ocrText\":\"손편지\"}"
		);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(java.util.Optional.of(draft));

		assertApiError(
			() -> service.confirm(ownerId, draft.getId(), "LETTER"),
			ErrorCode.DRAFT_002
		);
		verifyNoInteractions(originalImageStorage, informationExtractClient);
	}

	@Test
	void keepsTypePendingWhenInformationExtractFails() {
		UUID ownerId = UUID.randomUUID();
		MemoryDraft draft = draft(ownerId, "영수증");
		byte[] imageBytes = new byte[] {1, 2, 3};
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(java.util.Optional.of(draft));
		when(originalImageStorage.load(draft.getOriginalImageKey())).thenReturn(imageBytes);
		when(informationExtractClient.extract(DocumentType.RECEIPT, imageBytes, "image/jpeg"))
			.thenThrow(new ApiException(ErrorCode.AI_001));

		assertApiError(
			() -> service.confirm(ownerId, draft.getId(), "RECEIPT"),
			ErrorCode.AI_001
		);

		assertThat(draft.getDraftStatus()).isEqualTo(DraftStatus.TYPE_PENDING);
		assertThat(draft.getDocumentType()).isNull();
		assertThat(draft.getFrontCandidate()).isNull();
		verify(memoryDraftRepository, never()).saveAndFlush(draft);
	}

	private MemoryDraft draft(UUID ownerId, String text) {
		try {
			Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
			return MemoryDraft.analyzed(
				UUID.randomUUID(),
				ownerId,
				"drafts/%s/original.jpg".formatted(UUID.randomUUID()),
				"image/jpeg",
				objectMapper.writeValueAsString(new ParsedContent(text, "")),
				DocumentType.TICKET,
				now,
				now.plus(7, ChronoUnit.DAYS)
			);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	private void assertApiError(Runnable action, ErrorCode errorCode) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(errorCode)
			);
	}
}
