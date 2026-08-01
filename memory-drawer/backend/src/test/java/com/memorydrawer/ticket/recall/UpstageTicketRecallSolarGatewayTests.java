package com.memorydrawer.ticket.recall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
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
			.andExpect(jsonPath("$.temperature").value(0.3))
			.andExpect(jsonPath("$.messages[0].content", containsString("답변을 그대로 나열하거나 이어 붙이는 요약문이 아니라")))
			.andExpect(jsonPath("$.messages[0].content", containsString("모든 비어 있지 않은 답변")))
			.andExpect(jsonPath("$.messages[0].content", containsString("8~24자")))
			.andRespond(withSuccess(solarResponse(content), MediaType.APPLICATION_JSON));

		String result = gateway.generateTitle(answeredQuestions());

		assertThat(result).isEqualTo("함께 본 영화");
		server.verify();
	}

	@Test
	void retriesListLikeTitleAndReturnsSynthesizedCandidate() {
		JsonNode listedContent = objectMapper.createObjectNode()
			.put("titleCandidate", "블랙핑크 첫 등장, TTL 떼창, Forever Young 감동");
		JsonNode synthesizedContent = objectMapper.createObjectNode()
			.put("titleCandidate", "블랙핑크와 떼창으로 벅찼던 순간");
		server.expect(requestTo("https://api.upstage.ai/v1/chat/completions"))
			.andRespond(withSuccess(solarResponse(listedContent), MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://api.upstage.ai/v1/chat/completions"))
			.andExpect(jsonPath("$.messages[0].content", containsString("이전 후보가 답변별 핵심어")))
			.andExpect(jsonPath("$.messages[1].content", containsString("rejectedTitle")))
			.andExpect(jsonPath("$.messages[1].content", containsString("모든 답변의 의미")))
			.andRespond(withSuccess(solarResponse(synthesizedContent), MediaType.APPLICATION_JSON));

		String result = gateway.generateTitle(concertAnsweredQuestions());

		assertThat(result).isEqualTo("블랙핑크와 떼창으로 벅찼던 순간");
		server.verify();
	}

	@Test
	void rejectsListLikeTitleWhenRetryStillListsAnswers() {
		JsonNode listedContent = objectMapper.createObjectNode()
			.put("titleCandidate", "첫 등장, 떼창, 감동");
		server.expect(requestTo("https://api.upstage.ai/v1/chat/completions"))
			.andRespond(withSuccess(solarResponse(listedContent), MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://api.upstage.ai/v1/chat/completions"))
			.andRespond(withSuccess(solarResponse(listedContent), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> gateway.generateTitle(concertAnsweredQuestions()))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_002)
			);
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

	private List<AnsweredQuestion> concertAnsweredQuestions() {
		return List.of(
			new AnsweredQuestion(
				"CONCERT_PERFORMANCE_1",
				"가장 벅찼던 순간은 언제였나요?",
				"블랙핑크가 처음 등장했을 때였어요."
			),
			new AnsweredQuestion(
				"CONCERT_PERFORMANCE_2",
				"그날 떼창하거나 함성을 질렀던 순간이 있나요?",
				"TTL을 함께 떼창했어요."
			),
			new AnsweredQuestion(
				"CONCERT_PERFORMANCE_3",
				"어떤 곡에서 마음이 가장 크게 움직였나요?",
				"Forever Young에서 가장 감동했어요."
			)
		);
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
