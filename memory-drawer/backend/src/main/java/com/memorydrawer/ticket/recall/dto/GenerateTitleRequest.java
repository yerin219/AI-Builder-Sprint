package com.memorydrawer.ticket.recall.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.memorydrawer.ticket.recall.TicketRecallAnswer;

import jakarta.validation.Valid;

public record GenerateTitleRequest(
	List<@Valid TicketRecallAnswer> answers
) {
	public GenerateTitleRequest {
		answers = answers == null
			? null
			: Collections.unmodifiableList(new ArrayList<>(answers));
	}
}
