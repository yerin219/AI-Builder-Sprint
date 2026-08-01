package com.memorydrawer.memorydraft.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import com.memorydrawer.ticket.recall.TicketSubtype;

@Entity
@Table(name = "memory_drafts")
public class MemoryDraft {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", nullable = false, length = 36)
	private UUID id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "owner_id", nullable = false, length = 36)
	private UUID ownerId;

	@Column(name = "original_image_key", nullable = false, length = 512)
	private String originalImageKey;

	@Column(name = "original_image_content_type", nullable = false, length = 64)
	private String originalImageContentType;

	@Lob
	@Column(name = "parsed_content", nullable = false, columnDefinition = "LONGTEXT")
	private String parsedContent;

	@Enumerated(EnumType.STRING)
	@Column(name = "suggested_document_type", length = 32)
	private DocumentType suggestedDocumentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", length = 32)
	private DocumentType documentType;

	@Lob
	@Column(name = "front_candidate", columnDefinition = "LONGTEXT")
	private String frontCandidate;

	@Enumerated(EnumType.STRING)
	@Column(name = "ticket_subtype", length = 32)
	private TicketSubtype ticketSubtype;

	@Enumerated(EnumType.STRING)
	@Column(name = "draft_status", nullable = false, length = 32)
	private DraftStatus draftStatus;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected MemoryDraft() {
	}

	private MemoryDraft(
		UUID id,
		UUID ownerId,
		String originalImageKey,
		String originalImageContentType,
		String parsedContent,
		DocumentType suggestedDocumentType,
		Instant createdAt,
		Instant expiresAt
	) {
		this.id = id;
		this.ownerId = ownerId;
		this.originalImageKey = originalImageKey;
		this.originalImageContentType = originalImageContentType;
		this.parsedContent = parsedContent;
		this.suggestedDocumentType = suggestedDocumentType;
		this.draftStatus = DraftStatus.TYPE_PENDING;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public static MemoryDraft analyzed(
		UUID id,
		UUID ownerId,
		String originalImageKey,
		String originalImageContentType,
		String parsedContent,
		DocumentType suggestedDocumentType,
		Instant createdAt,
		Instant expiresAt
	) {
		return new MemoryDraft(
			id,
			ownerId,
			originalImageKey,
			originalImageContentType,
			parsedContent,
			suggestedDocumentType,
			createdAt,
			expiresAt
		);
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public String getOriginalImageKey() {
		return originalImageKey;
	}

	public String getOriginalImageContentType() {
		return originalImageContentType;
	}

	public String getParsedContent() {
		return parsedContent;
	}

	public DocumentType getSuggestedDocumentType() {
		return suggestedDocumentType;
	}

	public DocumentType getDocumentType() {
		return documentType;
	}

	public String getFrontCandidate() {
		return frontCandidate;
	}

	public DraftStatus getDraftStatus() {
		return draftStatus;
	}

	public TicketSubtype getTicketSubtype() {
		return ticketSubtype;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void confirmDocumentType(DocumentType documentType, String frontCandidate) {
		if (draftStatus != DraftStatus.TYPE_PENDING) {
			throw new IllegalStateException("문서 유형을 확정할 수 없는 임시 기록 상태입니다.");
		}
		this.documentType = documentType;
		this.frontCandidate = frontCandidate;
		this.draftStatus = DraftStatus.FRONT_PENDING;
	}

	public void confirmFront(String confirmedFront) {
		if (draftStatus != DraftStatus.FRONT_PENDING
			&& draftStatus != DraftStatus.FRONT_CONFIRMED) {
			throw new IllegalStateException("카드 앞면을 확정할 수 없는 임시 기록 상태입니다.");
		}
		if (confirmedFront == null || confirmedFront.isBlank()) {
			throw new IllegalArgumentException("확정된 카드 앞면은 비어 있을 수 없습니다.");
		}
		this.frontCandidate = confirmedFront;
		this.draftStatus = DraftStatus.FRONT_CONFIRMED;
	}

	public void confirmTicketSubtype(TicketSubtype ticketSubtype) {
		if (draftStatus != DraftStatus.FRONT_CONFIRMED
			|| documentType != DocumentType.TICKET) {
			throw new IllegalStateException("티켓 회상을 진행할 수 없는 임시 기록 상태입니다.");
		}
		if (ticketSubtype == null) {
			throw new IllegalArgumentException("티켓 세부 유형은 비어 있을 수 없습니다.");
		}
		this.ticketSubtype = ticketSubtype;
	}

	public void markSaved() {
		if (draftStatus != DraftStatus.FRONT_CONFIRMED) {
			throw new IllegalStateException("카드를 저장할 수 없는 임시 기록 상태입니다.");
		}
		this.draftStatus = DraftStatus.SAVED;
	}
}
