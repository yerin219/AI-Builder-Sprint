package com.memorydrawer.card.query.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.memorydrawer.card.DocumentType;

public record YearCardListResponse(int year, List<CardItem> cards) {

	public YearCardListResponse {
		cards = List.copyOf(cards);
	}

	public record CardItem(
		UUID cardId,
		DocumentType documentType,
		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate memoryDate,
		CardFront front
	) {
	}

	public sealed interface CardFront permits ReceiptFront, TicketFront, LetterFront {
	}

	public record ReceiptFront(String storeName, String frontImageUrl) implements CardFront {
	}

	public record TicketFront(
		String eventName,
		String venue,
		String seat,
		String frontImageUrl
	) implements CardFront {
	}

	public record LetterFront(
		String ocrText,
		String frontImageMode,
		String frontImageUrl
	) implements CardFront {
	}
}
