package com.memorydrawer.memorydraft.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalLetterFrontImageStorageTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID DRAFT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@TempDir
	Path directory;

	@Test
	void storesAndAtomicallyReplacesTheDraftResult() {
		LocalLetterFrontImageStorage storage = new LocalLetterFrontImageStorage(directory.toString());
		byte[] first = pngBytes((byte)1);
		byte[] second = pngBytes((byte)2);

		storage.store(OWNER_ID, DRAFT_ID, first);
		storage.store(OWNER_ID, DRAFT_ID, second);

		assertThat(storage.load(OWNER_ID, DRAFT_ID)).containsExactly(second);
	}

	@Test
	void deletesStoredResultAndEmptyDraftDirectories() {
		LocalLetterFrontImageStorage storage = new LocalLetterFrontImageStorage(directory.toString());
		storage.store(OWNER_ID, DRAFT_ID, pngBytes((byte)1));

		storage.delete(OWNER_ID, DRAFT_ID);

		assertThatThrownBy(() -> storage.load(OWNER_ID, DRAFT_ID))
			.isInstanceOfSatisfying(IllegalStateException.class, exception ->
				assertThat(exception.getCause()).isInstanceOf(NoSuchFileException.class)
			);
		assertThat(directory.resolve("drafts").resolve(OWNER_ID.toString()))
			.doesNotExist();
	}

	@Test
	void rejectsNonPngData() {
		LocalLetterFrontImageStorage storage = new LocalLetterFrontImageStorage(directory.toString());

		assertThatThrownBy(() -> storage.store(OWNER_ID, DRAFT_ID, new byte[] {1, 2, 3}))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private byte[] pngBytes(byte marker) {
		return new byte[] {
			(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, marker
		};
	}
}
