package com.memorydrawer.card.query;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.memorydrawer.auth.AuthenticatedUserIdResolver;
import com.memorydrawer.card.query.dto.CardDetailResponse;
import com.memorydrawer.common.api.ApiResponse;

@RestController
@RequestMapping("/cards")
public class CardQueryController {

	private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
	private final CardQueryService cardQueryService;

	public CardQueryController(
		AuthenticatedUserIdResolver authenticatedUserIdResolver,
		CardQueryService cardQueryService
	) {
		this.authenticatedUserIdResolver = authenticatedUserIdResolver;
		this.cardQueryService = cardQueryService;
	}

	@GetMapping(path = "/{cardId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<CardDetailResponse>> card(
		@PathVariable UUID cardId,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		return ResponseEntity.ok(ApiResponse.success(
			"카드 상세 정보를 조회했습니다.",
			cardQueryService.card(ownerId, cardId)
		));
	}
}
