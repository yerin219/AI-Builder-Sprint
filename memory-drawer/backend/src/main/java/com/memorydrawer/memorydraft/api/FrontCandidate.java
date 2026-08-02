package com.memorydrawer.memorydraft.api;

public sealed interface FrontCandidate
	permits ReceiptFrontCandidate, TicketFrontCandidate, LetterFrontCandidate {
}
