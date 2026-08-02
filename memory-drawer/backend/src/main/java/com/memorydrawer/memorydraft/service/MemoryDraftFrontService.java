package com.memorydrawer.memorydraft.service;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.api.ConfirmFrontRequest;
import com.memorydrawer.memorydraft.api.ConfirmFrontResponse;
import com.memorydrawer.memorydraft.api.ConfirmedFront;
import com.memorydrawer.memorydraft.api.ConfirmedFrontSnapshot;
import com.memorydrawer.memorydraft.api.FrontImageMode;
import com.memorydrawer.memorydraft.api.LetterConfirmedFront;
import com.memorydrawer.memorydraft.api.ReceiptConfirmedFront;
import com.memorydrawer.memorydraft.api.TicketConfirmedFront;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.image.LetterFrontImageService;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@Service
public class MemoryDraftFrontService {

	private static final String NEXT_ACTION = "WRITE_BACK";
	private static final Set<String> RECEIPT_FIELDS = Set.of("storeName");
	private static final Set<String> TICKET_FIELDS = Set.of(
		"eventName",
		"venue",
		"seat"
	);
	private static final Set<String> LETTER_FIELDS = Set.of("ocrText");

	private final MemoryDraftRepository memoryDraftRepository;
	private final LetterFrontImageService letterFrontImageService;
	private final ObjectMapper objectMapper;

	public MemoryDraftFrontService(
		MemoryDraftRepository memoryDraftRepository,
		LetterFrontImageService letterFrontImageService,
		ObjectMapper objectMapper
	) {
		this.memoryDraftRepository = memoryDraftRepository;
		this.letterFrontImageService = letterFrontImageService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public ConfirmFrontResponse confirm(
		UUID ownerId,
		UUID draftId,
		ConfirmFrontRequest request
	) {
		MemoryDraft draft = findOwnedDraft(ownerId, draftId);
		if ((draft.getDraftStatus() != DraftStatus.FRONT_PENDING
			&& draft.getDraftStatus() != DraftStatus.FRONT_CONFIRMED)
			|| draft.getDocumentType() == null) {
			throw new ApiException(ErrorCode.DRAFT_002);
		}
		if (request == null || request.memoryDate() == null) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}

		ConfirmedFront confirmedFront = validateFront(
			draft,
			request.front()
		);
		ConfirmedFrontSnapshot snapshot = new ConfirmedFrontSnapshot(
			request.memoryDate(),
			objectMapper.valueToTree(confirmedFront)
		);
		draft.confirmFront(serialize(snapshot));
		memoryDraftRepository.saveAndFlush(draft);

		return new ConfirmFrontResponse(
			draft.getId(),
			draft.getDocumentType(),
			snapshot.memoryDate(),
			confirmedFront,
			draft.getDraftStatus(),
			NEXT_ACTION
		);
	}

	private MemoryDraft findOwnedDraft(UUID ownerId, UUID draftId) {
		MemoryDraft draft = memoryDraftRepository.findById(draftId)
			.orElseThrow(() -> new ApiException(ErrorCode.DRAFT_001));
		if (!draft.getOwnerId().equals(ownerId)) {
			throw new ApiException(ErrorCode.DRAFT_004);
		}
		return draft;
	}

	private ConfirmedFront validateFront(
		MemoryDraft draft,
		JsonNode front
	) {
		if (front == null || !front.isObject()) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}

		return switch (draft.getDocumentType()) {
			case RECEIPT -> receiptFront(front);
			case TICKET -> ticketFront(front);
			case LETTER -> letterFront(draft, front);
		};
	}

	private ReceiptConfirmedFront receiptFront(JsonNode front) {
		validateAllowedFields(front, RECEIPT_FIELDS);
		return new ReceiptConfirmedFront(requiredText(front, "storeName"));
	}

	private TicketConfirmedFront ticketFront(JsonNode front) {
		validateAllowedFields(front, TICKET_FIELDS);
		return new TicketConfirmedFront(
			requiredText(front, "eventName"),
			requiredText(front, "venue"),
			optionalText(front, "seat")
		);
	}

	private LetterConfirmedFront letterFront(MemoryDraft draft, JsonNode front) {
		validateAllowedFields(front, LETTER_FIELDS);
		String ocrText = requiredText(front, "ocrText");
		FrontImageMode imageMode = letterFrontImageService.prepareBackgroundRemoved(draft)
			? FrontImageMode.BACKGROUND_REMOVED
			: FrontImageMode.TEXT_ONLY;
		return new LetterConfirmedFront(
			ocrText,
			imageMode
		);
	}

	private void validateAllowedFields(JsonNode front, Set<String> allowedFields) {
		Iterator<String> fieldNames = front.fieldNames();
		while (fieldNames.hasNext()) {
			if (!allowedFields.contains(fieldNames.next())) {
				throw new ApiException(ErrorCode.VALIDATION_001);
			}
		}
	}

	private String requiredText(JsonNode front, String fieldName) {
		JsonNode value = front.get(fieldName);
		if (value == null || !value.isTextual()) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}
		String normalized = value.asText().trim();
		if (normalized.isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}
		return normalized;
	}

	private String optionalText(JsonNode front, String fieldName) {
		JsonNode value = front.get(fieldName);
		if (value == null || value.isNull()) {
			return null;
		}
		if (!value.isTextual()) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}
		String normalized = value.asText().trim();
		return normalized.isBlank() ? null : normalized;
	}

	private String serialize(ConfirmedFrontSnapshot snapshot) {
		try {
			return objectMapper.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException(
				"확정된 카드 앞면을 내부 형식으로 저장할 수 없습니다.",
				exception
			);
		}
	}
}
