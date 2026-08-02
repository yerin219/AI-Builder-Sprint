package com.memorydrawer.memorydraft.image;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.memorydrawer.memorydraft.domain.MemoryDraft;

@Service
public class LetterFrontImageService {

	private final OriginalImageStorage originalImageStorage;
	private final LetterBackgroundRemover backgroundRemover;
	private final LetterFrontImageStorage letterFrontImageStorage;

	public LetterFrontImageService(
		OriginalImageStorage originalImageStorage,
		LetterBackgroundRemover backgroundRemover,
		LetterFrontImageStorage letterFrontImageStorage
	) {
		this.originalImageStorage = originalImageStorage;
		this.backgroundRemover = backgroundRemover;
		this.letterFrontImageStorage = letterFrontImageStorage;
	}

	public boolean prepareBackgroundRemoved(MemoryDraft draft) {
		try {
			byte[] original = originalImageStorage.load(draft.getOriginalImageKey());
			Optional<byte[]> removed = backgroundRemover.remove(original);
			if (removed.isPresent()) {
				letterFrontImageStorage.store(
					draft.getOwnerId(),
					draft.getId(),
					removed.orElseThrow()
				);
				return true;
			}
		} catch (RuntimeException ignored) {
			// OCR confirmation remains usable when optional image processing fails.
		}

		deleteStaleResult(draft);
		return false;
	}

	private void deleteStaleResult(MemoryDraft draft) {
		try {
			letterFrontImageStorage.delete(draft.getOwnerId(), draft.getId());
		} catch (RuntimeException ignored) {
			// TEXT_ONLY mode never reads a stale background-removed result.
		}
	}
}
