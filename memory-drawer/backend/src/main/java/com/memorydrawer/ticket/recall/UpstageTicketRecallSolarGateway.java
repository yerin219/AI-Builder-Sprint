package com.memorydrawer.ticket.recall;

import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.memorydrawer.ai.config.UpstageProperties;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.ticket.recall.TicketRecallAnswerValidator.AnsweredQuestion;

@Component
public final class UpstageTicketRecallSolarGateway implements TicketRecallSolarGateway {

	private static final String SUBTYPE_PROMPT = """
		당신은 기억서랍의 티켓 세부 유형 분류기입니다.
		제공된 Document Parse 결과만 근거로 분류하세요.
		허용 결과는 CONCERT_PERFORMANCE, MOVIE, EXHIBITION, UNKNOWN뿐입니다.
		확신할 수 없으면 UNKNOWN을 선택하고 문서에 없는 사실을 만들지 마세요.
		""";

	private static final String TITLE_PROMPT = """
		당신은 사용자가 작성한 하나 이상, 최대 세 개의 회상 답변을 바탕으로 기억 카드의 한국어 제목 한 줄을 제안합니다.
		비어 있지 않은 모든 답변의 사실과 의미를 빠뜨리지 말고 하나의 기억 장면으로 종합하세요.
		답변을 그대로 나열하거나 이어 붙이는 요약문이 아니라, 답변 사이의 관계를 자연스럽게 연결한 하나의 제목을 만드세요.

		제목 작성 규칙:
		- 공백을 포함해 8~24자를 권장합니다.
		- 모든 비어 있지 않은 답변이 제목의 전체 의미에 기여해야 하며, 특정 답변을 임의로 버리지 마세요.
		- 각 답변의 핵심어를 차례대로 소개하지 말고 하나의 주어·행동·느낌 구조로 재구성하세요.
		- 쉼표, 가운뎃점, 세미콜론, 슬래시나 막대 기호로 여러 답변을 나열하지 마세요.
		- 서로 다른 답변의 문구 두 개 이상을 그대로 이어 붙이지 마세요.
		- 질문 문장을 제목으로 반복하지 마세요.
		- 명사구 또는 짧은 문장 한 줄로 작성하세요.
		- 감성적으로 표현하되 답변에 없는 구체적인 사실, 인물, 장소나 감정을 만들거나 과장하지 마세요.
		- 나쁜 예: 첫 등장, 함께한 떼창, 마지막 곡의 감동
		- 좋은 예: 첫 무대를 함께 노래해 더 벅찼던 순간
		- JSON의 titleCandidate만 반환하세요.
		""";
	private static final String TITLE_RETRY_PROMPT = TITLE_PROMPT + """

		이전 후보가 답변별 핵심어를 구분자로 나열해 거부되었습니다.
		이번에는 이전 후보를 고치는 데 그치지 말고, 원본 답변 전체의 관계를 다시 해석해 하나의 장면으로 재작성하세요.
		구분자를 제거한 단순 연결문도 허용하지 않습니다.
		""";
	private static final String LIST_SEPARATORS = ",，、;；|/·";
	private static final double CLASSIFICATION_TEMPERATURE = 0;
	private static final double TITLE_TEMPERATURE = 0.3;

	private final RestClient restClient;
	private final UpstageProperties properties;
	private final ObjectMapper objectMapper;

