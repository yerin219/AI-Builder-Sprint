package com.memorydrawer.ticket.recall;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;
import com.memorydrawer.ticket.recall.dto.GenerateTitleRequest;
import com.memorydrawer.ticket.recall.dto.SubtypeSuggestionResponse;
import com.memorydrawer.ticket.recall.dto.TicketQuestionsResponse;
import com.memorydrawer.ticket.recall.dto.TitleCandidateResponse;

@Service
public class TicketRecallApplicationService {

	private final MemoryDraftRepository memoryDraftRepository;
	private final TicketRecallService ticketRecallService;
	private final ObjectMapper objectMapper;

	public TicketRecallApplicationService(
		MemoryDraftRepository memoryDraftRepository,
		TicketRecallService ticketRecallService,
		ObjectMapper objectMapper
	) {
		this.memoryDraftRepository = memoryDraftRepository;
		this.ticketRecallService = ticketRecallService;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public SubtypeSuggestionResponse suggestSubtype(UUID ownerId, UUID draftId) {
		MemoryDraft draft = recallEligibleDraft(ownerId, draftId);
		return ticketRecallService.suggestSubtype(parsedContent(draft).solarInput());
	}

	@Transactional
	public TicketQuestionsResponse confirmSubtype(
		UUID ownerId,
		UUID draftId,
		String requestedSubtype
	) {
		MemoryDraft draft = recallEligibleDraft(ownerId, draftId);
		TicketSubtype ticketSubtype = parseSubtype(requestedSubtype);
		draft.confirmTicketSubtype(ticketSubtype);
		memoryDraftRepository.saveAndFlush(draft);
		return ticketRecallService.questionsFor(ticketSubtype);
	}

	@Transactional(readOnly = true)
	public TitleCandidateResponse generateTitle(
		UUID ownerId,
		UUID draftId,
		GenerateTitleRequest request
	) {
		MemoryDraft draft = recallEligibleDraft(ownerId, draftId);
		if (draft.getTicketSubtype() == null) {
			throw new ApiException(ErrorCode.TICKET_002);
		}
		try {
			return ticketRecallService.generateTitle(
				draft.getTicketSubtype(),
				request.answers()
			);
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.TICKET_003, exception);
		} catch (IllegalStateException exception) {
			throw new ApiException(ErrorCode.AI_002, exception);
		}
	}

	private MemoryDraft recallEligibleDraft(UUID ownerId, UUID draftId) {
		MemoryDraft draft = memoryDraftRepository.findById(draftId)
			.orElseThrow(() -> new ApiException(ErrorCode.DRAFT_001));
		if (!draft.getOwnerId().equals(ownerId)) {
			throw new ApiException(ErrorCode.DRAFT_004);
		}
		if (draft.getDocumentType() != DocumentType.TICKET
			|| draft.getDraftStatus() != DraftStatus.FRONT_CONFIRMED) {
			throw new ApiException(ErrorCode.TICKET_002);
		}
		return draft;
	}

	private TicketSubtype parseSubtype(String value) {
		try {
			return TicketSubtype.valueOf(value.trim());
		} catch (NullPointerException | IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.TICKET_001, exception);
		}
	}

	private ParsedContent parsedContent(MemoryDraft draft) {
		try {
			return objectMapper.readValue(draft.getParsedContent(), ParsedContent.class);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.DOCUMENT_001, exception);
		}
	}
}
