package com.memorydrawer.card.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.memorydrawer.memorydraft.image.LetterFrontImageStorage;

class CardImageServiceTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID DRAFT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

	private MemoryCardRepository memoryCardRepository;
	private LetterFrontImageStorage letterFrontImageStorage;
	private BackPhotoStorage backPhotoStorage;
	private CardImageService cardImageService;

	@BeforeEach
	void setUp() {
		memoryCardRepository = mock(MemoryCardRepository.class);
		letterFrontImageStorage = mock(LetterFrontImageStorage.class);
		backPhotoStorage = mock(BackPhotoStorage.class);
		cardImageService = new CardImageService(
			memoryCardRepository,
			letterFrontImageStorage,
			backPhotoStorage,
			new ObjectMapper()
		);
	}

	@Test
	void rejectsTextOnlyLetterFrontWithoutLoadingAnyImage() {
		MemoryCard card = letterCard("TEXT_ONLY");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
		verifyNoInteractions(letterFrontImageStorage);
	}

	@Test
	void rejectsLegacyOriginalLetterFrontWithoutExposingSourceImage() {
		MemoryCard card = letterCard("ORIGINAL");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
		verifyNoInteractions(letterFrontImageStorage);
	}

	@Test
	void loadsStoredBackgroundRemovedPngForLetter() {
		MemoryCard card = letterCard("BACKGROUND_REMOVED");
		byte[] png = pngBytes();
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		when(letterFrontImageStorage.load(OWNER_ID, DRAFT_ID)).thenReturn(png);

		CardImageResource resource = cardImageService.front(OWNER_ID, CARD_ID);

		assertThat(resource.bytes()).containsExactly(png);
		assertThat(resource.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
	}

	@Test
	void mapsMissingBackgroundRemovedImageToCardNotFound() {
		MemoryCard card = letterCard("BACKGROUND_REMOVED");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		when(letterFrontImageStorage.load(OWNER_ID, DRAFT_ID)).thenThrow(
			new IllegalStateException("image is missing", new NoSuchFileException("letter-front.png"))
		);

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void mapsCorruptBackgroundRemovedImageToCardDataError() {
		MemoryCard card = letterCard("BACKGROUND_REMOVED");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
		when(letterFrontImageStorage.load(OWNER_ID, DRAFT_ID)).thenReturn(new byte[] {1, 2, 3});

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARD_003)
			);
	}

	@Test
	void rejectsInvalidStoredLetterImageModeAsCardDataError() {
		MemoryCard card = letterCard("UNKNOWN");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARD_003)
			);
		verifyNoInteractions(letterFrontImageStorage);
	}

	@Test
	void rejectsNullLetterFrontDataAsCardDataError() {
		MemoryCard card = card(
			DocumentType.LETTER,
			"drafts/owner/card/original.jpg",
			"[]",
			null
		);
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARD_003)
			);
		verifyNoInteractions(letterFrontImageStorage);
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
		verifyNoInteractions(letterFrontImageStorage);
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
	void rejectsReceiptFrontImageBeforeLoadingStoredFile() {
		MemoryCard card = card(DocumentType.RECEIPT, "drafts/owner/card/original.jpg", "[]");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
		verifyNoInteractions(letterFrontImageStorage);
	}

	@Test
	void rejectsTicketFrontImageBeforeLoadingStoredFile() {
		MemoryCard card = card(DocumentType.TICKET, "drafts/owner/card/original.jpg", "[]");
		when(memoryCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardImageService.front(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class);
		verifyNoInteractions(letterFrontImageStorage);
	}

	private byte[] pngBytes() {
		return new byte[] {
			(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1
		};
	}

	private MemoryCard letterCard(String imageMode) {
		return card(
			DocumentType.LETTER,
			"drafts/owner/card/original.jpg",
			"[]",
			"{\"ocrText\":\"편지 본문\",\"frontImageMode\":\"%s\"}".formatted(imageMode)
		);
	}

	private MemoryCard card(DocumentType documentType, String originalImageKey, String backPhotoKeys) {
		return card(
			documentType,
			originalImageKey,
			backPhotoKeys,
			"{\"storeName\":\"가게\"}"
		);
	}

	private MemoryCard card(
		DocumentType documentType,
		String originalImageKey,
		String backPhotoKeys,
		String frontData
	) {
		return MemoryCard.create(
			CARD_ID,
			OWNER_ID,
			DRAFT_ID,
			documentType,
			LocalDate.of(2026, 7, 31),
			frontData,
			"{}",
			originalImageKey,
			backPhotoKeys,
			Instant.parse("2026-07-31T00:00:00Z")
		);
	}
}
