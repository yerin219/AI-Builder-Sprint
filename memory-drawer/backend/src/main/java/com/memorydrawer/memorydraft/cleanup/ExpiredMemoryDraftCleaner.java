package com.memorydrawer.memorydraft.cleanup;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@Component
public class ExpiredMemoryDraftCleaner {

	private final MemoryDraftRepository memoryDraftRepository;
	private final OriginalImageStorage originalImageStorage;

	public ExpiredMemoryDraftCleaner(
		MemoryDraftRepository memoryDraftRepository,
		OriginalImageStorage originalImageStorage
	) {
		this.memoryDraftRepository = memoryDraftRepository;
		this.originalImageStorage = originalImageStorage;
	}

	@Transactional
	@Scheduled(
		initialDelayString = "${memory-drawer.draft.cleanup-interval-ms}",
		fixedDelayString = "${memory-drawer.draft.cleanup-interval-ms}"
	)
	public void deleteExpiredDrafts() {
		List<MemoryDraft> expiredDrafts = memoryDraftRepository
			.findAllByExpiresAtBeforeAndDraftStatusNot(Instant.now(), DraftStatus.SAVED);
		for (MemoryDraft draft : expiredDrafts) {
			originalImageStorage.delete(draft.getOriginalImageKey());
			memoryDraftRepository.delete(draft);
		}
	}
}
