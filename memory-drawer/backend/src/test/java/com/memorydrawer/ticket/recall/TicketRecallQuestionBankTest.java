package com.memorydrawer.ticket.recall;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketRecallQuestionBankTest {

	@Test
	void returnsAllThreeConcertQuestionsInOrder() {
		var questions = TicketRecallQuestionBank.questionsFor(TicketSubtype.CONCERT_PERFORMANCE);

		assertThat(questions).extracting(TicketRecallQuestion::questionId)
			.containsExactly(
				"CONCERT_PERFORMANCE_1",
				"CONCERT_PERFORMANCE_2",
				"CONCERT_PERFORMANCE_3"
			);
		assertThat(questions).extracting(TicketRecallQuestion::order).containsExactly(1, 2, 3);
	}
}
