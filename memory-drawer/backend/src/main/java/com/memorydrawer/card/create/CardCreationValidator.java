package com.memorydrawer.card.create;

import java.util.List;
import java.util.Objects;

import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.card.create.dto.CardBackRequest;
import com.memorydrawer.card.create.dto.CardCreateRequest;
import com.memorydrawer.ticket.recall.TicketRecallAnswerValidator;
import com.memorydrawer.ticket.recall.TicketRecallAnswerValidator.AnsweredQuestion;
import com.memorydrawer.ticket.recall.TicketSubtype;

public final class CardCreationValidator {

	private CardCreationValidator() {
	}

	public static ValidatedCardBack validate(
		DocumentType documentType,
		TicketSubtype confirmedTicketSubtype,
		CardCreateRequest request,
		int backPhotoCount
	) {
		Objects.requireNonNull(documentType, "문서 유형이 필요합니다.");
		if (request == null || request.draftId() == null || request.back() == null) {
			throw new IllegalArgumentException("카드 저장 요청과 draftId, 뒷면 정보가 필요합니다.");
		}
		if (backPhotoCount < 0) {
			throw new IllegalArgumentException("뒷면 사진 개수는 음수일 수 없습니다.");
		}

		CardBackRequest back = request.back();
		validateCommonFields(back);

		return switch (documentType) {
			case RECEIPT, LETTER -> validateDiaryBack(back, backPhotoCount);
			case TICKET -> validateTicketBack(back, confirmedTicketSubtype, backPhotoCount);
		};
	}

	private static void validateCommonFields(CardBackRequest back) {
		if (back.companions() == null) {
			throw new IllegalArgumentException("동행인 목록이 필요합니다. 혼자라면 빈 목록을 보내야 합니다.");
		}
		if (back.companions().stream().anyMatch(companion -> !hasText(companion))) {
			throw new IllegalArgumentException("동행인 목록에는 빈 이름을 넣을 수 없습니다.");
		}
		if (!hasText(back.weather()) || !hasText(back.mood())) {
			throw new IllegalArgumentException("날씨와 기분은 필수입니다.");
		}
	}

	private static ValidatedCardBack validateDiaryBack(CardBackRequest back, int backPhotoCount) {
		if (!hasText(back.diaryText()) && backPhotoCount == 0) {
			throw new IllegalArgumentException("일기 또는 뒷면 사진 중 하나 이상이 필요합니다.");
		}
		if (back.writingMode() != null || hasText(back.title()) || hasText(back.memoryText())
			|| (back.answers() != null && !back.answers().isEmpty())) {
			throw new IllegalArgumentException("영수증과 손편지에는 티켓 기록 필드를 사용할 수 없습니다.");
		}

		return new ValidatedCardBack(
			back.companions(), back.weather().trim(), back.mood().trim(), normalize(back.diaryText()),
			null, null, null, null, null, backPhotoCount
		);
	}

	private static ValidatedCardBack validateTicketBack(
		CardBackRequest back,
		TicketSubtype confirmedTicketSubtype,
		int backPhotoCount
	) {
		if (backPhotoCount != 0) {
			throw new IllegalArgumentException("티켓에는 뒷면 사진을 저장할 수 없습니다.");
		}
		if (hasText(back.diaryText())) {
			throw new IllegalArgumentException("티켓에는 diaryText를 사용할 수 없습니다.");
		}
		if (back.writingMode() == null) {
			throw new IllegalArgumentException("티켓 작성 방식이 필요합니다.");
		}

		return switch (back.writingMode()) {
			case DIRECT -> validateDirectTicket(back);
			case AI_RECALL -> validateAiRecallTicket(back, confirmedTicketSubtype);
		};
	}

	private static ValidatedCardBack validateDirectTicket(CardBackRequest back) {
		if (!hasText(back.title()) || !hasText(back.memoryText())) {
			throw new IllegalArgumentException("직접 기록 티켓에는 제목과 추억이 필요합니다.");
		}
		if (back.answers() != null && !back.answers().isEmpty()) {
			throw new IllegalArgumentException("직접 기록 티켓에는 AI 회상 답변을 사용할 수 없습니다.");
		}

		return new ValidatedCardBack(
			back.companions(), back.weather().trim(), back.mood().trim(), null,
			WritingMode.DIRECT, null, back.title().trim(), back.memoryText().trim(), null, 0
		);
	}

	private static ValidatedCardBack validateAiRecallTicket(
		CardBackRequest back,
		TicketSubtype confirmedTicketSubtype
	) {
		if (confirmedTicketSubtype == null) {
			throw new IllegalArgumentException("AI 회상 티켓에는 확정된 티켓 세부 유형이 필요합니다.");
		}
		if (!hasText(back.title())) {
			throw new IllegalArgumentException("AI 회상 티켓에는 사용자가 확정한 제목이 필요합니다.");
		}
		if (hasText(back.memoryText())) {
			throw new IllegalArgumentException("AI 회상 티켓에는 memoryText를 사용할 수 없습니다.");
		}

		List<AnsweredQuestion> answers = TicketRecallAnswerValidator.validateAll(
			confirmedTicketSubtype,
			back.answers()
		);

		return new ValidatedCardBack(
			back.companions(), back.weather().trim(), back.mood().trim(), null,
			WritingMode.AI_RECALL, confirmedTicketSubtype, back.title().trim(), null, answers, 0
		);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String normalize(String value) {
		return hasText(value) ? value.trim() : null;
	}
}
