package com.memorydrawer.card.create.dto;

import java.util.List;

import com.memorydrawer.card.WritingMode;
import com.memorydrawer.ticket.recall.TicketRecallAnswer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardBackRequest(
	@NotNull List<String> companions,
	@NotBlank String weather,
	@NotBlank String mood,
	String diaryText,
	WritingMode writingMode,
	String title,
	String memoryText,
	List<TicketRecallAnswer> answers
) {
	public CardBackRequest {
		companions = companions == null ? null : List.copyOf(companions);
		answers = answers == null ? null : List.copyOf(answers);
	}
}
