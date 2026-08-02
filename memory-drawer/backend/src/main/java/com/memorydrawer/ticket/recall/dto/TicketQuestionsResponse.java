package com.memorydrawer.ticket.recall.dto;

import java.util.List;

import com.memorydrawer.ticket.recall.TicketRecallQuestion;
import com.memorydrawer.ticket.recall.TicketSubtype;

public record TicketQuestionsResponse(
	TicketSubtype ticketSubtype,
	List<TicketRecallQuestion> questions
) {
	public TicketQuestionsResponse {
		questions = List.copyOf(questions);
	}
}
