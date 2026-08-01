package com.memorydrawer.card.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.card.create.dto.CardBackRequest;
import com.memorydrawer.card.create.dto.CardCreateRequest;
import com.memorydrawer.ticket.recall.TicketRecallAnswer;
import com.memorydrawer.ticket.recall.TicketSubtype;

class CardCreationValidatorTest {

	private static final UUID DRAFT_ID = UUID.fromString("9bb06555-85de-46e2-b44e-8f67eb8e08d2");

	@Test
	void acceptsReceiptWhenBackPhotoExistsWithoutDiaryText() {
		var request = request(new CardBackRequest(
			List.of(), "맑음", "행복", null, null, null, null, null
		));

		var result = CardCreationValidator.validate(DocumentType.RECEIPT, null, request, 1);

		assertThat(result.companions()).isEmpty();
		assertThat(result.diaryText()).isNull();
		assertThat(result.backPhotoCount()).isEqualTo(1);
	}

	@Test
	void rejectsLetterWithoutDiaryOrBackPhoto() {
		var request = request(new CardBackRequest(
			List.of("지수"), "흐림", "감동", "  ", null, null, null, null
		));

		assertThatIllegalArgumentException().isThrownBy(
			() -> CardCreationValidator.validate(DocumentType.LETTER, null, request, 0)
		);
	}

	@Test
	void acceptsDirectTicketWithoutSubtypeOrAnswers() {
		var request = request(new CardBackRequest(
			List.of("현수"), "맑음", "벅참", null, WritingMode.DIRECT,
			"여름밤의 흠뻑쇼", "마지막 앙코르까지 함께 노래했다.", null
		));

		var result = CardCreationValidator.validate(DocumentType.TICKET, null, request, 0);

		assertThat(result.writingMode()).isEqualTo(WritingMode.DIRECT);
		assertThat(result.ticketSubtype()).isNull();
		assertThat(result.answers()).isNull();
	}

	@Test
	void acceptsAiRecallTicketAndStoresServerQuestionText() {
		var request = request(new CardBackRequest(
			List.of("현수"), "맑음", "벅참", null, WritingMode.AI_RECALL,
			"함께 부른 마지막 앙코르", null,
			List.of(
				new TicketRecallAnswer("CONCERT_PERFORMANCE_1", "마지막 곡을 함께 불렀어요."),
				new TicketRecallAnswer("CONCERT_PERFORMANCE_2", null),
				new TicketRecallAnswer("CONCERT_PERFORMANCE_3", null)
			)
		));

		var result = CardCreationValidator.validate(
			DocumentType.TICKET, TicketSubtype.CONCERT_PERFORMANCE, request, 0
		);

		assertThat(result.writingMode()).isEqualTo(WritingMode.AI_RECALL);
		assertThat(result.ticketSubtype()).isEqualTo(TicketSubtype.CONCERT_PERFORMANCE);
		assertThat(result.memoryText()).isNull();
		assertThat(result.answers()).hasSize(3);
		assertThat(result.answers().get(0)).satisfies(answer -> {
			assertThat(answer.questionId()).isEqualTo("CONCERT_PERFORMANCE_1");
			assertThat(answer.question()).isEqualTo("가장 벅찼던 순간은 언제였나요?");
		});
		assertThat(result.answers()).element(1).satisfies(answer -> assertThat(answer.answer()).isNull());
		assertThat(result.answers()).element(2).satisfies(answer -> assertThat(answer.answer()).isNull());
	}

	@Test
	void rejectsAiRecallTicketWithoutConfirmedSubtype() {
		var request = request(new CardBackRequest(
			List.of(), "맑음", "행복", null, WritingMode.AI_RECALL, "제목", null,
			List.of(
				new TicketRecallAnswer("MOVIE_1", "답변"),
				new TicketRecallAnswer("MOVIE_2", null),
				new TicketRecallAnswer("MOVIE_3", null)
			)
		));

		assertThatIllegalArgumentException().isThrownBy(
			() -> CardCreationValidator.validate(DocumentType.TICKET, null, request, 0)
		);
	}

	@Test
	void rejectsBackPhotosForTicket() {
		var request = request(new CardBackRequest(
			List.of(), "맑음", "행복", null, WritingMode.DIRECT, "제목", "추억", null
		));

		assertThatIllegalArgumentException().isThrownBy(
			() -> CardCreationValidator.validate(DocumentType.TICKET, null, request, 1)
		);
	}

	@Test
	void rejectsMissingCommonFields() {
		var request = request(new CardBackRequest(
			null, " ", "행복", "일기", null, null, null, null
		));

		assertThatIllegalArgumentException().isThrownBy(
			() -> CardCreationValidator.validate(DocumentType.RECEIPT, null, request, 0)
		);
	}

	private static CardCreateRequest request(CardBackRequest back) {
		return new CardCreateRequest(DRAFT_ID, back);
	}
}
