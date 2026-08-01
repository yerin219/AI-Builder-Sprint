package com.memorydrawer.memorydraft.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import com.memorydrawer.receipt.PurchaseItem;

@Service
public class MemoryDraftDocumentTypeService {

	private static final String NEXT_ACTION = "CONFIRM_FRONT";
	private static final Set<String> EXCLUDED_PURCHASE_ITEM_PREFIXES = Set.of(
		"소계",
		"합계",
		"총합계",
		"총액",
		"결제금액",
		"받을금액",
		"청구금액",
		"부가세",
		"부가가치세",
		"공급가액",
		"과세물품",
		"면세물품",
		"할인",
		"쿠폰",
		"포인트",
		"적립",
		"결제수단",
		"현금",
		"신용카드",
		"체크카드",
		"카드번호",
		"승인번호",
		"주문번호",
		"거래번호",
		"사업자정보",
		"사업자등록번호",
		"대표자",
		"광고",
		"subtotal",
		"totalamount",
		"totaldue",
		"grandtotal",
		"vatamount",
		"taxamount",
		"discount",
		"coupon",
		"points",
		"payment",
		"cardnumber",
		"cardno",
		"cardpayment",
		"approval",
		"approvalnumber",
		"ordernumber",
		"orderno",
		"businessinfo",
		"businessnumber",
		"businessregistration",
		"advert"
	);
	private static final Set<String> EXCLUDED_PURCHASE_ITEM_NAMES = Set.of(
		"total",
		"vat",
		"tax",
		"card",
		"order",
		"business"
	);
	private static final Set<String> EXCLUDED_PURCHASE_OPTION_NAMES = Set.of(
		"ice",
		"iced",
		"hot",
		"샷추가",
		"extrashot",
		"포장",
		"테이크아웃",
		"takeout",
		"사이즈업",
		"sizeup",
		"옵션"
	);

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
		byte[] imageBytes = originalImageStorage.load(draft.getOriginalImageKey());
		ExtractedFrontFields extracted = informationExtractClient.extract(
			documentType,
			imageBytes,
			draft.getOriginalImageContentType()
		);

		if (documentType == DocumentType.LETTER) {
			ParsedContent parsedContent = deserializeParsedContent(draft.getParsedContent());
			return new LetterFrontCandidate(
				null,
				normalizeText(parsedContent.text()),
				normalizeText(extracted.sender()),
				normalizeText(extracted.recipient())
			);
		}
		LocalDate memoryDate = normalizeDate(extracted.memoryDate());

		return documentType == DocumentType.RECEIPT
			? new ReceiptFrontCandidate(
				memoryDate,
				normalizeText(extracted.storeName()),
				normalizePurchaseItems(extracted.purchaseItems())
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

	private List<PurchaseItem> normalizePurchaseItems(List<PurchaseItem> items) {
		if (items == null || items.isEmpty()) {
			return List.of();
		}

		List<PurchaseItem> normalizedItems = new ArrayList<>();
		for (PurchaseItem item : items) {
			if (item == null || item.quantity() < 1) {
				continue;
			}
			String name = normalizeText(item.name());
			if (name == null || isExcludedPurchaseItem(name)) {
				continue;
			}
			normalizedItems.add(new PurchaseItem(name, item.quantity()));
		}
		return List.copyOf(normalizedItems);
	}

	private boolean isExcludedPurchaseItem(String name) {
		String key = name.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{P}\\p{S}]+", "");
		if (EXCLUDED_PURCHASE_ITEM_NAMES.contains(key)
			|| EXCLUDED_PURCHASE_OPTION_NAMES.contains(key)) {
			return true;
		}
		return EXCLUDED_PURCHASE_ITEM_PREFIXES.stream().anyMatch(key::startsWith);
	}

	private List<String> emptyFields(FrontCandidate frontCandidate) {
		List<String> fields = new ArrayList<>();
		if (frontCandidate instanceof ReceiptFrontCandidate receipt) {
			addIfNull(fields, "memoryDate", receipt.memoryDate());
			addIfNull(fields, "storeName", receipt.storeName());
			if (receipt.purchaseItems().isEmpty()) {
				fields.add("purchaseItems");
			}
		} else if (frontCandidate instanceof TicketFrontCandidate ticket) {
			addIfNull(fields, "memoryDate", ticket.memoryDate());
			addIfNull(fields, "eventName", ticket.eventName());
			addIfNull(fields, "venue", ticket.venue());
			addIfNull(fields, "seat", ticket.seat());
		} else if (frontCandidate instanceof LetterFrontCandidate letter) {
			addIfNull(fields, "memoryDate", letter.memoryDate());
			addIfNull(fields, "ocrText", letter.ocrText());
			addIfNull(fields, "sender", letter.sender());
			addIfNull(fields, "recipient", letter.recipient());
		}
		return List.copyOf(fields);
	}

	private void addIfNull(List<String> fields, String fieldName, Object value) {
		if (value == null) {
			fields.add(fieldName);
		}
	}
}
