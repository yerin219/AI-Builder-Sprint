package com.memorydrawer.card.image;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.memorydrawer.auth.AuthenticatedUserIdResolver;

@RestController
@RequestMapping("/files/cards/{cardId}")
public class CardImageController {

	private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
	private final CardImageService cardImageService;

	public CardImageController(
		AuthenticatedUserIdResolver authenticatedUserIdResolver,
		CardImageService cardImageService
	) {
		this.authenticatedUserIdResolver = authenticatedUserIdResolver;
		this.cardImageService = cardImageService;
	}

	@GetMapping("/front")
	public ResponseEntity<byte[]> front(
		@PathVariable UUID cardId,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		return response(cardImageService.front(ownerId, cardId));
	}

	@GetMapping("/back/{index}")
	public ResponseEntity<byte[]> back(
		@PathVariable UUID cardId,
		@PathVariable int index,
		Principal principal
	) {
		UUID ownerId = authenticatedUserIdResolver.resolve(principal);
		return response(cardImageService.back(ownerId, cardId, index));
	}

	private ResponseEntity<byte[]> response(CardImageResource image) {
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noStore())
			.contentType(image.mediaType())
			.body(image.bytes());
	}
}
