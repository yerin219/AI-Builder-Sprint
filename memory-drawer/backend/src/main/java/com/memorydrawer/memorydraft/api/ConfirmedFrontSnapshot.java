package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;

public record ConfirmedFrontSnapshot(
	LocalDate memoryDate,
	JsonNode front
) {
}
