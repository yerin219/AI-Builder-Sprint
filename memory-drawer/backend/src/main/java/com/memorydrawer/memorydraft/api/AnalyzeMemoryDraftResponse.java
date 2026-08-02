package com.memorydrawer.memorydraft.api;

import java.util.UUID;

import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;

public record AnalyzeMemoryDraftResponse(
	UUID draftId,
	DocumentType suggestedDocumentType,
	TypeCardResponse typeCard,
	boolean requiresManualSelection,
	DraftStatus draftStatus,
	String nextAction
) {

	public static AnalyzeMemoryDraftResponse from(MemoryDraft draft) {
		DocumentType suggestion = draft.getSuggestedDocumentType();
		boolean manualSelection = suggestion == null;
		return new AnalyzeMemoryDraftResponse(
			draft.getId(),
			suggestion,
			TypeCardResponse.from(suggestion),
			manualSelection,
			draft.getDraftStatus(),
			manualSelection ? "SELECT_DOCUMENT_TYPE" : "CONFIRM_DOCUMENT_TYPE"
		);
	}
}
