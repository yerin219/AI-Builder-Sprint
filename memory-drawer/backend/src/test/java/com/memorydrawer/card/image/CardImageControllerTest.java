package com.memorydrawer.card.image;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.memorydrawer.auth.AuthenticatedUserIdResolver;
import com.memorydrawer.card.query.CardAccessDeniedException;
import com.memorydrawer.common.error.GlobalExceptionHandler;

class CardImageControllerTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private AuthenticatedUserIdResolver userIdResolver;
	private CardImageService cardImageService;
	private MockMvc mockMvc;
	private Principal principal;

	@BeforeEach
	void setUp() {
		userIdResolver = mock(AuthenticatedUserIdResolver.class);
		cardImageService = mock(CardImageService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(
			new CardImageController(userIdResolver, cardImageService)
		).setControllerAdvice(new GlobalExceptionHandler()).build();
		principal = () -> OWNER_ID.toString();
		when(userIdResolver.resolve(principal)).thenReturn(OWNER_ID);
	}

	@Test
	void servesAuthorizedFrontImageWithoutCaching() throws Exception {
		byte[] bytes = {1, 2, 3};
		when(cardImageService.front(OWNER_ID, CARD_ID))
			.thenReturn(new CardImageResource(bytes, MediaType.IMAGE_PNG));

		mockMvc.perform(get("/files/cards/{cardId}/front", CARD_ID).principal(principal))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_PNG))
			.andExpect(content().bytes(bytes))
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	@Test
	void returnsCommonErrorEnvelopeForAnotherUsersImage() throws Exception {
		when(cardImageService.back(OWNER_ID, CARD_ID, 1))
			.thenThrow(new CardAccessDeniedException());

		mockMvc.perform(get("/files/cards/{cardId}/back/{index}", CARD_ID, 1).principal(principal))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("CARD_001"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}
}
