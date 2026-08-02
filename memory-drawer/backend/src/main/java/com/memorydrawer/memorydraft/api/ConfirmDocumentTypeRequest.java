package com.memorydrawer.memorydraft.api;

import jakarta.validation.constraints.NotBlank;

public record ConfirmDocumentTypeRequest(
	@NotBlank
	String documentType
) {
}
