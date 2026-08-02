package com.memorydrawer.card.manage.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;
import com.memorydrawer.card.create.dto.CardBackRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CardUpdateRequest(
	@NotNull LocalDate memoryDate,
	@NotNull JsonNode front,
	@NotNull @Valid CardBackRequest back
) {
}
