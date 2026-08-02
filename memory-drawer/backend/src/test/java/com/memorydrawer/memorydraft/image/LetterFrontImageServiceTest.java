package com.memorydrawer.memorydraft.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.MemoryDraft;

class LetterFrontImageServiceTest {

	private OriginalImageStorage originalImageStorage;
	private LetterBackgroundRemover backgroundRemover;
	private LetterFrontImageStorage letterFrontImageStorage;
	private LetterFrontImageService service;

	@BeforeEach
	void setUp() {
		originalImageStorage = mock(OriginalImageStorage.class);
		backgroundRemover = mock(LetterBackgroundRemover.class);
		letterFrontImageStorage = mock(LetterFrontImageStorage.class);
		service = new LetterFrontImageService(
			originalImageStorage,
			backgroundRemover,
			letterFrontImageStorage
		);
	}

	@Test
	void storesSuccessfulBackgroundRemoval() {
		MemoryDraft draft = draft();
		byte[] original = {1, 2, 3};
		byte[] removed = {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
		when(originalImageStorage.load(draft.getOriginalImageKey())).thenReturn(original);
		when(backgroundRemover.remove(original)).thenReturn(Optional.of(removed));

		assertThat(service.prepareBackgroundRemoved(draft)).isTrue();
		verify(letterFrontImageStorage).store(draft.getOwnerId(), draft.getId(), removed);
	}

	@Test
	void deletesStaleResultAndFallsBackWhenQualityGateRejectsImage() {
		MemoryDraft draft = draft();
		byte[] original = {1, 2, 3};
		when(originalImageStorage.load(draft.getOriginalImageKey())).thenReturn(original);
		when(backgroundRemover.remove(original)).thenReturn(Optional.empty());

		assertThat(service.prepareBackgroundRemoved(draft)).isFalse();
		verify(letterFrontImageStorage).delete(draft.getOwnerId(), draft.getId());
	}

	@Test
	void deletesStaleResultAndFallsBackWhenImageCannotBeLoaded() {
		MemoryDraft draft = draft();
		when(originalImageStorage.load(draft.getOriginalImageKey()))
			.thenThrow(new IllegalStateException("missing original"));

		assertThat(service.prepareBackgroundRemoved(draft)).isFalse();
		verify(letterFrontImageStorage).delete(draft.getOwnerId(), draft.getId());
	}

	@Test
	void deletesStaleResultAndFallsBackWhenProcessedImageCannotBeStored() {
		MemoryDraft draft = draft();
		byte[] original = {1, 2, 3};
		byte[] removed = {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
		when(originalImageStorage.load(draft.getOriginalImageKey())).thenReturn(original);
		when(backgroundRemover.remove(original)).thenReturn(Optional.of(removed));
		doThrow(new IllegalStateException("storage unavailable"))
			.when(letterFrontImageStorage)
			.store(draft.getOwnerId(), draft.getId(), removed);

		assertThat(service.prepareBackgroundRemoved(draft)).isFalse();
		verify(letterFrontImageStorage).delete(draft.getOwnerId(), draft.getId());
	}

	private MemoryDraft draft() {
		return MemoryDraft.analyzed(
			UUID.fromString("00000000-0000-0000-0000-000000000002"),
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"drafts/owner/draft/original.jpg",
			"image/jpeg",
			"parsed content",
			DocumentType.LETTER,
			Instant.parse("2026-08-01T00:00:00Z"),
			Instant.parse("2026-08-08T00:00:00Z")
		);
	}
}
