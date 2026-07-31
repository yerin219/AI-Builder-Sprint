package com.memorydrawer.ticket.recall.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmTicketSubtypeRequest(
	@NotBlank String ticketSubtype
) {
}
