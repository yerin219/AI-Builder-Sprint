package com.memorydrawer.card.create;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.create.dto.CardCreateRequest;
import com.memorydrawer.card.create.dto.CardCreateResponse;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.image.BackPhotoStorage;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.api.ConfirmedFrontSnapshot;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.image.ImageFileValidator;
import com.memorydrawer.memorydraft.image.ValidatedImage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@Service
public class CardCreateService {

	private final MemoryDraftRepository memoryDraftRepository;
	private final MemoryCardRepository memoryCardRepository;
	private final ImageFileValidator imageFileValidator;
	private final BackPhotoStorage backPhotoStorage;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Autowired
	public CardCreateService(
		MemoryDraftRepository memoryDraftRepository,
		MemoryCardRepository memoryCardRepository,
		ImageFileValidator imageFileValidator,
		BackPhotoStorage backPhotoStorage,
		ObjectMapper objectMapper
	) {
		this(
			memoryDraftRepository,
			memoryCardRepository,
			imageFileValidator,
			backPhotoStorage,
			objectMapper,
			Clock.systemUTC()
		);
	}

	CardCreateService(
		MemoryDraftRepository memoryDraftRepository,
		MemoryCardRepository memoryCardRepository,
		ImageFileValidator imageFileValidator,
		BackPhotoStorage backPhotoStorage,
		ObjectMapper objectMapper,
		Clock clock
	) {
		this.memoryDraftRepository = memoryDraftRepository;
		this.memoryCardRepository = memoryCardRepository;
		this.imageFileValidator = imageFileValidator;
		this.backPhotoStorage = backPhotoStorage;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional
	public CardCreateResponse create(
		UUID ownerId,
		CardCreateRequest request,
		List<MultipartFile> backPhotos
	) {
		MemoryDraft draft = findOwnedDraft(ownerId, request.draftId());
		if (draft.getDraftStatus() != DraftStatus.FRONT_CONFIRMED
			|| memoryCardRepository.existsByDraftId(draft.getId())) {
			throw new ApiException(ErrorCode.DRAFT_003);
		}

		List<ValidatedImage> validatedImages = validateImages(backPhotos);
		DocumentType documentType = DocumentType.valueOf(draft.getDocumentType().name());
		ValidatedCardBack validatedBack;
		try {
			validatedBack = CardCreationValidator.validate(
				documentType,
				draft.getTicketSubtype(),
				request,
				validatedImages.size()
			);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new ApiException(ErrorCode.VALIDATION_001, exception);
		}

		ConfirmedFrontSnapshot front = deserializeFront(draft.getFrontCandidate());
		UUID cardId = UUID.randomUUID();
		List<String> storedPhotoKeys = List.of();
		try {
			storedPhotoKeys = backPhotoStorage.store(ownerId, cardId, validatedImages);
			MemoryCard card = MemoryCard.create(
				cardId,
				ownerId,
				draft.getId(),
				documentType,
				front.memoryDate(),
				objectMapper.writeValueAsString(front.front()),
				objectMapper.writeValueAsString(validatedBack),
				draft.getOriginalImageKey(),
				objectMapper.writeValueAsString(storedPhotoKeys),
				Instant.now(clock)
			);
			memoryCardRepository.saveAndFlush(card);
			draft.markSaved();
			memoryDraftRepository.saveAndFlush(draft);
			return CardCreateResponse.saved(cardId, documentType, front.memoryDate());
		} catch (DataIntegrityViolationException exception) {
			backPhotoStorage.deleteAll(storedPhotoKeys);
			throw new ApiException(ErrorCode.DRAFT_003, exception);
		} catch (JsonProcessingException exception) {
			backPhotoStorage.deleteAll(storedPhotoKeys);
			throw new ApiException(ErrorCode.CARD_003, exception);
		} catch (ApiException exception) {
			backPhotoStorage.deleteAll(storedPhotoKeys);
			throw exception;
		} catch (RuntimeException exception) {
			backPhotoStorage.deleteAll(storedPhotoKeys);
			throw new ApiException(ErrorCode.CARD_003, exception);
		}
	}

	private MemoryDraft findOwnedDraft(UUID ownerId, UUID draftId) {
		MemoryDraft draft = memoryDraftRepository.findById(draftId)
			.orElseThrow(() -> new ApiException(ErrorCode.DRAFT_001));
		if (!draft.getOwnerId().equals(ownerId)) {
			throw new ApiException(ErrorCode.DRAFT_004);
		}
		return draft;
	}

	private ConfirmedFrontSnapshot deserializeFront(String value) {
		try {
			ConfirmedFrontSnapshot snapshot = objectMapper.readValue(
				value,
				ConfirmedFrontSnapshot.class
			);
			if (snapshot.memoryDate() == null || snapshot.front() == null
				|| !snapshot.front().isObject()) {
				throw new ApiException(ErrorCode.DRAFT_003);
			}
			return snapshot;
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.DRAFT_003, exception);
		}
	}

	private List<ValidatedImage> validateImages(List<MultipartFile> backPhotos) {
		if (backPhotos == null || backPhotos.isEmpty()) {
			return List.of();
		}
		List<ValidatedImage> images = new ArrayList<>();
		for (MultipartFile backPhoto : backPhotos) {
			images.add(imageFileValidator.validate(backPhoto));
		}
		return List.copyOf(images);
	}
}
