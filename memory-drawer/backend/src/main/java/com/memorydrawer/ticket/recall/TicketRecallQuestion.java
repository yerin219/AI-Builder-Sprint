package com.memorydrawer.ticket.recall;

public record TicketRecallQuestion(
	String questionId,
	int order,
	String text
) {
}
