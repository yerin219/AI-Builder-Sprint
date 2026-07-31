package com.memorydrawer.card.image;

import java.util.List;
import java.util.UUID;

import com.memorydrawer.memorydraft.image.ValidatedImage;

public interface BackPhotoStorage {

	List<String> store(UUID ownerId, UUID cardId, List<ValidatedImage> images);

	void deleteAll(List<String> keys);
}
