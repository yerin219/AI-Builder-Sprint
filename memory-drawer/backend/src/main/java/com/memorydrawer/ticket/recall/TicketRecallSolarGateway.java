package com.memorydrawer.ticket.recall;

import java.util.List;
import java.util.Optional;

import com.memorydrawer.ticket.recall.TicketRecallAnswerValidator.AnsweredQuestion;

public interface TicketRecallSolarGateway {

	Optional<TicketSubtype> suggestSubtype(String parsedContent);

	String generateTitle(List<AnsweredQuestion> answeredQuestions);
}
