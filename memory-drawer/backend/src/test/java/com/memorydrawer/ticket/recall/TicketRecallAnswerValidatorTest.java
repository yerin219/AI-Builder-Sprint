package com.memorydrawer.ticket.recall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

class TicketRecallAnswerValidatorTest {

	@Test
	void returnsOnlyNonBlankAnswersWithServerQuestionText() {
		var answers = List.of(
			new TicketRecallAnswer("MOVIE_1", " 친구가 추천해서 봤어요. "),
			new TicketRecallAnswer("MOVIE_2", null),
			new TicketRecallAnswer("MOVIE_3", "  ")
		);

		var result = TicketRecallAnswerValidator.validate(TicketSubtype.MOVIE, answers);

		assertThat(result).containsExactly(new TicketRecallAnswerValidator.AnsweredQuestion(
			"MOVIE_1",
			"어떤 계기로 이 영화를 봤나요?",
			"친구가 추천해서 봤어요."
		));
	}

	@Test
	void rejectsDuplicateQuestionIds() {
		var answers = List.of(
			new TicketRecallAnswer("MOVIE_1", "답변"),
			new TicketRecallAnswer("MOVIE_1", null),
			new TicketRecallAnswer("MOVIE_3", null)
		);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> TicketRecallAnswerValidator.validate(TicketSubtype.MOVIE, answers));
	}

	@Test
	void rejectsQuestionIdsFromAnotherSubtype() {
		var answers = List.of(
			new TicketRecallAnswer("MOVIE_1", "답변"),
			new TicketRecallAnswer("MOVIE_2", null),
			new TicketRecallAnswer("EXHIBITION_3", null)
		);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> TicketRecallAnswerValidator.validate(TicketSubtype.MOVIE, answers));
	}

	@Test
	void rejectsWhenEveryAnswerIsBlank() {
		var answers = List.of(
			new TicketRecallAnswer("EXHIBITION_1", null),
			new TicketRecallAnswer("EXHIBITION_2", ""),
			new TicketRecallAnswer("EXHIBITION_3", "  ")
		);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> TicketRecallAnswerValidator.validate(TicketSubtype.EXHIBITION, answers));
	}
}
