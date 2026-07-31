package com.memorydrawer.memorydraft.api;

import com.memorydrawer.memorydraft.domain.DocumentType;

public record TypeCardResponse(
	DocumentType type,
	String label
) {

	public static TypeCardResponse from(DocumentType documentType) {
		if (documentType == null) {
			return null;
		}
		return new TypeCardResponse(documentType, documentType.getLabel());
	}
}
