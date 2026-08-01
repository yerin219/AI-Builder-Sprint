package com.memorydrawer.card.query;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.memorydrawer.card.query.dto.CardDetailResponse;
import com.memorydrawer.card.query.dto.DrawerListResponse;
import com.memorydrawer.card.query.dto.DrawerListResponse.DrawerItem;
import com.memorydrawer.card.query.dto.YearCardListResponse;

@Service
public final class CardQueryService {

	private final CardQuerySource querySource;

	public CardQueryService(CardQuerySource querySource) {
		this.querySource = Objects.requireNonNull(querySource);
	}

	public DrawerListResponse drawers(UUID ownerId) {
		Objects.requireNonNull(ownerId, "ownerId가 필요합니다.");
		List<DrawerItem> drawers = querySource.findDrawers(ownerId).stream()
			.sorted(Comparator.comparingInt(DrawerItem::year).reversed())
			.toList();
		return new DrawerListResponse(drawers);
	}

	public YearCardListResponse cards(UUID ownerId, int year) {
		Objects.requireNonNull(ownerId, "ownerId가 필요합니다.");
		var cards = querySource.findCards(ownerId, year).stream()
			.sorted(Comparator.comparing(YearCardListResponse.CardItem::memoryDate))
			.toList();
		return new YearCardListResponse(year, cards);
	}

	public CardDetailResponse card(UUID ownerId, UUID cardId) {
		Objects.requireNonNull(ownerId, "ownerId가 필요합니다.");
		Objects.requireNonNull(cardId, "cardId가 필요합니다.");
		return switch (querySource.lookupCard(ownerId, cardId)) {
			case CardLookupResult.Found found -> found.card();
			case CardLookupResult.Forbidden ignored -> throw new CardAccessDeniedException();
			case CardLookupResult.NotFound ignored -> throw new CardNotFoundException();
		};
	}
}
