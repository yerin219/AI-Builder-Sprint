package com.memorydrawer.memorydraft.api;

import java.util.List;
import java.util.UUID;

import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;

public record ConfirmDocumentTypeResponse(
	UUID draftId,
	DocumentType documentType,
	FrontCandidate frontCandidate,
	List<String> emptyFields,
	DraftStatus draftStatus,
	String nextAction
) {
}
