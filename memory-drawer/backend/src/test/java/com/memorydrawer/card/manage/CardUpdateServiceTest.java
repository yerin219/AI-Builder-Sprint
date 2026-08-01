package com.memorydrawer.card.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.card.create.dto.CardBackRequest;
import com.memorydrawer.card.domain.MemoryCard;
import com.memorydrawer.card.manage.dto.CardUpdateRequest;
import com.memorydrawer.card.query.CardAccessDeniedException;
import com.memorydrawer.card.repository.MemoryCardRepository;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

class CardUpdateServiceTest {

	private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID DRAFT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void updatesOwnedReceiptAndKeepsExistingBackPhotos() throws Exception {
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		MemoryCard card = receiptCard();
		when(repository.findById(CARD_ID)).thenReturn(Optional.of(card));
		CardUpdateService service = new CardUpdateService(repository, objectMapper);

		CardUpdateRequest request = new CardUpdateRequest(
			LocalDate.of(2025, 12, 24),
			json("""
				{
				  "storeName": " 새 가게 ",
				  "purchaseItems": [
				    {"name": " 아메리카노 ", "quantity": 2}
				  ]
				}
				"""),
			new CardBackRequest(
				List.of("민지"), "맑음", "행복", null,
				null, null, null, null
			)
		);

		var response = service.update(OWNER_ID, CARD_ID, request);

		assertThat(response.cardId()).isEqualTo(CARD_ID);
		assertThat(response.memoryDate()).isEqualTo(LocalDate.of(2025, 12, 24));
		assertThat(response.year()).isEqualTo(2025);
		assertThat(json(card.getFrontData()).path("storeName").asText()).isEqualTo("새 가게");
		assertThat(json(card.getFrontData()).path("purchaseItems").get(0).path("name").asText())
			.isEqualTo("아메리카노");
		assertThat(json(card.getFrontData()).path("purchaseItems").get(0).path("quantity").asInt())
			.isEqualTo(2);
		assertThat(json(card.getBackData()).path("companions").get(0).asText()).isEqualTo("민지");
		assertThat(json(card.getBackData()).path("backPhotoCount").asInt()).isEqualTo(1);
		verify(repository).saveAndFlush(card);
	}

	@Test
	void rejectsChangingTicketWritingMode() throws Exception {
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		MemoryCard card = directTicketCard();
		when(repository.findById(CARD_ID)).thenReturn(Optional.of(card));
		CardUpdateService service = new CardUpdateService(repository, objectMapper);

		CardUpdateRequest request = new CardUpdateRequest(
			LocalDate.of(2026, 7, 25),
			json("{\"eventName\":\"공연\",\"venue\":\"공연장\",\"seat\":null}"),
			new CardBackRequest(
				List.of(), "맑음", "벅참", null,
				WritingMode.AI_RECALL, "제목", null, List.of()
			)
		);

		assertThatThrownBy(() -> service.update(OWNER_ID, CARD_ID, request))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_001)
			);
	}

	@Test
	void keepsReceiptItemsWhenLegacyUpdateOmitsPurchaseItems() throws Exception {
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		MemoryCard card = receiptCard();
		when(repository.findById(CARD_ID)).thenReturn(Optional.of(card));
		CardUpdateService service = new CardUpdateService(repository, objectMapper);

		CardUpdateRequest request = new CardUpdateRequest(
			LocalDate.of(2026, 1, 2),
			json("{\"storeName\":\"기존 가게\"}"),
			new CardBackRequest(
				List.of(), "흐림", "보통", null,
				null, null, null, null
			)
		);

		service.update(OWNER_ID, CARD_ID, request);

		JsonNode purchaseItem = json(card.getFrontData()).path("purchaseItems").get(0);
		assertThat(purchaseItem.path("name").asText()).isEqualTo("라떼");
		assertThat(purchaseItem.path("quantity").asInt()).isEqualTo(1);
	}

	@Test
	void rejectsUpdatingAnotherUsersCard() throws Exception {
		MemoryCardRepository repository = mock(MemoryCardRepository.class);
		when(repository.findById(CARD_ID)).thenReturn(Optional.of(receiptCard()));
		CardUpdateService service = new CardUpdateService(repository, objectMapper);
		CardUpdateRequest request = new CardUpdateRequest(
			LocalDate.of(2026, 1, 1),
			json("{\"storeName\":\"가게\",\"purchaseItems\":[]}"),
			new CardBackRequest(List.of(), "맑음", "행복", "기억", null, null, null, null)
		);

		assertThatThrownBy(() -> service.update(OTHER_OWNER_ID, CARD_ID, request))
			.isInstanceOf(CardAccessDeniedException.class);
	}

	private MemoryCard receiptCard() {
		return MemoryCard.create(
			CARD_ID,
			OWNER_ID,
			DRAFT_ID,
			DocumentType.RECEIPT,
			LocalDate.of(2026, 1, 1),
			"{\"storeName\":\"기존 가게\",\"purchaseItems\":[{\"name\":\"라떼\",\"quantity\":1}]}",
			"{\"companions\":[],\"weather\":\"흐림\",\"mood\":\"보통\",\"diaryText\":null,\"writingMode\":null,\"ticketSubtype\":null,\"title\":null,\"memoryText\":null,\"answers\":null,\"backPhotoCount\":1}",
			"drafts/owner/draft/original.jpg",
			"[\"cards/owner/card/back/1.jpg\"]",
			Instant.parse("2026-07-31T00:00:00Z")
		);
	}

	private MemoryCard directTicketCard() {
		return MemoryCard.create(
			CARD_ID,
			OWNER_ID,
			DRAFT_ID,
			DocumentType.TICKET,
			LocalDate.of(2026, 7, 25),
			"{\"eventName\":\"공연\",\"venue\":\"공연장\",\"seat\":null}",
			"{\"companions\":[],\"weather\":\"맑음\",\"mood\":\"벅참\",\"diaryText\":null,\"writingMode\":\"DIRECT\",\"ticketSubtype\":null,\"title\":\"제목\",\"memoryText\":\"추억\",\"answers\":null,\"backPhotoCount\":0}",
			"drafts/owner/draft/original.jpg",
			"[]",
			Instant.parse("2026-07-31T00:00:00Z")
		);
	}

	private JsonNode json(String value) throws Exception {
		return objectMapper.readTree(value);
	}
}
