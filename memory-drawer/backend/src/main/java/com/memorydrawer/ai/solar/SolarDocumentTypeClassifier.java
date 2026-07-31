package com.memorydrawer.ai.solar;

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
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.ParsedContent;

@Component
public class SolarDocumentTypeClassifier implements DocumentTypeClassifier {

	private static final String SYSTEM_PROMPT = """
		당신은 기억서랍의 문서 유형 분류기입니다.
		제공된 Document Parse 결과만 근거로 문서를 분류하세요.
		허용 결과는 RECEIPT, TICKET, LETTER, UNKNOWN뿐입니다.
		확신할 수 없거나 여러 유형이 섞여 있으면 UNKNOWN을 선택하세요.
		사용자 기억이나 문서에 없는 사실을 만들지 마세요.
		""";

	private final RestClient restClient;
	private final UpstageProperties properties;
	private final ObjectMapper objectMapper;

	public SolarDocumentTypeClassifier(
		RestClient upstageRestClient,
		UpstageProperties properties,
		ObjectMapper objectMapper
	) {
		this.restClient = upstageRestClient;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public DocumentType classify(ParsedContent parsedContent) {
		JsonNode response = callSolar(parsedContent);
		String content = response.path("choices").path(0).path("message").path("content").asText("");
		if (content.isBlank()) {
			throw new ApiException(ErrorCode.AI_002);
		}

		try {
			String candidate = objectMapper.readTree(content).path("documentType").asText("");
			if ("UNKNOWN".equals(candidate)) {
				return null;
			}
			try {
				return DocumentType.valueOf(candidate);
			} catch (IllegalArgumentException exception) {
				return null;
			}
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.AI_002, exception);
		}
	}

	private JsonNode callSolar(ParsedContent parsedContent) {
		ObjectNode request = objectMapper.createObjectNode();
		request.put("model", properties.getSolarModel());
		request.put("temperature", 0);
		request.set("messages", messages(parsedContent));
		request.set("response_format", responseFormat());

		try {
			return restClient.post()
				.uri("/v1/chat/completions")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(JsonNode.class);
		} catch (RestClientException exception) {
			throw new ApiException(ErrorCode.AI_002, exception);
		}
	}

	private ArrayNode messages(ParsedContent parsedContent) {
		ArrayNode messages = objectMapper.createArrayNode();
		messages.addObject()
			.put("role", "system")
			.put("content", SYSTEM_PROMPT);
		messages.addObject()
			.put("role", "user")
			.put("content", parsedContent.solarInput());
		return messages;
	}

	private ObjectNode responseFormat() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode propertiesNode = schema.putObject("properties");
		propertiesNode.putObject("documentType")
			.put("type", "string")
			.set("enum", objectMapper.createArrayNode()
				.add("RECEIPT")
				.add("TICKET")
				.add("LETTER")
				.add("UNKNOWN"));
		schema.set("required", objectMapper.createArrayNode().add("documentType"));
		schema.put("additionalProperties", false);

		ObjectNode jsonSchema = objectMapper.createObjectNode();
		jsonSchema.put("name", "memory_drawer_document_type");
		jsonSchema.put("strict", true);
		jsonSchema.set("schema", schema);

		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", "json_schema");
		responseFormat.set("json_schema", jsonSchema);
		return responseFormat;
	}
}
