package com.memorydrawer.card.image;

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.FrontImageMode;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.query.CardAccessDeniedException;
import com.memorydrawer.card.query.CardNotFoundException;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.image.LetterFrontImageStorage;

@Service
public class CardImageService {

	private final MemoryCardRepository memoryCardRepository;
	private final LetterFrontImageStorage letterFrontImageStorage;
	private final BackPhotoStorage backPhotoStorage;
	private final ObjectMapper objectMapper;

	public CardImageService(
		MemoryCardRepository memoryCardRepository,
		LetterFrontImageStorage letterFrontImageStorage,
		BackPhotoStorage backPhotoStorage,
		ObjectMapper objectMapper
	) {
		this.memoryCardRepository = memoryCardRepository;
		this.letterFrontImageStorage = letterFrontImageStorage;
		this.backPhotoStorage = backPhotoStorage;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public CardImageResource front(UUID ownerId, UUID cardId) {
		MemoryCard card = ownedCard(ownerId, cardId);
		if (card.getDocumentType() != DocumentType.LETTER) {
			throw new CardNotFoundException();
		}
		if (frontImageMode(card) != FrontImageMode.BACKGROUND_REMOVED) {
			throw new CardNotFoundException();
		}
		return backgroundRemovedFront(card);
	}

	@Transactional(readOnly = true)
	public CardImageResource back(UUID ownerId, UUID cardId, int index) {
		MemoryCard card = ownedCard(ownerId, cardId);
		List<String> keys = backPhotoKeys(card);
		if (index < 1 || index > keys.size()) {
			throw new CardNotFoundException();
		}
		return load(keys.get(index - 1), backPhotoStorage::load);
	}

	private MemoryCard ownedCard(UUID ownerId, UUID cardId) {
		MemoryCard card = memoryCardRepository.findById(cardId)
			.orElseThrow(CardNotFoundException::new);
		if (!card.getOwnerId().equals(ownerId)) {
			throw new CardAccessDeniedException();
		}
		return card;
	}

	private List<String> backPhotoKeys(MemoryCard card) {
		try {
			JsonNode node = objectMapper.readTree(card.getBackPhotoKeys());
			if (!node.isArray()) {
				throw new ApiException(ErrorCode.CARD_003);
			}
			List<String> keys = new ArrayList<>();
			for (JsonNode item : node) {
				if (!item.isTextual() || item.textValue().isBlank()) {
					throw new ApiException(ErrorCode.CARD_003);
				}
				keys.add(item.textValue());
			}
			return List.copyOf(keys);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.CARD_003, exception);
		}
	}

	private FrontImageMode frontImageMode(MemoryCard card) {
		try {
			JsonNode front = objectMapper.readTree(card.getFrontData());
			if (front == null || !front.isObject()) {
				throw new ApiException(ErrorCode.CARD_003);
			}
			JsonNode value = front.get("frontImageMode");
			if (value == null || !value.isTextual()) {
				throw new ApiException(ErrorCode.CARD_003);
			}
			String storedMode = value.textValue();
			return "ORIGINAL".equals(storedMode)
				? FrontImageMode.TEXT_ONLY
				: FrontImageMode.valueOf(storedMode);
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.CARD_003, exception);
		}
	}

	private CardImageResource backgroundRemovedFront(MemoryCard card) {
		try {
			byte[] bytes = letterFrontImageStorage.load(card.getOwnerId(), card.getDraftId());
			if (!isPng(bytes)) {
				throw new ApiException(ErrorCode.CARD_003);
			}
			return new CardImageResource(bytes, MediaType.IMAGE_PNG);
		} catch (IllegalStateException exception) {
			if (exception.getCause() instanceof NoSuchFileException) {
				throw new CardNotFoundException();
			}
			throw new ApiException(ErrorCode.CARD_003, exception);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.CARD_003, exception);
		}
	}

	private boolean isPng(byte[] bytes) {
		byte[] signature = new byte[] {
			(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
		};
		if (bytes == null || bytes.length < signature.length) {
			return false;
		}
		for (int index = 0; index < signature.length; index++) {
			if (bytes[index] != signature[index]) {
				return false;
			}
		}
		return true;
	}

	private CardImageResource load(String key, ImageLoader loader) {
		try {
			return new CardImageResource(loader.load(key), mediaType(key));
		} catch (IllegalStateException exception) {
			if (exception.getCause() instanceof NoSuchFileException) {
				throw new CardNotFoundException();
			}
			throw new ApiException(ErrorCode.CARD_003, exception);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.CARD_003, exception);
		}
	}

	private MediaType mediaType(String key) {
		String normalized = key.toLowerCase(Locale.ROOT);
		if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
			return MediaType.IMAGE_JPEG;
		}
		if (normalized.endsWith(".png")) {
			return MediaType.IMAGE_PNG;
		}
		if (normalized.endsWith(".webp")) {
			return MediaType.parseMediaType("image/webp");
		}
		throw new IllegalArgumentException("지원하지 않는 카드 이미지 확장자입니다.");
	}

	@FunctionalInterface
	private interface ImageLoader {
		byte[] load(String key);
	}
}
