package com.memorydrawer.card.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.image.BackPhotoStorage;
import com.memorydrawer.card.query.CardAccessDeniedException;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;

class CardDeleteServiceTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final String ORIGINAL_KEY = "drafts/owner/draft/original.jpg";
	private static final List<String> BACK_KEYS = List.of("cards/owner/card/back/1.jpg");

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void deletesOwnedCardThenCleansImagesAfterCommit() {
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		OriginalImageStorage originalStorage = mock(OriginalImageStorage.class);
		BackPhotoStorage backStorage = mock(BackPhotoStorage.class);
		MemoryCard card = card();
		when(repository.findById(CARD_ID)).thenReturn(Optional.of(card));
		CardDeleteService service = new CardDeleteService(
			repository,
			originalStorage,
			backStorage,
			new ObjectMapper()
		);
		TransactionSynchronizationManager.initSynchronization();

		var response = service.delete(OWNER_ID, CARD_ID);

		assertThat(response.cardId()).isEqualTo(CARD_ID);
		verify(repository).delete(card);
		verify(repository).flush();
		verifyNoInteractions(originalStorage, backStorage);

		TransactionSynchronizationManager.getSynchronizations()
			.forEach(synchronization -> synchronization.afterCommit());

		verify(backStorage).deleteAll(BACK_KEYS);
		verify(originalStorage).delete(ORIGINAL_KEY);
	}

	@Test
	void rejectsDeletingAnotherUsersCard() {
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		OriginalImageStorage originalStorage = mock(OriginalImageStorage.class);
		BackPhotoStorage backStorage = mock(BackPhotoStorage.class);
		MemoryCard card = card();
		when(repository.findById(CARD_ID)).thenReturn(Optional.of(card));
		CardDeleteService service = new CardDeleteService(
			repository,
			originalStorage,
			backStorage,
			new ObjectMapper()
		);

		assertThatThrownBy(() -> service.delete(OTHER_OWNER_ID, CARD_ID))
			.isInstanceOf(CardAccessDeniedException.class);

		verify(repository, never()).delete(card);
		verifyNoInteractions(originalStorage, backStorage);
	}

	private MemoryCard card() {
		return MemoryCard.create(
			CARD_ID,
			OWNER_ID,
			UUID.fromString("00000000-0000-0000-0000-000000000004"),
			DocumentType.RECEIPT,
			LocalDate.of(2026, 1, 1),
			"{\"storeName\":\"가게\"}",
			"{\"companions\":[],\"weather\":\"맑음\",\"mood\":\"행복\"}",
			ORIGINAL_KEY,
			"[\"cards/owner/card/back/1.jpg\"]",
			Instant.parse("2026-07-31T00:00:00Z")
		);
	}
}
