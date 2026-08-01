package com.memorydrawer.card.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.query.dto.CardDetailResponse;
import com.memorydrawer.card.query.dto.YearCardListResponse;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.receipt.PurchaseItem;

class JpaCardQuerySourceTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void returnsConfirmedReceiptPurchaseItemsFromListAndDetail() {
		MemoryCard card = receiptCard(
			"""
				{"storeName":"서면카페","purchaseItems":[{"name":"아이스 아메리카노","quantity":2}]}
				"""
		);
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		when(repository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(card));
		when(repository.findById(CARD_ID)).thenReturn(java.util.Optional.of(card));
		JpaCardQuerySource source = new JpaCardQuerySource(
			repository,
			new ObjectMapper().findAndRegisterModules()
		);

		YearCardListResponse.ReceiptFront listFront = (YearCardListResponse.ReceiptFront)
			source.findCards(OWNER_ID, 2026).getFirst().front();
		int layoutSeed = source.findCards(OWNER_ID, 2026).getFirst().layoutSeed();
		CardLookupResult.Found found = (CardLookupResult.Found) source.lookupCard(OWNER_ID, CARD_ID);
		CardDetailResponse.ReceiptFront detailFront = (CardDetailResponse.ReceiptFront)
			found.card().front();

		assertThat(listFront.purchaseItems())
			.containsExactly(new PurchaseItem("아이스 아메리카노", 2));
		assertThat(detailFront.purchaseItems()).isEqualTo(listFront.purchaseItems());
		assertThat(layoutSeed).isEqualTo(CARD_ID.hashCode() & Integer.MAX_VALUE);
	}

	@Test
	void treatsLegacyReceiptWithoutPurchaseItemsAsEmptyList() {
		MemoryCard card = receiptCard("{\"storeName\":\"오래된 가게\"}");
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		when(repository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(card));
		when(repository.findById(CARD_ID)).thenReturn(java.util.Optional.of(card));
		JpaCardQuerySource source = new JpaCardQuerySource(
			repository,
			new ObjectMapper().findAndRegisterModules()
		);

		YearCardListResponse.ReceiptFront listFront = (YearCardListResponse.ReceiptFront)
			source.findCards(OWNER_ID, 2026).getFirst().front();
		CardLookupResult.Found found = (CardLookupResult.Found) source.lookupCard(OWNER_ID, CARD_ID);
		CardDetailResponse.ReceiptFront detailFront = (CardDetailResponse.ReceiptFront)
			found.card().front();

		assertThat(listFront.purchaseItems()).isEmpty();
		assertThat(detailFront.purchaseItems()).isEmpty();
	}

	private MemoryCard receiptCard(String frontData) {
		return MemoryCard.create(
			CARD_ID,
			OWNER_ID,
			UUID.fromString("00000000-0000-0000-0000-000000000003"),
			DocumentType.RECEIPT,
			LocalDate.of(2026, 7, 25),
			frontData,
			"{\"companions\":[],\"weather\":\"맑음\",\"mood\":\"행복\",\"diaryText\":\"기억\"}",
			"drafts/owner/draft/original.jpg",
			"[]",
			Instant.parse("2026-07-31T00:00:00Z")
		);
	}
}
