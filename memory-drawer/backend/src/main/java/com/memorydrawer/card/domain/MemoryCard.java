package com.memorydrawer.card.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.memorydrawer.card.DocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards")
public class MemoryCard {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", nullable = false, length = 36)
	private UUID id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "owner_id", nullable = false, length = 36)
	private UUID ownerId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "draft_id", nullable = false, unique = true, length = 36)
	private UUID draftId;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 32)
	private DocumentType documentType;

	@Column(name = "memory_date", nullable = false)
	private LocalDate memoryDate;

	@Lob
	@Column(name = "front_data", nullable = false, columnDefinition = "LONGTEXT")
	private String frontData;

	@Lob
	@Column(name = "back_data", nullable = false, columnDefinition = "LONGTEXT")
	private String backData;

	@Column(name = "original_image_key", nullable = false, length = 512)
	private String originalImageKey;

	@Lob
	@Column(name = "back_photo_keys", nullable = false, columnDefinition = "LONGTEXT")
	private String backPhotoKeys;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected MemoryCard() {
	}

	private MemoryCard(
		UUID id,
		UUID ownerId,
		UUID draftId,
		DocumentType documentType,
		LocalDate memoryDate,
		String frontData,
		String backData,
		String originalImageKey,
		String backPhotoKeys,
		Instant createdAt
	) {
		this.id = id;
		this.ownerId = ownerId;
		this.draftId = draftId;
		this.documentType = documentType;
		this.memoryDate = memoryDate;
		this.frontData = frontData;
		this.backData = backData;
		this.originalImageKey = originalImageKey;
		this.backPhotoKeys = backPhotoKeys;
		this.createdAt = createdAt;
	}

	public static MemoryCard create(
		UUID id,
		UUID ownerId,
		UUID draftId,
		DocumentType documentType,
		LocalDate memoryDate,
		String frontData,
		String backData,
		String originalImageKey,
		String backPhotoKeys,
		Instant createdAt
	) {
		return new MemoryCard(
			id,
			ownerId,
			draftId,
			documentType,
			memoryDate,
			frontData,
			backData,
			originalImageKey,
			backPhotoKeys,
			createdAt
		);
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public UUID getDraftId() {
		return draftId;
	}

	public DocumentType getDocumentType() {
		return documentType;
	}

	public LocalDate getMemoryDate() {
		return memoryDate;
	}

	public String getFrontData() {
		return frontData;
	}

	public String getBackData() {
		return backData;
	}

	public String getOriginalImageKey() {
		return originalImageKey;
	}

	public String getBackPhotoKeys() {
		return backPhotoKeys;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void update(
		LocalDate memoryDate,
		String frontData,
		String backData
	) {
		this.memoryDate = memoryDate;
		this.frontData = frontData;
		this.backData = backData;
	}
}
