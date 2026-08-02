package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;

public record TicketFrontCandidate(
	LocalDate memoryDate,
	String eventName,
	String venue,
	String seat
) implements FrontCandidate {
}
