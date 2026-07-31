package com.memorydrawer.card.query.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.FrontImageMode;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.ticket.recall.TicketSubtype;

public record CardDetailResponse(
	UUID cardId,
	DocumentType documentType,
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate memoryDate,
	CardFront front,
	CardBack back
) {

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
		FrontImageMode frontImageMode,
		String frontImageUrl
	) implements CardFront {
	}

	public sealed interface CardBack permits DiaryBack, DirectTicketBack, AiRecallTicketBack {
	}

	public record DiaryBack(
		List<String> companions,
		String weather,
		String mood,
		String diaryText,
		List<String> backPhotoUrls
	) implements CardBack {
		public DiaryBack {
			companions = List.copyOf(companions);
			backPhotoUrls = List.copyOf(backPhotoUrls);
		}
	}

	public record DirectTicketBack(
		List<String> companions,
		String weather,
		String mood,
		WritingMode writingMode,
		String title,
		String memoryText
	) implements CardBack {
		public DirectTicketBack {
			companions = List.copyOf(companions);
			if (writingMode != WritingMode.DIRECT) {
				throw new IllegalArgumentException("직접 기록 상세의 writingMode는 DIRECT여야 합니다.");
			}
		}
	}

	public record AiRecallTicketBack(
		List<String> companions,
		String weather,
		String mood,
		WritingMode writingMode,
		TicketSubtype ticketSubtype,
		String title,
		List<TicketAnswer> answers
	) implements CardBack {
		public AiRecallTicketBack {
			companions = List.copyOf(companions);
			answers = List.copyOf(answers);
			if (writingMode != WritingMode.AI_RECALL) {
				throw new IllegalArgumentException("AI 회상 상세의 writingMode는 AI_RECALL이어야 합니다.");
			}
		}
	}

	public record TicketAnswer(
		String questionId,
		String question,
		String answer
	) {
	}
}
