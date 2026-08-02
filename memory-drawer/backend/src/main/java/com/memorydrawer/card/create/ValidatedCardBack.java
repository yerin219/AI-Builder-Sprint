package com.memorydrawer.card.create;

import java.util.List;

import com.memorydrawer.card.WritingMode;
import com.memorydrawer.ticket.recall.TicketRecallAnswerValidator.AnsweredQuestion;
import com.memorydrawer.ticket.recall.TicketSubtype;

public record ValidatedCardBack(
	List<String> companions,
	String weather,
	String mood,
	String diaryText,
	WritingMode writingMode,
	TicketSubtype ticketSubtype,
	String title,
	String memoryText,
	List<AnsweredQuestion> answers,
	int backPhotoCount
) {
	public ValidatedCardBack {
		companions = List.copyOf(companions);
		answers = answers == null ? null : List.copyOf(answers);
	}
}
