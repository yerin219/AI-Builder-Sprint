package com.memorydrawer.ticket.recall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TicketRecallAnswerValidator {

	private TicketRecallAnswerValidator() {
	}

	public static List<AnsweredQuestion> validate(
		TicketSubtype ticketSubtype,
		List<TicketRecallAnswer> answers
	) {
		return validateAll(ticketSubtype, answers).stream()
			.filter(answeredQuestion -> answeredQuestion.answer() != null)
			.toList();
	}

	public static List<AnsweredQuestion> validateAll(
		TicketSubtype ticketSubtype,
		List<TicketRecallAnswer> answers
	) {
		List<TicketRecallQuestion> questions = TicketRecallQuestionBank.questionsFor(ticketSubtype);
		if (answers == null || answers.size() != questions.size()) {
			throw new IllegalArgumentException("확정된 유형의 질문 세 개를 모두 보내야 합니다.");
		}

		Map<String, TicketRecallAnswer> answersByQuestionId = new HashMap<>();
		for (TicketRecallAnswer answer : answers) {
			if (answer == null || answer.questionId() == null
				|| answersByQuestionId.putIfAbsent(answer.questionId(), answer) != null) {
				throw new IllegalArgumentException("질문 ID가 없거나 중복되었습니다.");
			}
		}

		List<AnsweredQuestion> allQuestions = questions.stream()
			.map(question -> toAnsweredQuestion(question, answersByQuestionId))
			.toList();

		if (allQuestions.stream().allMatch(answeredQuestion -> answeredQuestion.answer() == null)) {
			throw new IllegalArgumentException("최소 한 질문에는 답해야 합니다.");
		}

		return allQuestions;
	}

	private static AnsweredQuestion toAnsweredQuestion(
		TicketRecallQuestion question,
		Map<String, TicketRecallAnswer> answersByQuestionId
	) {
		TicketRecallAnswer answer = answersByQuestionId.remove(question.questionId());
		if (answer == null) {
			throw new IllegalArgumentException("티켓 세부 유형과 질문 ID가 일치하지 않습니다.");
		}

		String normalizedAnswer = normalize(answer.answer());
		return new AnsweredQuestion(question.questionId(), question.text(), normalizedAnswer);
	}

	private static String normalize(String answer) {
		if (answer == null || answer.isBlank()) {
			return null;
		}
		return answer.trim();
	}

	public record AnsweredQuestion(
		String questionId,
		String question,
		String answer
	) {
	}
}
