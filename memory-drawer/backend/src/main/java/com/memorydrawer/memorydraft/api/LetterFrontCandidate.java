package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;

public record LetterFrontCandidate(
	LocalDate memoryDate,
	String ocrText
) implements FrontCandidate {
}
