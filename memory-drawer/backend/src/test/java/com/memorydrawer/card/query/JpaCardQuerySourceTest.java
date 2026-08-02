package com.memorydrawer.card.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.FrontImageMode;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.query.dto.CardDetailResponse;
import com.memorydrawer.card.query.dto.YearCardListResponse;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

class JpaCardQuerySourceTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void normalizesLegacyOriginalToTextOnlyWithoutImageUrlInListAndDetail() {
		assertTextOnlyProjection("ORIGINAL");
	}

	@Test
	void keepsTextOnlyWithoutImageUrlInListAndDetail() {
		assertTextOnlyProjection("TEXT_ONLY");
	}

	@Test
	void exposesImageUrlOnlyForBackgroundRemovedMode() {
		JpaCardQuerySource source = source(card("BACKGROUND_REMOVED"));

		YearCardListResponse.LetterFront listFront = listFront(source);
		CardDetailResponse.LetterFront detailFront = detailFront(source);

		String expectedUrl = "/files/cards/%s/front".formatted(CARD_ID);
		assertThat(listFront.frontImageMode()).isEqualTo(FrontImageMode.BACKGROUND_REMOVED);
		assertThat(listFront.frontImageUrl()).isEqualTo(expectedUrl);
		assertThat(detailFront.frontImageMode()).isEqualTo(FrontImageMode.BACKGROUND_REMOVED);
		assertThat(detailFront.frontImageUrl()).isEqualTo(expectedUrl);
	}

	@Test
	void rejectsUnknownStoredImageModeAsCardDataError() {
		JpaCardQuerySource source = source(card("UNKNOWN"));

		assertCardDataError(() -> source.findCards(OWNER_ID, 2026));
		assertCardDataError(() -> source.lookupCard(OWNER_ID, CARD_ID));
	}

	private void assertTextOnlyProjection(String storedMode) {
		JpaCardQuerySource source = source(card(storedMode));

		YearCardListResponse.LetterFront listFront = listFront(source);
		CardDetailResponse.LetterFront detailFront = detailFront(source);

		assertThat(listFront.frontImageMode()).isEqualTo(FrontImageMode.TEXT_ONLY);
		assertThat(listFront.frontImageUrl()).isNull();
		assertThat(detailFront.frontImageMode()).isEqualTo(FrontImageMode.TEXT_ONLY);
		assertThat(detailFront.frontImageUrl()).isNull();
	}

	private YearCardListResponse.LetterFront listFront(JpaCardQuerySource source) {
		return (YearCardListResponse.LetterFront)source.findCards(OWNER_ID, 2026)
			.getFirst()
			.front();
	}

	private CardDetailResponse.LetterFront detailFront(JpaCardQuerySource source) {
		CardLookupResult result = source.lookupCard(OWNER_ID, CARD_ID);
		assertThat(result).isInstanceOf(CardLookupResult.Found.class);
		return (CardDetailResponse.LetterFront)((CardLookupResult.Found)result)
			.card()
			.front();
	}

	private JpaCardQuerySource source(MemoryCard card) {
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		when(repository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(card));
		when(repository.findById(CARD_ID)).thenReturn(Optional.of(card));
		return new JpaCardQuerySource(repository, new ObjectMapper());
	}

	private MemoryCard card(String storedImageMode) {
		return MemoryCard.create(
			CARD_ID,
			OWNER_ID,
			UUID.fromString("00000000-0000-0000-0000-000000000003"),
			DocumentType.LETTER,
			LocalDate.of(2026, 3, 1),
			"{\"ocrText\":\"편지 본문\",\"frontImageMode\":\"%s\"}".formatted(storedImageMode),
			"{\"companions\":[],\"weather\":\"맑음\",\"mood\":\"행복\",\"diaryText\":\"기억\"}",
			"drafts/owner/draft/original.jpg",
			"[]",
			Instant.parse("2026-08-01T00:00:00Z")
		);
	}

	private void assertCardDataError(Runnable action) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARD_003)
			);
	}
}
