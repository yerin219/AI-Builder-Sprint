package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;

public record LetterFrontCandidate(
	LocalDate memoryDate,
	String ocrText,
	String sender,
	String recipient
) implements FrontCandidate {
	public LetterFrontCandidate(LocalDate memoryDate, String ocrText) {
		this(memoryDate, ocrText, null, null);
	}
}
