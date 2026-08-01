package com.memorydrawer.card.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.FrontImageMode;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.query.dto.CardDetailResponse;
import com.memorydrawer.card.query.dto.DrawerListResponse.DrawerItem;
import com.memorydrawer.card.query.dto.YearCardListResponse;
import com.memorydrawer.card.query.dto.YearCardListResponse.CardItem;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.receipt.PurchaseItem;
import com.memorydrawer.ticket.recall.TicketSubtype;

@Component
public class JpaCardQuerySource implements CardQuerySource {

	private final MemoryCardRepository memoryCardRepository;
	private final ObjectMapper objectMapper;

	public JpaCardQuerySource(
		MemoryCardRepository memoryCardRepository,
		ObjectMapper objectMapper
	) {
		this.memoryCardRepository = memoryCardRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<DrawerItem> findDrawers(UUID ownerId) {
		Map<Integer, Long> counts = memoryCardRepository.findAllByOwnerId(ownerId).stream()
			.collect(Collectors.groupingBy(
				card -> card.getMemoryDate().getYear(),
				Collectors.counting()
			));
		return counts.entrySet().stream()
			.map(entry -> new DrawerItem(entry.getKey(), entry.getValue()))
			.toList();
	}

	@Override
	public List<CardItem> findCards(UUID ownerId, int year) {
		return memoryCardRepository.findAllByOwnerId(ownerId).stream()
			.filter(card -> card.getMemoryDate().getYear() == year)
			.map(this::toCardItem)
			.toList();
	}

	@Override
	public CardLookupResult lookupCard(UUID ownerId, UUID cardId) {
		return memoryCardRepository.findById(cardId)
			.<CardLookupResult>map(card -> card.getOwnerId().equals(ownerId)
				? new CardLookupResult.Found(toDetail(card))
				: new CardLookupResult.Forbidden())
			.orElseGet(CardLookupResult.NotFound::new);
	}

	private CardItem toCardItem(MemoryCard card) {
		JsonNode front = readJson(card.getFrontData());
		return new CardItem(
			card.getId(),
			card.getDocumentType(),
			card.getMemoryDate(),
			switch (card.getDocumentType()) {
				case RECEIPT -> new YearCardListResponse.ReceiptFront(
					requiredText(front, "storeName"),
					purchaseItems(front)
				);
				case TICKET -> new YearCardListResponse.TicketFront(
					requiredText(front, "eventName"),
					requiredText(front, "venue"),
					optionalText(front, "seat")
				);
				case LETTER -> new YearCardListResponse.LetterFront(
					requiredText(front, "ocrText"),
					requiredText(front, "frontImageMode"),
					frontImageUrl(card)
				);
			}
		);
	}

	private CardDetailResponse toDetail(MemoryCard card) {
		JsonNode front = readJson(card.getFrontData());
		JsonNode back = readJson(card.getBackData());
		return new CardDetailResponse(
			card.getId(),
			card.getDocumentType(),
			card.getMemoryDate(),
			detailFront(card, front),
			detailBack(card, back)
		);
	}

	private CardDetailResponse.CardFront detailFront(MemoryCard card, JsonNode front) {
		return switch (card.getDocumentType()) {
			case RECEIPT -> new CardDetailResponse.ReceiptFront(
				requiredText(front, "storeName"),
				purchaseItems(front)
			);
			case TICKET -> new CardDetailResponse.TicketFront(
				requiredText(front, "eventName"),
				requiredText(front, "venue"),
				optionalText(front, "seat")
			);
			case LETTER -> new CardDetailResponse.LetterFront(
				requiredText(front, "ocrText"),
				FrontImageMode.valueOf(requiredText(front, "frontImageMode")),
				frontImageUrl(card)
			);
		};
	}

	private CardDetailResponse.CardBack detailBack(MemoryCard card, JsonNode back) {
		List<String> companions = stringList(back.path("companions"));
		String weather = requiredText(back, "weather");
		String mood = requiredText(back, "mood");
		if (card.getDocumentType() != com.memorydrawer.card.DocumentType.TICKET) {
			return new CardDetailResponse.DiaryBack(
				companions,
				weather,
				mood,
				optionalText(back, "diaryText"),
				backPhotoUrls(card)
			);
		}

		WritingMode writingMode = WritingMode.valueOf(requiredText(back, "writingMode"));
		if (writingMode == WritingMode.DIRECT) {
			return new CardDetailResponse.DirectTicketBack(
				companions,
				weather,
				mood,
				writingMode,
				requiredText(back, "title"),
				requiredText(back, "memoryText")
			);
		}

		List<CardDetailResponse.TicketAnswer> answers = new ArrayList<>();
		for (JsonNode answer : back.path("answers")) {
			answers.add(new CardDetailResponse.TicketAnswer(
				requiredText(answer, "questionId"),
				requiredText(answer, "question"),
				optionalText(answer, "answer")
			));
		}
		return new CardDetailResponse.AiRecallTicketBack(
			companions,
			weather,
			mood,
			writingMode,
			TicketSubtype.valueOf(requiredText(back, "ticketSubtype")),
			requiredText(back, "title"),
			answers
		);
	}

	private JsonNode readJson(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.CARD_003, exception);
		}
	}

	private List<String> stringList(JsonNode value) {
		if (!value.isArray()) {
			throw new ApiException(ErrorCode.CARD_003);
		}
		List<String> values = new ArrayList<>();
		value.forEach(item -> values.add(item.asText()));
		return List.copyOf(values);
	}

	private List<PurchaseItem> purchaseItems(JsonNode front) {
		JsonNode value = front.get("purchaseItems");
		if (value == null || value.isNull()) {
			return List.of();
		}
		if (!value.isArray()) {
			throw new ApiException(ErrorCode.CARD_003);
		}

		List<PurchaseItem> items = new ArrayList<>();
		for (JsonNode item : value) {
			if (!item.isObject()) {
				throw new ApiException(ErrorCode.CARD_003);
			}
			String name = requiredText(item, "name");
			JsonNode quantity = item.get("quantity");
			if (quantity == null || !quantity.isIntegralNumber()
				|| !quantity.canConvertToInt() || quantity.intValue() < 1) {
				throw new ApiException(ErrorCode.CARD_003);
			}
			items.add(new PurchaseItem(name, quantity.intValue()));
		}
		return List.copyOf(items);
	}

	private String requiredText(JsonNode node, String fieldName) {
		String value = optionalText(node, fieldName);
		if (value == null) {
			throw new ApiException(ErrorCode.CARD_003);
		}
		return value;
	}

	private String optionalText(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		if (value == null || value.isNull()) {
			return null;
		}
		if (!value.isTextual()) {
			throw new ApiException(ErrorCode.CARD_003);
		}
		String normalized = value.asText().trim();
		return normalized.isBlank() ? null : normalized;
	}

	private String frontImageUrl(MemoryCard card) {
		return "/files/cards/%s/front".formatted(card.getId());
	}

	private List<String> backPhotoUrls(MemoryCard card) {
		JsonNode keys = readJson(card.getBackPhotoKeys());
		if (!keys.isArray()) {
			throw new ApiException(ErrorCode.CARD_003);
		}
		List<String> urls = new ArrayList<>();
		for (int index = 0; index < keys.size(); index++) {
			urls.add("/files/cards/%s/back/%d".formatted(card.getId(), index + 1));
		}
		return List.copyOf(urls);
	}
}
