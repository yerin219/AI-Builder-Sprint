package com.memorydrawer.ticket.recall;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.memorydrawer.auth.AuthenticatedUserIdResolver;
import com.memorydrawer.common.api.ApiResponse;
import com.memorydrawer.ticket.recall.dto.ConfirmTicketSubtypeRequest;
import com.memorydrawer.ticket.recall.dto.GenerateTitleRequest;
import com.memorydrawer.ticket.recall.dto.SubtypeSuggestionResponse;
import com.memorydrawer.ticket.recall.dto.TicketQuestionsResponse;
import com.memorydrawer.ticket.recall.dto.TitleCandidateResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/memory-drafts/{draftId}/ticket-recall")
public class TicketRecallController {

	private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
	private final TicketRecallApplicationService ticketRecallApplicationService;

	public TicketRecallController(
		AuthenticatedUserIdResolver authenticatedUserIdResolver,
		TicketRecallApplicationService ticketRecallApplicationService
	) {
		this.authenticatedUserIdResolver = authenticatedUserIdResolver;
		this.ticketRecallApplicationService = ticketRecallApplicationService;
	}

	@PostMapping(path = "/subtype-suggestion", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<SubtypeSuggestionResponse>> suggestSubtype(
		@PathVariable UUID draftId,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		SubtypeSuggestionResponse response = ticketRecallApplicationService
			.suggestSubtype(ownerId, draftId);
		String message = response.requiresManualSelection()
			? "티켓 세부 유형을 직접 선택해주세요."
			: "티켓 세부 유형을 분석했습니다.";
		return ResponseEntity.ok(ApiResponse.success(message, response));
	}

	@PostMapping(
		path = "/questions",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ApiResponse<TicketQuestionsResponse>> questions(
		@PathVariable UUID draftId,
		@Valid @RequestBody ConfirmTicketSubtypeRequest request,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		TicketQuestionsResponse response = ticketRecallApplicationService.confirmSubtype(
			ownerId,
			draftId,
			request.ticketSubtype()
		);
		return ResponseEntity.ok(ApiResponse.success("회상 질문을 조회했습니다.", response));
	}

	@PostMapping(
		path = "/title",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ApiResponse<TitleCandidateResponse>> generateTitle(
		@PathVariable UUID draftId,
		@Valid @RequestBody GenerateTitleRequest request,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		TitleCandidateResponse response = ticketRecallApplicationService.generateTitle(
			ownerId,
			draftId,
			request
		);
		return ResponseEntity.ok(ApiResponse.success("제목을 생성했습니다.", response));
	}
}
