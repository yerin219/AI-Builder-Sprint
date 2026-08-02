package com.memorydrawer.card.create.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.card.DocumentType;

class CardCreateResponseTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void savedResponseUsesMemoryYearAndSavedStatus() {
		var response = CardCreateResponse.saved(
			UUID.fromString("e89ed42d-1a89-4eea-8ddc-dca90a5c78c4"),
			DocumentType.TICKET,
			LocalDate.of(2026, 7, 25)
		);

		JsonNode json = objectMapper.valueToTree(response);

		assertThat(json.get("memoryDate").asText()).isEqualTo("2026-07-25");
		assertThat(json.get("printLayout").asText()).isEqualTo("LANDSCAPE_TICKET");
		assertThat(json.get("year").asInt()).isEqualTo(2026);
		assertThat(json.get("draftStatus").asText()).isEqualTo("SAVED");
	}
}
