package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;
import java.util.UUID;

import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;

public record ConfirmFrontResponse(
	UUID draftId,
	DocumentType documentType,
	LocalDate memoryDate,
	ConfirmedFront front,
	DraftStatus draftStatus,
	String nextAction
) {
}
