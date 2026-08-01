package com.memorydrawer.card.create;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.memorydrawer.auth.AuthenticatedUserIdResolver;
import com.memorydrawer.card.create.dto.CardCreateRequest;
import com.memorydrawer.card.create.dto.CardCreateResponse;
import com.memorydrawer.common.api.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cards")
public class CardController {

	private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
	private final CardCreateService cardCreateService;

	public CardController(
		AuthenticatedUserIdResolver authenticatedUserIdResolver,
		CardCreateService cardCreateService
	) {
		this.authenticatedUserIdResolver = authenticatedUserIdResolver;
		this.cardCreateService = cardCreateService;
	}

	@PostMapping(
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ApiResponse<CardCreateResponse>> create(
		@Valid @RequestPart("card") CardCreateRequest request,
		@RequestPart(value = "backPhotos", required = false) List<MultipartFile> backPhotos,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		CardCreateResponse response = cardCreateService.create(ownerId, request, backPhotos);
		return ResponseEntity.status(201).body(
			ApiResponse.success("추억 카드가 저장되었습니다.", response)
		);
	}
}
