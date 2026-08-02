package com.memorydrawer.memorydraft.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.memorydrawer.auth.AuthenticatedUserIdResolver;
import com.memorydrawer.common.api.ApiResponse;
import com.memorydrawer.memorydraft.service.MemoryDraftAnalyzeService;
import com.memorydrawer.memorydraft.service.MemoryDraftDocumentTypeService;
import com.memorydrawer.memorydraft.service.MemoryDraftFrontService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/memory-drafts")
public class MemoryDraftController {

	private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
	private final MemoryDraftAnalyzeService memoryDraftAnalyzeService;
	private final MemoryDraftDocumentTypeService memoryDraftDocumentTypeService;
	private final MemoryDraftFrontService memoryDraftFrontService;

	public MemoryDraftController(
		AuthenticatedUserIdResolver authenticatedUserIdResolver,
		MemoryDraftAnalyzeService memoryDraftAnalyzeService,
		MemoryDraftDocumentTypeService memoryDraftDocumentTypeService,
		MemoryDraftFrontService memoryDraftFrontService
	) {
		this.authenticatedUserIdResolver = authenticatedUserIdResolver;
		this.memoryDraftAnalyzeService = memoryDraftAnalyzeService;
		this.memoryDraftDocumentTypeService = memoryDraftDocumentTypeService;
		this.memoryDraftFrontService = memoryDraftFrontService;
	}

	@PostMapping(
		path = "/analyze",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ApiResponse<AnalyzeMemoryDraftResponse>> analyze(
		@RequestPart("image") MultipartFile image,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		AnalyzeMemoryDraftResponse response = memoryDraftAnalyzeService.analyze(ownerId, image);
		String message = response.requiresManualSelection()
			? "문서 유형을 직접 선택해주세요."
			: "문서 유형을 분석했습니다.";
		return ResponseEntity.ok(ApiResponse.success(message, response));
	}

	@PostMapping(
		path = "/{draftId}/document-type/confirm",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ApiResponse<ConfirmDocumentTypeResponse>> confirmDocumentType(
		@PathVariable UUID draftId,
		@Valid @RequestBody ConfirmDocumentTypeRequest request,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		ConfirmDocumentTypeResponse response = memoryDraftDocumentTypeService.confirm(
			ownerId,
			draftId,
			request.documentType()
		);
		String message = switch (response.documentType()) {
			case RECEIPT -> "영수증 정보를 추출했습니다.";
			case TICKET -> "티켓 정보를 추출했습니다.";
			case LETTER -> "손편지 내용을 추출했습니다.";
		};
		return ResponseEntity.ok(ApiResponse.success(message, response));
	}

	@PutMapping(
		path = "/{draftId}/front/confirm",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ApiResponse<ConfirmFrontResponse>> confirmFront(
		@PathVariable UUID draftId,
		@Valid @RequestBody ConfirmFrontRequest request,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		ConfirmFrontResponse response = memoryDraftFrontService.confirm(
			ownerId,
			draftId,
			request
		);
		return ResponseEntity.ok(
			ApiResponse.success("카드 앞면이 확정되었습니다.", response)
		);
	}
}
