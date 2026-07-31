package com.memorydrawer.card.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.create.dto.CardBackRequest;
import com.memorydrawer.card.create.dto.CardCreateRequest;
import com.memorydrawer.card.image.BackPhotoStorage;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.image.ImageFileValidator;
import com.memorydrawer.memorydraft.image.ValidatedImage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

class CardCreateServiceTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID DRAFT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void deletesStoredPhotosAndReturnsCardErrorWhenDatabaseSaveFails() {
		MemoryDraftRepository draftRepository = mock(MemoryDraftRepository.class);
		MemoryCardRepository cardRepository = mock(MemoryCardRepository.class);
		ImageFileValidator imageValidator = mock(ImageFileValidator.class);
		BackPhotoStorage photoStorage = mock(BackPhotoStorage.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		CardCreateService service = new CardCreateService(
			draftRepository,
			cardRepository,
			imageValidator,
			photoStorage,
			objectMapper,
			Clock.fixed(Instant.parse("2026-07-31T00:00:00Z"), ZoneOffset.UTC)
		);

		MemoryDraft draft = frontConfirmedDraft();
		MockMultipartFile photo = new MockMultipartFile(
			"backPhotos", "memory.jpg", "image/jpeg", new byte[] {1, 2, 3}
		);
		ValidatedImage validatedImage = new ValidatedImage(
			new byte[] {1, 2, 3}, "image/jpeg", "jpg"
		);
		List<String> storedKeys = List.of("cards/owner/card/back/1.jpg");

		when(draftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draft));
		when(cardRepository.existsByDraftId(DRAFT_ID)).thenReturn(false);
		when(imageValidator.validate(photo)).thenReturn(validatedImage);
		when(photoStorage.store(any(), any(), any())).thenReturn(storedKeys);
		when(cardRepository.saveAndFlush(any())).thenThrow(new RuntimeException("database unavailable"));

		CardCreateRequest request = new CardCreateRequest(
			DRAFT_ID,
			new CardBackRequest(List.of(), "맑음", "행복", "기억", null, null, null, null)
		);

		assertThatThrownBy(() -> service.create(OWNER_ID, request, List.of(photo)))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARD_003)
			);
		verify(photoStorage).deleteAll(storedKeys);
	}

	private MemoryDraft frontConfirmedDraft() {
		MemoryDraft draft = MemoryDraft.analyzed(
			DRAFT_ID,
			OWNER_ID,
			"drafts/owner/draft/original.jpg",
			"image/jpeg",
			"parsed",
			DocumentType.RECEIPT,
			Instant.parse("2026-07-30T00:00:00Z"),
			Instant.parse("2026-08-06T00:00:00Z")
		);
		draft.confirmDocumentType(DocumentType.RECEIPT, "{}");
		draft.confirmFront("{\"memoryDate\":\"2026-07-31\",\"front\":{\"storeName\":\"가게\"}}");
		return draft;
	}
}
