package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;

public record ReceiptFrontCandidate(
	LocalDate memoryDate,
	String storeName
) implements FrontCandidate {
}
