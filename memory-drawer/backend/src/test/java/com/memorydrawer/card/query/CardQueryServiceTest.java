package com.memorydrawer.card.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.card.query.dto.CardDetailResponse;
import com.memorydrawer.card.query.dto.DrawerListResponse.DrawerItem;
import com.memorydrawer.card.query.dto.YearCardListResponse;
import com.memorydrawer.card.query.dto.YearCardListResponse.CardItem;

class CardQueryServiceTest {

	private static final UUID CARD_ID = UUID.fromString("e89ed42d-1a89-4eea-8ddc-dca90a5c78c4");
	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

	@Test
	void sortsDrawersFromMostRecentYear() {
		var source = new StubQuerySource(
			List.of(new DrawerItem(2024, 1), new DrawerItem(2026, 2), new DrawerItem(2025, 3)),
			List.of(),
			new CardLookupResult.NotFound()
		);

		var response = new CardQueryService(source).drawers(OWNER_ID);

		assertThat(response.drawers()).extracting(DrawerItem::year).containsExactly(2026, 2025, 2024);
	}

	@Test
	void returnsEmptyArraysInsteadOfNotFoundForLists() {
		var service = new CardQueryService(new StubQuerySource(
			List.of(), List.of(), new CardLookupResult.NotFound()
		));

		assertThat(service.drawers(OWNER_ID).drawers()).isEmpty();
		assertThat(service.cards(OWNER_ID, 2026).cards()).isEmpty();
	}

	@Test
	void sortsCardsFromOldestMemoryDate() {
		CardItem older = ticketItem(UUID.fromString("00000000-0000-0000-0000-000000000001"), LocalDate.of(2026, 1, 1));
		CardItem newer = ticketItem(UUID.fromString("00000000-0000-0000-0000-000000000002"), LocalDate.of(2026, 12, 31));
		var service = new CardQueryService(new StubQuerySource(
			List.of(), List.of(newer, older), new CardLookupResult.NotFound()
		));

		YearCardListResponse response = service.cards(OWNER_ID, 2026);

		assertThat(response.cards()).containsExactly(older, newer);
	}

	@Test
	void returnsOwnedCardDetail() {
		CardDetailResponse detail = directTicketDetail();
		var service = new CardQueryService(new StubQuerySource(
			List.of(), List.of(), new CardLookupResult.Found(detail)
		));

		assertThat(service.card(OWNER_ID, CARD_ID)).isSameAs(detail);
	}

	@Test
	void distinguishesForbiddenCardFromMissingCard() {
		var forbiddenService = new CardQueryService(new StubQuerySource(
			List.of(), List.of(), new CardLookupResult.Forbidden()
		));
		var missingService = new CardQueryService(new StubQuerySource(
			List.of(), List.of(), new CardLookupResult.NotFound()
		));

		assertThatThrownBy(() -> forbiddenService.card(OWNER_ID, CARD_ID))
			.isInstanceOf(CardAccessDeniedException.class)
			.satisfies(exception -> assertThat(CardAccessDeniedException.ERROR_CODE).isEqualTo("CARD_001"));
		assertThatThrownBy(() -> missingService.card(OWNER_ID, CARD_ID))
			.isInstanceOf(CardNotFoundException.class)
			.satisfies(exception -> assertThat(CardNotFoundException.ERROR_CODE).isEqualTo("CARD_002"));
	}

	private static CardItem ticketItem(UUID cardId, LocalDate memoryDate) {
		return new CardItem(
			cardId,
			DocumentType.TICKET,
			memoryDate,
			new YearCardListResponse.TicketFront("행사", "장소", null)
		);
	}

	private static CardDetailResponse directTicketDetail() {
		return new CardDetailResponse(
			CARD_ID,
			DocumentType.TICKET,
			LocalDate.of(2026, 7, 25),
			new CardDetailResponse.TicketFront("행사", "장소", null),
			new CardDetailResponse.DirectTicketBack(
				List.of(), "맑음", "행복", WritingMode.DIRECT, "제목", "추억"
			)
		);
	}

	private record StubQuerySource(
		List<DrawerItem> drawers,
		List<CardItem> cards,
		CardLookupResult lookupResult
	) implements CardQuerySource {

		@Override
		public List<DrawerItem> findDrawers(UUID ownerId) {
			return drawers;
		}

		@Override
		public List<CardItem> findCards(UUID ownerId, int year) {
			return cards;
		}

		@Override
		public CardLookupResult lookupCard(UUID ownerId, UUID cardId) {
			return lookupResult;
		}
	}
}
