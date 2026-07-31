package com.memorydrawer.ticket.recall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.memorydrawer.ticket.recall.TicketRecallAnswerValidator.AnsweredQuestion;
import com.memorydrawer.ticket.recall.dto.SubtypeSuggestionResponse.NextAction;

class TicketRecallServiceTest {

	@Test
	void returnsManualSelectionWhenSolarCannotSuggestSubtype() {
		var service = new TicketRecallService(new StubSolarGateway(Optional.empty(), "사용하지 않음"));

		var response = service.suggestSubtype("티켓 문서 내용");

		assertThat(response.suggestedTicketSubtype()).isNull();
		assertThat(response.requiresManualSelection()).isTrue();
		assertThat(response.nextAction()).isEqualTo(NextAction.SELECT_TICKET_SUBTYPE);
	}

	@Test
	void returnsAllQuestionsForConfirmedSubtypeWithoutSolarQuestionGeneration() {
		var service = new TicketRecallService(
			new StubSolarGateway(Optional.of(TicketSubtype.EXHIBITION), "사용하지 않음")
		);

		var response = service.questionsFor(TicketSubtype.EXHIBITION);

		assertThat(response.questions()).hasSize(3);
		assertThat(response.questions()).extracting(TicketRecallQuestion::questionId)
			.containsExactly("EXHIBITION_1", "EXHIBITION_2", "EXHIBITION_3");
	}

	@Test
	void sendsOnlyNonBlankUserAnswersToSolarTitleGeneration() {
		var gateway = new RecordingSolarGateway();
		var service = new TicketRecallService(gateway);
		var answers = List.of(
			new TicketRecallAnswer("MOVIE_1", "친구가 추천해서 봤어요."),
			new TicketRecallAnswer("MOVIE_2", null),
			new TicketRecallAnswer("MOVIE_3", "  ")
		);

		var response = service.generateTitle(TicketSubtype.MOVIE, answers);

		assertThat(response.titleCandidate()).isEqualTo("함께 본 영화");
		assertThat(gateway.receivedAnswers).containsExactly(new AnsweredQuestion(
			"MOVIE_1",
			"어떤 계기로 이 영화를 봤나요?",
			"친구가 추천해서 봤어요."
		));
	}

	@Test
	void rejectsMultilineSolarTitle() {
		var service = new TicketRecallService(
			new StubSolarGateway(Optional.of(TicketSubtype.MOVIE), "첫 줄\n두 번째 줄")
		);
		var answers = List.of(
			new TicketRecallAnswer("MOVIE_1", "답변"),
			new TicketRecallAnswer("MOVIE_2", null),
			new TicketRecallAnswer("MOVIE_3", null)
		);

		assertThatIllegalStateException()
			.isThrownBy(() -> service.generateTitle(TicketSubtype.MOVIE, answers));
	}

	private record StubSolarGateway(
		Optional<TicketSubtype> subtypeSuggestion,
		String titleCandidate
	) implements TicketRecallSolarGateway {

		@Override
		public Optional<TicketSubtype> suggestSubtype(String parsedContent) {
			return subtypeSuggestion;
		}

		@Override
		public String generateTitle(List<AnsweredQuestion> answeredQuestions) {
			return titleCandidate;
		}
	}

	private static final class RecordingSolarGateway implements TicketRecallSolarGateway {

		private List<AnsweredQuestion> receivedAnswers;

		@Override
		public Optional<TicketSubtype> suggestSubtype(String parsedContent) {
			return Optional.empty();
		}

		@Override
		public String generateTitle(List<AnsweredQuestion> answeredQuestions) {
			this.receivedAnswers = answeredQuestions;
			return "함께 본 영화";
		}
	}
}
