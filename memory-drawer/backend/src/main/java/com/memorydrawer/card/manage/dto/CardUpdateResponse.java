package com.memorydrawer.card.manage.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.memorydrawer.card.DocumentType;

public record CardUpdateResponse(
	UUID cardId,
	DocumentType documentType,
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate memoryDate,
	int year
) {
	public static CardUpdateResponse updated(
		UUID cardId,
		DocumentType documentType,
		LocalDate memoryDate
	) {
		return new CardUpdateResponse(
			cardId,
			documentType,
			memoryDate,
			memoryDate.getYear()
		);
	}
}
