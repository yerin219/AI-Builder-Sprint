package com.memorydrawer.card.manage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.image.BackPhotoStorage;
import com.memorydrawer.card.manage.dto.CardDeleteResponse;
import com.memorydrawer.card.query.CardAccessDeniedException;
import com.memorydrawer.card.query.CardNotFoundException;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;

@Service
public class CardDeleteService {

	private static final Logger log = LoggerFactory.getLogger(CardDeleteService.class);

	private final MemoryCardRepository memoryCardRepository;
	private final OriginalImageStorage originalImageStorage;
	private final BackPhotoStorage backPhotoStorage;
	private final ObjectMapper objectMapper;

	public CardDeleteService(
		MemoryCardRepository memoryCardRepository,
		OriginalImageStorage originalImageStorage,
		BackPhotoStorage backPhotoStorage,
		ObjectMapper objectMapper
	) {
		this.memoryCardRepository = memoryCardRepository;
		this.originalImageStorage = originalImageStorage;
		this.backPhotoStorage = backPhotoStorage;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public CardDeleteResponse delete(UUID ownerId, UUID cardId) {
		MemoryCard card = ownedCard(ownerId, cardId);
		List<String> backPhotoKeys = readBackPhotoKeys(card.getBackPhotoKeys());
		String originalImageKey = card.getOriginalImageKey();

		memoryCardRepository.delete(card);
		memoryCardRepository.flush();
		deleteImagesAfterCommit(originalImageKey, backPhotoKeys);
		return new CardDeleteResponse(cardId);
	}

	private MemoryCard ownedCard(UUID ownerId, UUID cardId) {
		MemoryCard card = memoryCardRepository.findById(cardId)
			.orElseThrow(CardNotFoundException::new);
		if (!card.getOwnerId().equals(ownerId)) {
			throw new CardAccessDeniedException();
		}
		return card;
	}

	private List<String> readBackPhotoKeys(String value) {
		try {
			JsonNode node = objectMapper.readTree(value);
			if (node == null || !node.isArray()) {
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

	private void deleteImagesAfterCommit(
		String originalImageKey,
		List<String> backPhotoKeys
	) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deleteImages(originalImageKey, backPhotoKeys);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					deleteImages(originalImageKey, backPhotoKeys);
				}
			}
		);
	}

	private void deleteImages(String originalImageKey, List<String> backPhotoKeys) {
		try {
			backPhotoStorage.deleteAll(backPhotoKeys);
		} catch (RuntimeException exception) {
			log.warn("카드 삭제 후 뒷면 이미지 정리에 실패했습니다.", exception);
		}
		try {
			originalImageStorage.delete(originalImageKey);
		} catch (RuntimeException exception) {
			log.warn("카드 삭제 후 원본 이미지 정리에 실패했습니다.", exception);
		}
	}
}
