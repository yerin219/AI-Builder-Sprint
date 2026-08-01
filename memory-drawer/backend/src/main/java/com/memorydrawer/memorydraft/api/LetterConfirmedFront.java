package com.memorydrawer.memorydraft.api;

public record LetterConfirmedFront(
	String ocrText,
	String sender,
	String recipient,
	FrontImageMode frontImageMode
) implements ConfirmedFront {
	public LetterConfirmedFront(String ocrText, FrontImageMode frontImageMode) {
		this(ocrText, null, null, frontImageMode);
	}
}
