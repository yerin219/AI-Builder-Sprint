package com.memorydrawer.card.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.query.CardAccessDeniedException;
import com.memorydrawer.card.query.CardNotFoundException;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;

class CardImageServiceTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

	private MemoryCardRepository memoryCardRepository;
	private OriginalImageStorage originalImageStorage;
	private BackPhotoStorage backPhotoStorage;
	private CardImageService cardImageService;

	@BeforeEach
	void setUp() {
		memoryCardRepository = mock(MemoryCardRepository.class);
		originalImageStorage = mock(OriginalImageStorage.class);
		backPhotoStorage = mock(BackPhotoStorage.class);
		cardImageService = new CardImageService(
			memoryCardRepository,
			originalImageStorage,
			backPhotoStorage,
			new ObjectMapper()
		);
	}

	@Test
	void loadsOwnedFrontImageWithStoredMediaType() {
		MemoryCard card = card(DocumentType.LETTER, "drafts/owner/card/original.jpg", "[]");
		byte[] bytes = {1, 2, 3};
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		when(originalImageStorage.load(card.getOriginalImageKey())).thenReturn(bytes);

		CardImageResource resource = cardImageService.front(OWNER_ID, CARD_ID);

		assertThat(resource.bytes()).containsExactly(bytes);
		assertThat(resource.mediaType()).isEqualTo(MediaType.IMAGE_JPEG);
	}

	@Test
	void loadsRequestedOneBasedBackPhoto() {
		MemoryCard card = card(
			DocumentType.RECEIPT,
			"drafts/owner/card/original.jpg",
			"[\"cards/owner/card/back/1.png\",\"cards/owner/card/back/2.webp\"]"
		);
		byte[] bytes = {4, 5, 6};
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		when(backPhotoStorage.load("cards/owner/card/back/2.webp")).thenReturn(bytes);

		CardImageResource resource = cardImageService.back(OWNER_ID, CARD_ID, 2);

		assertThat(resource.bytes()).containsExactly(bytes);
		assertThat(resource.mediaType()).isEqualTo(MediaType.parseMediaType("image/webp"));
	}

	@Test
	void rejectsAnotherUsersCardBeforeLoadingImage() {
		MemoryCard card = card(DocumentType.RECEIPT, "drafts/owner/card/original.jpg", "[]");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OTHER_OWNER_ID, CARD_ID))
			.isInstanceOf(CardAccessDeniedException.class);
		verify(originalImageStorage, never()).load(card.getOriginalImageKey());
	}

	@Test
	void distinguishesMissingCardAndMissingBackPhoto() {
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);

		MemoryCard card = card(DocumentType.RECEIPT, "drafts/owner/card/original.jpg", "[]");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		assertThatThrownBy(() -> cardImageService.back(OWNER_ID, CARD_ID, 1))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void mapsCorruptPhotoMetadataToCardError() {
		MemoryCard card = card(DocumentType.RECEIPT, "drafts/owner/card/original.jpg", "{not-json}");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.back(OWNER_ID, CARD_ID, 1))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARD_003)
			);
	}

	@Test
	void mapsMissingStoredImageToCardNotFound() {
		MemoryCard card = card(DocumentType.LETTER, "drafts/owner/card/original.jpg", "[]");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		when(originalImageStorage.load(card.getOriginalImageKey())).thenThrow(
			new IllegalStateException("image is missing", new NoSuchFileException(card.getOriginalImageKey()))
		);

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void rejectsReceiptFrontImageBeforeLoadingStoredFile() {
		MemoryCard card = card(DocumentType.RECEIPT, "drafts/owner/card/original.jpg", "[]");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
		verify(originalImageStorage, never()).load(card.getOriginalImageKey());
	}

	@Test
	void rejectsTicketFrontImageBeforeLoadingStoredFile() {
		MemoryCard card = card(DocumentType.TICKET, "drafts/owner/card/original.jpg", "[]");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
		verify(originalImageStorage, never()).load(card.getOriginalImageKey());
	}

	private MemoryCard card(DocumentType documentType, String originalImageKey, String backPhotoKeys) {
		return MemoryCard.create(
			CARD_ID,
			OWNER_ID,
			UUID.fromString("00000000-0000-0000-0000-000000000004"),
			documentType,
			LocalDate.of(2026, 7, 31),
			"{\"storeName\":\"가게\"}",
			"{}",
			originalImageKey,
			backPhotoKeys,
			Instant.parse("2026-07-31T00:00:00Z")
		);
	}
}
