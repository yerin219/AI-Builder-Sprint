package com.memorydrawer.ticket.recall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.ai.config.UpstageProperties;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.ticket.recall.TicketRecallAnswerValidator.AnsweredQuestion;

class UpstageTicketRecallSolarGatewayTests {

	private MockRestServiceServer server;
	private ObjectMapper objectMapper;
	private UpstageTicketRecallSolarGateway gateway;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upstage.ai");
		server = MockRestServiceServer.bindTo(builder).build();
		objectMapper = new ObjectMapper();
		UpstageProperties properties = new UpstageProperties();
		properties.setSolarModel("solar-pro3");
		gateway = new UpstageTicketRecallSolarGateway(
			builder.build(),
			properties,
			objectMapper
		);
	}

	@Test
	void acceptsTextualStructuredOutputField() {
		JsonNode content = objectMapper.createObjectNode()
			.put("titleCandidate", "함께 본 영화");
		server.expect(requestTo("https://api.upstage.ai/v1/chat/completions"))
			.andRespond(withSuccess(solarResponse(content), MediaType.APPLICATION_JSON));

		String result = gateway.generateTitle(answeredQuestions());

		assertThat(result).isEqualTo("함께 본 영화");
		server.verify();
	}

	@Test
	void rejectsNonTextualStructuredOutputField() {
		JsonNode content = objectMapper.createObjectNode().put("titleCandidate", 123);
		server.expect(requestTo("https://api.upstage.ai/v1/chat/completions"))
			.andRespond(withSuccess(solarResponse(content), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> gateway.generateTitle(answeredQuestions()))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_002)
			);
		server.verify();
	}

	private List<AnsweredQuestion> answeredQuestions() {
		return List.of(new AnsweredQuestion(
			"MOVIE_1",
			"어떤 계기로 이 영화를 봤나요?",
			"친구가 추천해서 봤어요."
		));
	}

	private String solarResponse(JsonNode content) {
		var response = objectMapper.createObjectNode();
		response.putArray("choices")
			.addObject()
			.putObject("message")
			.put("content", content.toString());
		return response.toString();
	}
}
