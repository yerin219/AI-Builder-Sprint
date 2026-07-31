package com.memorydrawer.ticket.recall.dto;

import java.util.List;

import com.memorydrawer.ticket.recall.TicketRecallAnswer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerateTitleRequest(
	@NotNull @Size(min = 3, max = 3) List<@Valid TicketRecallAnswer> answers
) {
	public GenerateTitleRequest {
		answers = answers == null ? null : List.copyOf(answers);
	}
}