	public UpstageTicketRecallSolarGateway(
		RestClient upstageRestClient,
		UpstageProperties properties,
		ObjectMapper objectMapper
	) {
		this.restClient = upstageRestClient;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<TicketSubtype> suggestSubtype(String parsedContent) {
		JsonNode response = callSolar(
			SUBTYPE_PROMPT,
			parsedContent,
			CLASSIFICATION_TEMPERATURE,
			responseFormat(
				"memory_drawer_ticket_subtype",
				"ticketSubtype",
				List.of("CONCERT_PERFORMANCE", "MOVIE", "EXHIBITION", "UNKNOWN")
			)
		);
		String candidate = responseContent(response, "ticketSubtype");
		if (candidate.equals("UNKNOWN")) {
			return Optional.empty();
		}
		try {
			return Optional.of(TicketSubtype.valueOf(candidate));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	@Override
	public String generateTitle(List<AnsweredQuestion> answeredQuestions) {
		ArrayNode answers = objectMapper.createArrayNode();
		answeredQuestions.forEach(answer -> answers.addObject()
			.put("questionId", answer.questionId())
			.put("question", answer.question())
			.put("answer", answer.answer()));
		ObjectNode userInput = objectMapper.createObjectNode();
		userInput.set("answers", answers);

		String candidate = requestTitle(TITLE_PROMPT, userInput);
		if (!isListLikeTitle(candidate)) {
			return candidate;
		}

		ObjectNode retryInput = userInput.deepCopy();
		retryInput.put("rejectedTitle", candidate);
		retryInput.put(
			"retryReason",
			"답변별 핵심어를 나열하지 말고 모든 답변의 의미를 하나의 장면으로 종합해야 합니다."
		);
		String retryCandidate = requestTitle(TITLE_RETRY_PROMPT, retryInput);
		if (isListLikeTitle(retryCandidate)) {
			throw new ApiException(ErrorCode.AI_002);
		}
		return retryCandidate;
	}

	private String requestTitle(String prompt, ObjectNode userInput) {
		JsonNode response = callSolar(
			prompt,
			userInput.toString(),
			TITLE_TEMPERATURE,
			responseFormat(
				"memory_drawer_ticket_title",
				"titleCandidate",
				null
			)
		);
		return responseContent(response, "titleCandidate");
	}

	private boolean isListLikeTitle(String title) {
		return title.chars().anyMatch(character -> LIST_SEPARATORS.indexOf(character) >= 0);
	}

	private JsonNode callSolar(
		String systemPrompt,
		String userContent,
		double temperature,
		ObjectNode responseFormat
	) {
		ObjectNode request = objectMapper.createObjectNode();
		request.put("model", properties.getSolarModel());
		request.put("temperature", temperature);
		ArrayNode messages = request.putArray("messages");
		messages.addObject().put("role", "system").put("content", systemPrompt);
		messages.addObject().put("role", "user").put("content", userContent);
		request.set("response_format", responseFormat);

		try {
			JsonNode response = restClient.post()
				.uri("/v1/chat/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(JsonNode.class);
			if (response == null) {
				throw new ApiException(ErrorCode.AI_002);
			}
			return response;
		} catch (RestClientException exception) {
			throw new ApiException(ErrorCode.AI_002, exception);
		}
	}

	private String responseContent(JsonNode response, String fieldName) {
		String content = response.path("choices").path(0).path("message")
			.path("content").asText("");
		if (content.isBlank()) {
			throw new ApiException(ErrorCode.AI_002);
		}
		try {
			JsonNode field = objectMapper.readTree(content).path(fieldName);
			if (!field.isTextual()) {
				throw new ApiException(ErrorCode.AI_002);
			}
			String value = field.textValue().trim();
			if (value.isBlank()) {
				throw new ApiException(ErrorCode.AI_002);
			}
			return value;
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.AI_002, exception);
		}
	}

	private ObjectNode responseFormat(
		String schemaName,
		String fieldName,
		List<String> allowedValues
	) {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode field = schema.putObject("properties").putObject(fieldName);
		field.put("type", "string");
		if (allowedValues != null) {
			ArrayNode values = field.putArray("enum");
			allowedValues.forEach(values::add);
		}
		schema.putArray("required").add(fieldName);
		schema.put("additionalProperties", false);

		ObjectNode jsonSchema = objectMapper.createObjectNode();
		jsonSchema.put("name", schemaName);
		jsonSchema.put("strict", true);
		jsonSchema.set("schema", schema);

		ObjectNode format = objectMapper.createObjectNode();
		format.put("type", "json_schema");
		format.set("json_schema", jsonSchema);
		return format;
	}
}
