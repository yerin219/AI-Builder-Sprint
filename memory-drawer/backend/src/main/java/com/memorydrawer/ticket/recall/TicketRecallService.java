package com.memorydrawer.ticket.recall;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.memorydrawer.ticket.recall.dto.SubtypeSuggestionResponse;
import com.memorydrawer.ticket.recall.dto.TicketQuestionsResponse;
import com.memorydrawer.ticket.recall.dto.TitleCandidateResponse;

@Service
public final class TicketRecallService {

	private final TicketRecallSolarGateway solarGateway;

	public TicketRecallService(TicketRecallSolarGateway solarGateway) {
		this.solarGateway = Objects.requireNonNull(solarGateway);
	}

	public SubtypeSuggestionResponse suggestSubtype(String parsedContent) {
		if (parsedContent == null || parsedContent.isBlank()) {
			throw new IllegalArgumentException("티켓 세부 유형 분석에 사용할 문서 내용이 필요합니다.");
		}

		return solarGateway.suggestSubtype(parsedContent)
			.map(SubtypeSuggestionResponse::suggested)
			.orElseGet(SubtypeSuggestionResponse::manualSelectionRequired);
	}

	public TicketQuestionsResponse questionsFor(TicketSubtype ticketSubtype) {
		Objects.requireNonNull(ticketSubtype, "티켓 세부 유형이 필요합니다.");
		return new TicketQuestionsResponse(
			ticketSubtype,
			TicketRecallQuestionBank.questionsFor(ticketSubtype)
		);
	}

	public TitleCandidateResponse generateTitle(
		TicketSubtype ticketSubtype,
		List<TicketRecallAnswer> answers
	) {
		var answeredQuestions = TicketRecallAnswerValidator.validate(ticketSubtype, answers);
		String titleCandidate = solarGateway.generateTitle(answeredQuestions);
		if (titleCandidate == null || titleCandidate.isBlank()
			|| titleCandidate.lines().count() != 1) {
			throw new IllegalStateException("Solar 제목 생성 결과는 비어 있지 않은 한 줄이어야 합니다.");
		}

		return new TitleCandidateResponse(titleCandidate.trim());
	}
}
