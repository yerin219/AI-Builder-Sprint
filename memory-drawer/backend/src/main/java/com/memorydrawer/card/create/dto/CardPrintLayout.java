package com.memorydrawer.card.create.dto;

import com.memorydrawer.card.DocumentType;

public enum CardPrintLayout {
	NARROW_RECEIPT,
	LANDSCAPE_TICKET,
	LETTER_SHEET;

	public static CardPrintLayout from(DocumentType documentType) {
		return switch (documentType) {
			case RECEIPT -> NARROW_RECEIPT;
			case TICKET -> LANDSCAPE_TICKET;
			case LETTER -> LETTER_SHEET;
		};
	}
}
