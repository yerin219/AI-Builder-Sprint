package com.memorydrawer.memorydraft.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.ai.documentparse.DocumentParseClient;
import com.memorydrawer.ai.solar.DocumentTypeClassifier;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.image.ImageFileValidator;
import com.memorydrawer.memorydraft.image.OriginalImageStorage;
import com.memorydrawer.memorydraft.image.StoredImage;
import com.memorydrawer.memorydraft.image.ValidatedImage;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@ExtendWith(MockitoExtension.class)
class MemoryDraftAnalyzeServiceTests {

	@Mock
	private ImageFileValidator imageFileValidator;
	@Mock
	private DocumentParseClient documentParseClient;
	@Mock
	private DocumentTypeClassifier documentTypeClassifier;
	@Mock
	private OriginalImageStorage originalImageStorage;
	@Mock
	private MemoryDraftRepository memoryDraftRepository;

	private MemoryDraftAnalyzeService service;

	@BeforeEach
	void setUp() {
		service = new MemoryDraftAnalyzeService(
			imageFileValidator,
			documentParseClient,
			documentTypeClassifier,
			originalImageStorage,
			memoryDraftRepository,
			new ObjectMapper(),
			Duration.ofDays(7)
		);
	}

	@Test
	void storesTypePendingDraftOnlyAfterDocumentParseAndSolarSucceed() {
		UUID ownerId = UUID.randomUUID();
		MockMultipartFile multipartFile = new MockMultipartFile("image", new byte[] {1});
		ValidatedImage image = new ValidatedImage(
			new byte[] {(byte)0xff, (byte)0xd8, (byte)0xff},
			"image/jpeg",
			"jpg"
		);
		ParsedContent parsedContent = new ParsedContent("콘서트 티켓", "<p>콘서트 티켓</p>");
		when(imageFileValidator.validate(multipartFile)).thenReturn(image);
		when(documentParseClient.parse(image)).thenReturn(parsedContent);
		when(documentTypeClassifier.classify(parsedContent)).thenReturn(DocumentType.TICKET);
		when(originalImageStorage.store(any(), any(), any()))
			.thenAnswer(invocation -> new StoredImage(
				"drafts/" + invocation.getArgument(0) + "/" + invocation.getArgument(1) + "/original.jpg",
				"image/jpeg"
			));
		when(memoryDraftRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.analyze(ownerId, multipartFile);

		assertThat(response.suggestedDocumentType()).isEqualTo(DocumentType.TICKET);
		assertThat(response.requiresManualSelection()).isFalse();
		assertThat(response.draftStatus()).isEqualTo(DraftStatus.TYPE_PENDING);
		assertThat(response.nextAction()).isEqualTo("CONFIRM_DOCUMENT_TYPE");

		ArgumentCaptor<MemoryDraft> draftCaptor = ArgumentCaptor.forClass(MemoryDraft.class);
		verify(memoryDraftRepository).saveAndFlush(draftCaptor.capture());
		MemoryDraft savedDraft = draftCaptor.getValue();
		assertThat(savedDraft.getOwnerId()).isEqualTo(ownerId);
		assertThat(savedDraft.getOriginalImageKey()).contains(response.draftId().toString());
		assertThat(savedDraft.getParsedContent()).contains("콘서트 티켓");

		InOrder order = inOrder(documentParseClient, documentTypeClassifier, originalImageStorage, memoryDraftRepository);
		order.verify(documentParseClient).parse(image);
		order.verify(documentTypeClassifier).classify(parsedContent);
		order.verify(originalImageStorage).store(any(), any(), any());
		order.verify(memoryDraftRepository).saveAndFlush(any());
	}

	@Test
	void mapsUnknownSuggestionToManualSelection() {
		UUID ownerId = UUID.randomUUID();
		MockMultipartFile multipartFile = new MockMultipartFile("image", new byte[] {1});
		ValidatedImage image = new ValidatedImage(new byte[] {1}, "image/jpeg", "jpg");
		ParsedContent parsedContent = new ParsedContent("분류 불가", "");
		when(imageFileValidator.validate(multipartFile)).thenReturn(image);
		when(documentParseClient.parse(image)).thenReturn(parsedContent);
		when(documentTypeClassifier.classify(parsedContent)).thenReturn(null);
		when(originalImageStorage.store(any(), any(), any()))
			.thenReturn(new StoredImage("drafts/image.jpg", "image/jpeg"));
		when(memoryDraftRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.analyze(ownerId, multipartFile);

		assertThat(response.suggestedDocumentType()).isNull();
		assertThat(response.typeCard()).isNull();
		assertThat(response.requiresManualSelection()).isTrue();
		assertThat(response.nextAction()).isEqualTo("SELECT_DOCUMENT_TYPE");
	}

	@Test
	void doesNotStoreImageOrDraftWhenDocumentParseFails() {
		UUID ownerId = UUID.randomUUID();
		MockMultipartFile multipartFile = new MockMultipartFile("image", new byte[] {1});
		ValidatedImage image = new ValidatedImage(new byte[] {1}, "image/jpeg", "jpg");
		when(imageFileValidator.validate(multipartFile)).thenReturn(image);
		when(documentParseClient.parse(image)).thenThrow(new ApiException(ErrorCode.AI_001));

		assertThatThrownBy(() -> service.analyze(ownerId, multipartFile))
			.isInstanceOf(ApiException.class);

		verifyNoInteractions(documentTypeClassifier, originalImageStorage, memoryDraftRepository);
	}

	@Test
	void deletesStoredImageWhenDatabaseSaveFails() {
		UUID ownerId = UUID.randomUUID();
		MockMultipartFile multipartFile = new MockMultipartFile("image", new byte[] {1});
		ValidatedImage image = new ValidatedImage(new byte[] {1}, "image/jpeg", "jpg");
		ParsedContent parsedContent = new ParsedContent("영수증", "");
		StoredImage storedImage = new StoredImage("drafts/image.jpg", "image/jpeg");
		when(imageFileValidator.validate(multipartFile)).thenReturn(image);
		when(documentParseClient.parse(image)).thenReturn(parsedContent);
		when(documentTypeClassifier.classify(parsedContent)).thenReturn(DocumentType.RECEIPT);
		when(originalImageStorage.store(any(), any(), any())).thenReturn(storedImage);
		when(memoryDraftRepository.saveAndFlush(any())).thenThrow(new RuntimeException("db failure"));

		assertThatThrownBy(() -> service.analyze(ownerId, multipartFile))
			.hasMessage("db failure");

		verify(originalImageStorage).delete(storedImage.key());
		verify(memoryDraftRepository, never()).save(any());
	}
}
