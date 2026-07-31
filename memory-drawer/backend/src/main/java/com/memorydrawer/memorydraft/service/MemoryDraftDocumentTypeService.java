package com.memorydrawer.memorydraft.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.api.ConfirmDocumentTypeResponse;
import com.memorydrawer.memorydraft.api.FrontCandidate;
import com.memorydrawer.memorydraft.api.LetterFrontCandidate;
import com.memorydrawer.memorydraft.api.ReceiptFrontCandidate;
import com.memorydrawer.memorydraft.api.TicketFrontCandidate;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.extract.ExtractedFrontFields;
import com.memorydrawer.memorydraft.extract.InformationExtractClient;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@Service
public class MemoryDraftDocumentTypeService {

	private static final String NEXT_ACTION = "CONFIRM_FRONT";

	private final MemoryDraftRepository memoryDraftRepository;
	private final OriginalImageStorage originalImageStorage;
	private final InformationExtractClient informationExtractClient;
	private final ObjectMapper objectMapper;

	public MemoryDraftDocumentTypeService(
		MemoryDraftRepository memoryDraftRepository,
		OriginalImageStorage originalImageStorage,
		InformationExtractClient informationExtractClient,
		ObjectMapper objectMapper
	) {
		this.memoryDraftRepository = memoryDraftRepository;
		this.originalImageStorage = originalImageStorage;
		this.informationExtractClient = informationExtractClient;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public ConfirmDocumentTypeResponse confirm(
		UUID ownerId,
		UUID draftId,
		String requestedDocumentType
	) {
		DocumentType documentType = parseDocumentType(requestedDocumentType);
		MemoryDraft draft = findOwnedDraft(ownerId, draftId);
		if (draft.getDraftStatus() != DraftStatus.TYPE_PENDING) {
			throw new ApiException(ErrorCode.DRAFT_002);
		}

		FrontCandidate frontCandidate = createFrontCandidate(draft, documentType);
		String serializedFrontCandidate = serialize(frontCandidate);
		draft.confirmDocumentType(documentType, serializedFrontCandidate);
		memoryDraftRepository.saveAndFlush(draft);

		return new ConfirmDocumentTypeResponse(
			draft.getId(),
			documentType,
			frontCandidate,
			emptyFields(frontCandidate),
			draft.getDraftStatus(),
			NEXT_ACTION
		);
	}

	private MemoryDraft findOwnedDraft(UUID ownerId, UUID draftId) {
		MemoryDraft draft = memoryDraftRepository.findById(draftId)
			.orElseThrow(() -> new ApiException(ErrorCode.DRAFT_001));
		if (!draft.getOwnerId().equals(ownerId)) {
			throw new ApiException(ErrorCode.DRAFT_004);
		}
		return draft;
	}

	private DocumentType parseDocumentType(String value) {
		if (value == null) {
			throw new ApiException(ErrorCode.DOCUMENT_002);
		}
		try {
			return DocumentType.valueOf(value.trim());
		} catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.DOCUMENT_002, exception);
		}
	}

	private FrontCandidate createFrontCandidate(
		MemoryDraft draft,
		DocumentType documentType
	) {
		if (documentType == DocumentType.LETTER) {
			ParsedContent parsedContent = deserializeParsedContent(draft.getParsedContent());
			return new LetterFrontCandidate(null, normalizeText(parsedContent.text()));
		}

		byte[] imageBytes = originalImageStorage.load(draft.getOriginalImageKey());
		ExtractedFrontFields extracted = informationExtractClient.extract(
			documentType,
			imageBytes,
			draft.getOriginalImageContentType()
		);
		LocalDate memoryDate = normalizeDate(extracted.memoryDate());

		return documentType == DocumentType.RECEIPT
			? new ReceiptFrontCandidate(
				memoryDate,
				normalizeText(extracted.storeName())
			)
			: new TicketFrontCandidate(
				memoryDate,
				normalizeText(extracted.eventName()),
				normalizeText(extracted.venue()),
				normalizeText(extracted.seat())
			);
	}

	private ParsedContent deserializeParsedContent(String value) {
		try {
			return objectMapper.readValue(value, ParsedContent.class);
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.DOCUMENT_001, exception);
		}
	}

	private String serialize(FrontCandidate frontCandidate) {
		try {
			return objectMapper.writeValueAsString(frontCandidate);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("앞면 후보를 내부 형식으로 저장할 수 없습니다.", exception);
		}
	}

	private LocalDate normalizeDate(String value) {
		String normalized = normalizeText(value);
		if (normalized == null) {
			return null;
		}
		try {
			return LocalDate.parse(normalized);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	private String normalizeText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isBlank() ? null : normalized;
	}

	private List<String> emptyFields(FrontCandidate frontCandidate) {
		List<String> fields = new ArrayList<>();
		if (frontCandidate instanceof ReceiptFrontCandidate receipt) {
			addIfNull(fields, "memoryDate", receipt.memoryDate());
			addIfNull(fields, "storeName", receipt.storeName());
		} else if (frontCandidate instanceof TicketFrontCandidate ticket) {
			addIfNull(fields, "memoryDate", ticket.memoryDate());
			addIfNull(fields, "eventName", ticket.eventName());
			addIfNull(fields, "venue", ticket.venue());
			addIfNull(fields, "seat", ticket.seat());
		} else if (frontCandidate instanceof LetterFrontCandidate letter) {
			addIfNull(fields, "memoryDate", letter.memoryDate());
			addIfNull(fields, "ocrText", letter.ocrText());
		}
		return List.copyOf(fields);
	}

	private void addIfNull(List<String> fields, String fieldName, Object value) {
		if (value == null) {
			fields.add(fieldName);
		}
	}
}
