package com.memorydrawer.ticket.recall.dto;

import com.memorydrawer.ticket.recall.TicketSubtype;

public record SubtypeSuggestionResponse(
	TicketSubtype suggestedTicketSubtype,
	boolean requiresManualSelection,
	NextAction nextAction
) {

	public static SubtypeSuggestionResponse suggested(TicketSubtype ticketSubtype) {
		return new SubtypeSuggestionResponse(ticketSubtype, false, NextAction.CONFIRM_TICKET_SUBTYPE);
	}

	public static SubtypeSuggestionResponse manualSelectionRequired() {
		return new SubtypeSuggestionResponse(null, true, NextAction.SELECT_TICKET_SUBTYPE);
	}

	public enum NextAction {
		CONFIRM_TICKET_SUBTYPE,
		SELECT_TICKET_SUBTYPE
	}
}
