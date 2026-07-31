package com.memorydrawer.memorydraft.api;

public record LetterConfirmedFront(
	String ocrText,
	FrontImageMode frontImageMode
) implements ConfirmedFront {
}
