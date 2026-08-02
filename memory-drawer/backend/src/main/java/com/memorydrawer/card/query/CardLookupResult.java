package com.memorydrawer.card.query;

import com.memorydrawer.card.query.dto.CardDetailResponse;

public sealed interface CardLookupResult permits CardLookupResult.Found, CardLookupResult.Forbidden,
	CardLookupResult.NotFound {

	record Found(CardDetailResponse card) implements CardLookupResult {
	}

	record Forbidden() implements CardLookupResult {
	}

	record NotFound() implements CardLookupResult {
	}
}
