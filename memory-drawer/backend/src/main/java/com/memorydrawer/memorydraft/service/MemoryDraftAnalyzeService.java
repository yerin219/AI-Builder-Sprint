package com.memorydrawer.memorydraft.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.ai.documentparse.DocumentParseClient;
import com.memorydrawer.ai.solar.DocumentTypeClassifier;
import com.memorydrawer.memorydraft.api.AnalyzeMemoryDraftResponse;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.image.ImageFileValidator;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;
import com.memorydrawer.memorydraft.image.StoredImage;
import com.memorydrawer.memorydraft.image.ValidatedImage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@Service
public class MemoryDraftAnalyzeService {

	private final ImageFileValidator imageFileValidator;
	private final DocumentParseClient documentParseClient;
	private final DocumentTypeClassifier documentTypeClassifier;
	private final OriginalImageStorage originalImageStorage;
	private final MemoryDraftRepository memoryDraftRepository;
	private final ObjectMapper objectMapper;
	private final Duration retention;

	public MemoryDraftAnalyzeService(
		ImageFileValidator imageFileValidator,
		DocumentParseClient documentParseClient,
		DocumentTypeClassifier documentTypeClassifier,
		OriginalImageStorage originalImageStorage,
		MemoryDraftRepository memoryDraftRepository,
		ObjectMapper objectMapper,
		@Value("${memory-drawer.draft.retention}") Duration retention
	) {
		this.imageFileValidator = imageFileValidator;
		this.documentParseClient = documentParseClient;
		this.documentTypeClassifier = documentTypeClassifier;
		this.originalImageStorage = originalImageStorage;
		this.memoryDraftRepository = memoryDraftRepository;
		this.objectMapper = objectMapper;
		this.retention = retention;
	}

	@Transactional
	public AnalyzeMemoryDraftResponse analyze(UUID ownerId, MultipartFile image) {
		ValidatedImage validatedImage = imageFileValidator.validate(image);
		ParsedContent parsedContent = documentParseClient.parse(validatedImage);
		DocumentType suggestedDocumentType = documentTypeClassifier.classify(parsedContent);
		String serializedParsedContent = serialize(parsedContent);

		UUID draftId = UUID.randomUUID();
		StoredImage storedImage = originalImageStorage.store(ownerId, draftId, validatedImage);
		try {
			Instant createdAt = Instant.now();
			MemoryDraft draft = MemoryDraft.analyzed(
				draftId,
				ownerId,
				storedImage.key(),
				storedImage.contentType(),
				serializedParsedContent,
				suggestedDocumentType,
				createdAt,
				createdAt.plus(retention)
			);
			MemoryDraft savedDraft = memoryDraftRepository.saveAndFlush(draft);
			return AnalyzeMemoryDraftResponse.from(savedDraft);
		} catch (RuntimeException exception) {
			deleteStoredImage(storedImage, exception);
			throw exception;
		}
	}

	private String serialize(ParsedContent parsedContent) {
		try {
			return objectMapper.writeValueAsString(parsedContent);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Document Parse 결과를 내부 형식으로 저장할 수 없습니다.", exception);
		}
	}

	private void deleteStoredImage(StoredImage storedImage, RuntimeException originalException) {
		try {
			originalImageStorage.delete(storedImage.key());
		} catch (RuntimeException cleanupException) {
			originalException.addSuppressed(cleanupException);
		}
	}
}
