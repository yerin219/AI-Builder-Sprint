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
import com.memorydrawer.card.query.dto.DrawerListResponse;
import com.memorydrawer.card.query.dto.YearCardListResponse;
import com.memorydrawer.common.api.ApiResponse;

@RestController
@RequestMapping("/drawers")
public class DrawerController {

	private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
	private final CardQueryService cardQueryService;

	public DrawerController(
		AuthenticatedUserIdResolver authenticatedUserIdResolver,
		CardQueryService cardQueryService
	) {
		this.authenticatedUserIdResolver = authenticatedUserIdResolver;
		this.cardQueryService = cardQueryService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<DrawerListResponse>> drawers(Principal principal) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		return ResponseEntity.ok(ApiResponse.success(
			"연도별 서랍을 조회했습니다.",
			cardQueryService.drawers(ownerId)
		));
	}

	@GetMapping(path = "/{year}/cards", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<YearCardListResponse>> cards(
		@PathVariable int year,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		return ResponseEntity.ok(ApiResponse.success(
			"연도별 카드를 조회했습니다.",
			cardQueryService.cards(ownerId, year)
		));
	}
}
