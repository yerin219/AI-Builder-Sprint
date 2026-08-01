package com.memorydrawer.card.create.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.memorydrawer.card.DocumentType;

public record CardCreateResponse(
	UUID cardId,
	DocumentType documentType,
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate memoryDate,
	int year,
	String draftStatus
) {
	public static CardCreateResponse saved(
		UUID cardId,
		DocumentType documentType,
		LocalDate memoryDate
	) {
		return new CardCreateResponse(
			cardId,
			documentType,
			memoryDate,
			memoryDate.getYear(),
			"SAVED"
		);
	}
}
