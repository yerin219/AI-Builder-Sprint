package com.memorydrawer.memorydraft.cleanup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.image.LetterFrontImageStorage;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

class ExpiredMemoryDraftCleanerTest {

	@Test
	void removesDerivedAndOriginalImagesBeforeDeletingExpiredUnsavedDraft() {
		MemoryDraftRepository repository = mock(MemoryDraftRepository.class);
		OriginalImageStorage originalStorage = mock(OriginalImageStorage.class);
		LetterFrontImageStorage letterStorage = mock(LetterFrontImageStorage.class);
		ExpiredMemoryDraftCleaner cleaner = new ExpiredMemoryDraftCleaner(
			repository,
			originalStorage,
			letterStorage
		);
		MemoryDraft draft = draft();
		when(repository.findAllByExpiresAtBeforeAndDraftStatusNot(
			any(Instant.class),
			eq(DraftStatus.SAVED)
		)).thenReturn(List.of(draft));

		cleaner.deleteExpiredDrafts();

		InOrder order = inOrder(letterStorage, originalStorage, repository);
		order.verify(letterStorage).delete(draft.getOwnerId(), draft.getId());
		order.verify(originalStorage).delete(draft.getOriginalImageKey());
		order.verify(repository).delete(draft);
		verify(repository).findAllByExpiresAtBeforeAndDraftStatusNot(
			any(Instant.class),
			eq(DraftStatus.SAVED)
		);
	}

	private MemoryDraft draft() {
		return MemoryDraft.analyzed(
			UUID.fromString("00000000-0000-0000-0000-000000000002"),
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"drafts/owner/draft/original.jpg",
			"image/jpeg",
			"parsed content",
			DocumentType.LETTER,
			Instant.parse("2026-07-01T00:00:00Z"),
			Instant.parse("2026-07-08T00:00:00Z")
		);
	}
}
