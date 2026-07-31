package com.memorydrawer.card.query;

import java.util.List;
import java.util.UUID;

import com.memorydrawer.card.query.dto.DrawerListResponse.DrawerItem;
import com.memorydrawer.card.query.dto.YearCardListResponse.CardItem;

public interface CardQuerySource {

	List<DrawerItem> findDrawers(UUID ownerId);

	List<CardItem> findCards(UUID ownerId, int year);

	CardLookupResult lookupCard(UUID ownerId, UUID cardId);
}
