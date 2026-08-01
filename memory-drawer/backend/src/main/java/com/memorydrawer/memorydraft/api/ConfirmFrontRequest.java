package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

public record ConfirmFrontRequest(
	@NotNull
	LocalDate memoryDate,

	@NotNull
	JsonNode front
) {
}
