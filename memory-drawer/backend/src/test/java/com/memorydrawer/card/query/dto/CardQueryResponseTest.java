package com.memorydrawer.card.query.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;
import com.memorydrawer.card.WritingMode;
import com.memorydrawer.ticket.recall.TicketSubtype;

class CardQueryResponseTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void emptyDrawerListIsSerializedAsAnEmptyArray() throws Exception {
		JsonNode json = objectMapper.valueToTree(new DrawerListResponse(List.of()));

		assertThat(json.get("drawers").isArray()).isTrue();
		assertThat(json.get("drawers").isEmpty()).isTrue();
	}

	@Test
	void ticketListKeepsOptionalSeatAsNull() throws Exception {
		UUID cardId = UUID.fromString("e89ed42d-1a89-4eea-8ddc-dca90a5c78c4");
		YearCardListResponse response = new YearCardListResponse(2026, List.of(
			new YearCardListResponse.CardItem(
				cardId,
				DocumentType.TICKET,
				LocalDate.of(2026, 7, 25),
				new YearCardListResponse.TicketFront("흠뻑쇼", "부산아시아드주경기장", null, "/files/cards/front")
			)
		));

		JsonNode json = objectMapper.valueToTree(response);

		assertThat(json.at("/cards/0/documentType").asText()).isEqualTo("TICKET");
		assertThat(json.at("/cards/0/memoryDate").asText()).isEqualTo("2026-07-25");
		assertThat(json.at("/cards/0/front/seat").isNull()).isTrue();
	}

	@Test
	void aiRecallTicketDetailContainsQuestionTextAndNullableAnswer() throws Exception {
		CardDetailResponse response = new CardDetailResponse(
			UUID.fromString("e89ed42d-1a89-4eea-8ddc-dca90a5c78c4"),
			DocumentType.TICKET,
			LocalDate.of(2026, 7, 25),
			new CardDetailResponse.TicketFront("흠뻑쇼", "부산아시아드주경기장", null, "/files/cards/front"),
			new CardDetailResponse.AiRecallTicketBack(
				List.of("현수"),
				"맑음",
				"벅참",
				WritingMode.AI_RECALL,
				TicketSubtype.CONCERT_PERFORMANCE,
				"함께 부른 마지막 앙코르",
				List.of(new CardDetailResponse.TicketAnswer(
					"CONCERT_PERFORMANCE_1",
					"가장 벅찼던 순간은 언제였나요?",
					null
				))
			)
		);

		JsonNode json = objectMapper.valueToTree(response);

		assertThat(json.at("/back/writingMode").asText()).isEqualTo("AI_RECALL");
		assertThat(json.at("/back/ticketSubtype").asText()).isEqualTo("CONCERT_PERFORMANCE");
		assertThat(json.at("/back/memoryText").isMissingNode()).isTrue();
		assertThat(json.at("/back/answers/0/question").asText()).isEqualTo("가장 벅찼던 순간은 언제였나요?");
		assertThat(json.at("/back/answers/0/answer").isNull()).isTrue();
	}

	@Test
	void letterDetailContainsImageModeAndBackPhotoUrls() throws Exception {
		CardDetailResponse response = new CardDetailResponse(
			UUID.fromString("e89ed42d-1a89-4eea-8ddc-dca90a5c78c4"),
			DocumentType.LETTER,
			LocalDate.of(2026, 3, 1),
			new CardDetailResponse.LetterFront(
				"편지 본문", com.memorydrawer.card.FrontImageMode.ORIGINAL, "/files/cards/front"
			),
			new CardDetailResponse.DiaryBack(
				List.of(), "흐림", "감동", "다시 읽어 보았다.", List.of("/files/cards/back/1")
			)
		);

		JsonNode json = objectMapper.valueToTree(response);

		assertThat(json.at("/front/frontImageMode").asText()).isEqualTo("ORIGINAL");
		assertThat(json.at("/back/backPhotoUrls/0").asText()).isEqualTo("/files/cards/back/1");
	}
}
