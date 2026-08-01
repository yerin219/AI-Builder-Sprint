package com.memorydrawer.card.manage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.card.create.CardCreationValidator;
import com.memorydrawer.card.create.ValidatedCardBack;
import com.memorydrawer.card.create.dto.CardCreateRequest;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.manage.dto.CardUpdateRequest;
import com.memorydrawer.card.manage.dto.CardUpdateResponse;
import com.memorydrawer.card.query.CardAccessDeniedException;
import com.memorydrawer.card.query.CardNotFoundException;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.api.ConfirmedFront;
import com.memorydrawer.memorydraft.api.FrontImageMode;
import com.memorydrawer.memorydraft.api.LetterConfirmedFront;
import com.memorydrawer.memorydraft.api.ReceiptConfirmedFront;
import com.memorydrawer.memorydraft.api.TicketConfirmedFront;
import com.memorydrawer.receipt.PurchaseItem;
import com.memorydrawer.ticket.recall.TicketSubtype;

@Service
public class CardUpdateService {

	private static final Set<String> RECEIPT_FIELDS = Set.of("storeName", "purchaseItems");
	private static final Set<String> PURCHASE_ITEM_FIELDS = Set.of("name", "quantity");
	private static final Set<String> TICKET_FIELDS = Set.of("eventName", "venue", "seat");
	private static final Set<String> LETTER_FIELDS = Set.of("ocrText");

	private final MemoryCardRepository memoryCardRepository;
	private final ObjectMapper objectMapper;

	public CardUpdateService(
		MemoryCardRepository memoryCardRepository,
		ObjectMapper objectMapper
	) {
		this.memoryCardRepository = memoryCardRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public CardUpdateResponse update(
		UUID ownerId,
		UUID cardId,
		CardUpdateRequest request
	) {
		MemoryCard card = ownedCard(ownerId, cardId);
		if (request == null || request.memoryDate() == null
			|| request.front() == null || request.back() == null) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}

		try {
			JsonNode existingFront = readJson(card.getFrontData());
			JsonNode existingBack = readJson(card.getBackData());
			ConfirmedFront updatedFront = validateFront(
				card.getDocumentType(),
				request.front(),
				existingFront
			);
			int backPhotoCount = readStringList(card.getBackPhotoKeys()).size();
			TicketSubtype ticketSubtype = validateWritingModeAndSubtype(
				card.getDocumentType(),
				request,
				existingBack
			);
			ValidatedCardBack updatedBack = CardCreationValidator.validate(
				card.getDocumentType(),
				ticketSubtype,
				new CardCreateRequest(card.getDraftId(), request.back()),
				backPhotoCount
			);

			card.update(
				request.memoryDate(),
				objectMapper.writeValueAsString(updatedFront),
				objectMapper.writeValueAsString(updatedBack)
			);
			memoryCardRepository.saveAndFlush(card);
			return CardUpdateResponse.updated(
				card.getId(),
				card.getDocumentType(),
				card.getMemoryDate()
			);
		} catch (ApiException exception) {
			throw exception;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new ApiException(ErrorCode.VALIDATION_001, exception);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.CARD_003, exception);
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.CARD_003, exception);
		}
	}

	private MemoryCard ownedCard(UUID ownerId, UUID cardId) {
		MemoryCard card = memoryCardRepository.findById(cardId)
			.orElseThrow(CardNotFoundException::new);
		if (!card.getOwnerId().equals(ownerId)) {
			throw new CardAccessDeniedException();
		}
		return card;
	}

	private ConfirmedFront validateFront(
		DocumentType documentType,
		JsonNode requestedFront,
		JsonNode existingFront
	) {
		if (!requestedFront.isObject()) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}
		return switch (documentType) {
			case RECEIPT -> {
				validateAllowedFields(requestedFront, RECEIPT_FIELDS);
				yield new ReceiptConfirmedFront(
					requiredText(requestedFront, "storeName"),
					purchaseItemsOrEmpty(
						requestedFront.has("purchaseItems") ? requestedFront : existingFront
					)
				);
			}
			case TICKET -> {
				validateAllowedFields(requestedFront, TICKET_FIELDS);
				yield new TicketConfirmedFront(
					requiredText(requestedFront, "eventName"),
					requiredText(requestedFront, "venue"),
					optionalText(requestedFront, "seat")
				);
			}
			case LETTER -> {
				validateAllowedFields(requestedFront, LETTER_FIELDS);
				yield new LetterConfirmedFront(
					requiredText(requestedFront, "ocrText"),
					FrontImageMode.valueOf(requiredText(existingFront, "frontImageMode"))
				);
			}
		};
	}

	private List<PurchaseItem> purchaseItemsOrEmpty(JsonNode front) {
		JsonNode value = front.get("purchaseItems");
		if (value == null) {
			return List.of();
		}
		if (!value.isArray()) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}

		List<PurchaseItem> items = new ArrayList<>();
		for (JsonNode item : value) {
			if (!item.isObject()) {
				throw new ApiException(ErrorCode.VALIDATION_001);
			}
			validateAllowedFields(item, PURCHASE_ITEM_FIELDS);
			String name = requiredText(item, "name");
			JsonNode quantity = item.get("quantity");
			if (quantity == null || !quantity.isIntegralNumber()
				|| !quantity.canConvertToInt() || quantity.intValue() < 1) {
				throw new ApiException(ErrorCode.VALIDATION_001);
			}
			items.add(new PurchaseItem(name, quantity.intValue()));
		}
		return List.copyOf(items);
	}

	private TicketSubtype validateWritingModeAndSubtype(
		DocumentType documentType,
		CardUpdateRequest request,
		JsonNode existingBack
	) {
		if (documentType != DocumentType.TICKET) {
			return null;
		}
		WritingMode existingMode = WritingMode.valueOf(requiredText(existingBack, "writingMode"));
		if (request.back().writingMode() != existingMode) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}
		if (existingMode == WritingMode.DIRECT) {
			return null;
		}
		return TicketSubtype.valueOf(requiredText(existingBack, "ticketSubtype"));
	}

	private JsonNode readJson(String value) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(value);
		if (node == null || !node.isObject()) {
			throw new ApiException(ErrorCode.CARD_003);
		}
		return node;
	}

	private List<String> readStringList(String value) throws JsonProcessingException {
		JsonNode node = objectMapper.readTree(value);
		if (node == null || !node.isArray()) {
			throw new ApiException(ErrorCode.CARD_003);
		}
		List<String> result = new ArrayList<>();
		for (JsonNode item : node) {
			if (!item.isTextual() || item.textValue().isBlank()) {
				throw new ApiException(ErrorCode.CARD_003);
			}
			result.add(item.textValue());
		}
		return List.copyOf(result);
	}

	private void validateAllowedFields(JsonNode front, Set<String> allowedFields) {
		Iterator<String> fieldNames = front.fieldNames();
		while (fieldNames.hasNext()) {
			if (!allowedFields.contains(fieldNames.next())) {
				throw new ApiException(ErrorCode.VALIDATION_001);
			}
		}
	}

	private String requiredText(JsonNode node, String fieldName) {
		String value = optionalText(node, fieldName);
		if (value == null) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}
		return value;
	}

	private String optionalText(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		if (value == null || value.isNull()) {
			return null;
		}
		if (!value.isTextual()) {
			throw new ApiException(ErrorCode.VALIDATION_001);
		}
		String normalized = value.asText().trim();
		return normalized.isBlank() ? null : normalized;
	}
}
