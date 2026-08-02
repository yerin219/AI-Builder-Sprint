package com.memorydrawer.card.create.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CardCreateRequest(
	@NotNull UUID draftId,
	@NotNull @Valid CardBackRequest back
) {
}
