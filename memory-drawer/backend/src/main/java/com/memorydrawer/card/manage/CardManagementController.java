package com.memorydrawer.card.manage;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.memorydrawer.auth.AuthenticatedUserIdResolver;
import com.memorydrawer.card.manage.dto.CardDeleteResponse;
import com.memorydrawer.card.manage.dto.CardUpdateRequest;
import com.memorydrawer.card.manage.dto.CardUpdateResponse;
import com.memorydrawer.common.api.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cards")
public class CardManagementController {

	private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
	private final CardUpdateService cardUpdateService;
	private final CardDeleteService cardDeleteService;

	public CardManagementController(
		AuthenticatedUserIdResolver authenticatedUserIdResolver,
		CardUpdateService cardUpdateService,
		CardDeleteService cardDeleteService
	) {
		this.authenticatedUserIdResolver = authenticatedUserIdResolver;
		this.cardUpdateService = cardUpdateService;
		this.cardDeleteService = cardDeleteService;
	}

	@PutMapping(
		path = "/{cardId}",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ApiResponse<CardUpdateResponse>> update(
		@PathVariable UUID cardId,
		@Valid @RequestBody CardUpdateRequest request,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		return ResponseEntity.ok(ApiResponse.success(
			"추억 카드가 수정되었습니다.",
			cardUpdateService.update(ownerId, cardId, request)
		));
	}

	@DeleteMapping(path = "/{cardId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<CardDeleteResponse>> delete(
		@PathVariable UUID cardId,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		return ResponseEntity.ok(ApiResponse.success(
			"추억 카드가 삭제되었습니다.",
			cardDeleteService.delete(ownerId, cardId)
		));
	}
}
