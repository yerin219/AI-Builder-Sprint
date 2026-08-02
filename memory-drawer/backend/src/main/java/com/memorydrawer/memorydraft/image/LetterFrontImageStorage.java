package com.memorydrawer.memorydraft.image;

import java.util.UUID;

public interface LetterFrontImageStorage {

	void store(UUID ownerId, UUID draftId, byte[] pngBytes);

	byte[] load(UUID ownerId, UUID draftId);

	void delete(UUID ownerId, UUID draftId);
}
