package com.memorydrawer.ticket.recall;

import java.util.List;

public final class TicketRecallQuestionBank {

	private static final List<TicketRecallQuestion> CONCERT_PERFORMANCE_QUESTIONS = List.of(
		new TicketRecallQuestion("CONCERT_PERFORMANCE_1", 1, "가장 벅찼던 순간은 언제였나요?"),
		new TicketRecallQuestion("CONCERT_PERFORMANCE_2", 2, "그날 떼창하거나 함성을 질렀던 순간이 있나요?"),
		new TicketRecallQuestion("CONCERT_PERFORMANCE_3", 3, "어떤 곡에서 마음이 가장 크게 움직였나요?")
	);

	private static final List<TicketRecallQuestion> MOVIE_QUESTIONS = List.of(
		new TicketRecallQuestion("MOVIE_1", 1, "어떤 계기로 이 영화를 봤나요?"),
		new TicketRecallQuestion("MOVIE_2", 2, "가장 마음에 남은 장면이나 대사가 있나요?"),
		new TicketRecallQuestion("MOVIE_3", 3, "영화가 끝난 뒤 어떤 이야기를 나눴나요?")
	);

	private static final List<TicketRecallQuestion> EXHIBITION_QUESTIONS = List.of(
		new TicketRecallQuestion("EXHIBITION_1", 1, "어떤 계기로 이 전시를 보게 되었나요?"),
		new TicketRecallQuestion("EXHIBITION_2", 2, "가장 마음에 남은 작품은 무엇인가요?"),
		new TicketRecallQuestion("EXHIBITION_3", 3, "관람이 끝난 뒤 어떤 이야기를 나눴나요?")
	);

	private TicketRecallQuestionBank() {
	}

	public static List<TicketRecallQuestion> questionsFor(TicketSubtype ticketSubtype) {
		return switch (ticketSubtype) {
			case CONCERT_PERFORMANCE -> CONCERT_PERFORMANCE_QUESTIONS;
			case MOVIE -> MOVIE_QUESTIONS;
			case EXHIBITION -> EXHIBITION_QUESTIONS;
		};
	}
}
