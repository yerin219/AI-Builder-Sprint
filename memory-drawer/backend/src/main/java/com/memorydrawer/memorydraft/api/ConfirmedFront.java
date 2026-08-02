package com.memorydrawer.memorydraft.api;

public sealed interface ConfirmedFront
	permits ReceiptConfirmedFront, TicketConfirmedFront, LetterConfirmedFront {
}
