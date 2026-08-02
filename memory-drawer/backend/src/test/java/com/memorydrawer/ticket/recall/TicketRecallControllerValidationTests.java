package com.memorydrawer.ticket.recall;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.auth.AuthenticatedUserIdResolver;
import com.memorydrawer.common.error.GlobalExceptionHandler;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

class TicketRecallControllerValidationTests {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private MemoryDraftRepository memoryDraftRepository;
	private UUID ownerId;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().findAndRegisterModules();
		memoryDraftRepository = mock(MemoryDraftRepository.class);
		TicketRecallSolarGateway solarGateway = mock(TicketRecallSolarGateway.class);
		TicketRecallService ticketRecallService = new TicketRecallService(solarGateway);
		TicketRecallApplicationService applicationService = new TicketRecallApplicationService(
			memoryDraftRepository,
			ticketRecallService,
			objectMapper
		);
		TicketRecallController controller = new TicketRecallController(
			new AuthenticatedUserIdResolver(),
			applicationService
		);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
		ownerId = UUID.randomUUID();
	}

	@Test
	void returnsTicket001ForBlankSubtypeInsteadOfGenericValidationError() throws Exception {
		MemoryDraft draft = frontConfirmedTicket(false);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		mockMvc.perform(post(
				"/memory-drafts/{draftId}/ticket-recall/questions",
				draft.getId()
			)
				.principal(() -> ownerId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "ticketSubtype": " "
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("TICKET_001"))
			.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@Test
	void returnsTicket003WhenAnswersAreNull() throws Exception {
		MemoryDraft draft = frontConfirmedTicket(true);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		mockMvc.perform(post(
				"/memory-drafts/{draftId}/ticket-recall/title",
				draft.getId()
			)
				.principal(() -> ownerId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "answers": null
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("TICKET_003"))
			.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@Test
	void returnsTicket003WhenAnswerCountIsNotThree() throws Exception {
		MemoryDraft draft = frontConfirmedTicket(true);
		when(memoryDraftRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

		mockMvc.perform(post(
				"/memory-drafts/{draftId}/ticket-recall/title",
				draft.getId()
			)
				.principal(() -> ownerId.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "answers": [
					    {"questionId": "MOVIE_1", "answer": "친구가 추천했어요."},
					    {"questionId": "MOVIE_2", "answer": null}
					  ]
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("TICKET_003"))
			.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@Test
	void returnsCommonValidationEnvelopeForMalformedDraftId() throws Exception {
		mockMvc.perform(post(
				"/memory-drafts/not-a-uuid/ticket-recall/subtype-suggestion"
			)
				.principal(() -> ownerId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("VALIDATION_001"))
			.andExpect(jsonPath("$.message").value("요청값을 확인해주세요."))
			.andExpect(jsonPath("$.data").value(nullValue()));
	}

	private MemoryDraft frontConfirmedTicket(boolean confirmSubtype) throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		MemoryDraft draft = MemoryDraft.analyzed(
			UUID.randomUUID(),
			ownerId,
			"drafts/%s/original.jpg".formatted(UUID.randomUUID()),
			"image/jpeg",
			objectMapper.writeValueAsString(new ParsedContent("ticket content", "")),
			DocumentType.TICKET,
			now,
			now.plus(7, ChronoUnit.DAYS)
		);
		draft.confirmDocumentType(DocumentType.TICKET, "{}");
		draft.confirmFront("""
			{
			  "memoryDate": "2026-07-25",
			  "front": {
			    "eventName": "공연",
			    "venue": "공연장",
			    "seat": null
			  }
			}
			""");
		if (confirmSubtype) {
			draft.confirmTicketSubtype(TicketSubtype.MOVIE);
		}
		return draft;
	}
}
