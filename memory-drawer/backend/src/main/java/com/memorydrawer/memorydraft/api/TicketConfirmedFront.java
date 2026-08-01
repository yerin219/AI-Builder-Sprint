package com.memorydrawer.memorydraft.api;

public record TicketConfirmedFront(
	String eventName,
	String venue,
	String seat
) implements ConfirmedFront {
}
