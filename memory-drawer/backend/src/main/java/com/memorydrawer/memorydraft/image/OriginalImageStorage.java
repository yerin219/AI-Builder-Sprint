package com.memorydrawer.memorydraft.image;

import java.util.UUID;

public interface OriginalImageStorage {

	StoredImage store(UUID ownerId, UUID draftId, ValidatedImage image);

	byte[] load(String key);

	void delete(String key);
}
