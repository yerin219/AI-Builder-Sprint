package com.memorydrawer.memorydraft.extract;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.receipt.PurchaseItem;

@Component
public class UpstageInformationExtractClient implements InformationExtractClient {

	private final RestClient restClient;
	private final UpstageProperties properties;
	private final ObjectMapper objectMapper;

	public UpstageInformationExtractClient(
		RestClient upstageRestClient,
		UpstageProperties properties,
		ObjectMapper objectMapper
	) {
		this.restClient = upstageRestClient;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public ExtractedFrontFields extract(
		DocumentType documentType,
		byte[] imageBytes,
		String contentType
	) {
		if (documentType != DocumentType.RECEIPT && documentType != DocumentType.TICKET) {
			throw new IllegalArgumentException("Information Extract는 영수증과 티켓에만 사용할 수 있습니다.");
		}

		JsonNode response = callInformationExtract(
			documentType,
			imageBytes,
			contentType
		);
		return normalizeResponse(documentType, response);
	}

	private JsonNode callInformationExtract(
		DocumentType documentType,
		byte[] imageBytes,
		String contentType
	) {
		ObjectNode request = objectMapper.createObjectNode();
		request.put("model", properties.getInformationExtractModel());
		request.set("messages", messages(imageBytes, contentType));
		request.set("response_format", responseFormat(documentType));

		try {
			return restClient.post()
				.uri("/v1/information-extraction/chat/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(JsonNode.class);
		} catch (RestClientException exception) {
			throw new ApiException(ErrorCode.AI_001, exception);
		}
	}

	private ArrayNode messages(byte[] imageBytes, String contentType) {
		ArrayNode content = objectMapper.createArrayNode();
		content.addObject()
			.put("type", "image_url")
			.putObject("image_url")
			.put("url", dataUri(imageBytes, contentType));

		ArrayNode messages = objectMapper.createArrayNode();
		messages.addObject()
			.put("role", "user")
			.set("content", content);
		return messages;
	}

	private ObjectNode responseFormat(DocumentType documentType) {
		List<FieldDefinition> fields = documentType == DocumentType.RECEIPT
			? List.of(
				new FieldDefinition("memoryDate", "문서에 명확히 표시된 날짜. YYYY-MM-DD 형식"),
				new FieldDefinition("storeName", "영수증의 가게 또는 상호명")
			)
			: List.of(
				new FieldDefinition("memoryDate", "문서에 명확히 표시된 행사 날짜. YYYY-MM-DD 형식"),
				new FieldDefinition("eventName", "티켓의 행사, 공연, 영화 또는 전시 이름"),
				new FieldDefinition("venue", "행사가 열린 장소"),
				new FieldDefinition("seat", "티켓에 표시된 좌석")
			);

		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		ObjectNode propertiesNode = schema.putObject("properties");
		ArrayNode required = objectMapper.createArrayNode();
		for (FieldDefinition field : fields) {
			ObjectNode fieldSchema = propertiesNode.putObject(field.name());
			fieldSchema.put("type", "string");
			fieldSchema.put(
				"description",
				field.description() + ". 확신할 수 없으면 빈 문자열"
			);
			required.add(field.name());
		}
		if (documentType == DocumentType.RECEIPT) {
			addPurchaseItemsSchema(propertiesNode, required);
		}
		schema.set("required", required);
		schema.put("additionalProperties", false);

		ObjectNode jsonSchema = objectMapper.createObjectNode();
		jsonSchema.put(
			"name",
			documentType == DocumentType.RECEIPT
				? "memory_drawer_receipt_front"
				: "memory_drawer_ticket_front"
		);
		jsonSchema.put("strict", true);
		jsonSchema.set("schema", schema);

		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", "json_schema");
		responseFormat.set("json_schema", jsonSchema);
		return responseFormat;
	}

	private void addPurchaseItemsSchema(ObjectNode propertiesNode, ArrayNode required) {
		ObjectNode purchaseItems = propertiesNode.putObject("purchaseItems");
		purchaseItems.put("type", "array");
		purchaseItems.put(
			"description",
			"실제로 구매하거나 주문한 메뉴·상품만 추출합니다. 소계, 합계, 부가세, 할인, "
				+ "쿠폰, 포인트, 결제수단, 카드번호, 승인번호, 주문번호, 사업자정보, 광고 문구는 "
				+ "제외합니다. 카페·음식점의 ICE, 샷 추가, 포장, 사이즈업 같은 옵션은 제외하고, "
				+ "마트 영수증은 실제 상품명은 남기되 할인 행은 제외합니다."
		);

		ObjectNode itemSchema = purchaseItems.putObject("items");
		itemSchema.put("type", "object");
		ObjectNode itemProperties = itemSchema.putObject("properties");
		itemProperties.putObject("name")
			.put("type", "string")
			.put("description", "실제로 구매하거나 주문한 메뉴 또는 상품명");
		itemProperties.putObject("quantity")
			.put("type", "integer")
			.put("minimum", 1)
			.put("description", "구매 수량. 별도 수량 표시가 없으면 1");
		itemSchema.putArray("required").add("name").add("quantity");
		itemSchema.put("additionalProperties", false);
		required.add("purchaseItems");
	}

	private ExtractedFrontFields normalizeResponse(
		DocumentType documentType,
		JsonNode response
	) {
		String content = response == null
			? ""
			: response.path("choices").path(0).path("message").path("content").asText("");
		if (content.isBlank()) {
			throw new ApiException(ErrorCode.AI_001);
		}

		try {
			JsonNode result = objectMapper.readTree(content);
			return documentType == DocumentType.RECEIPT
				? new ExtractedFrontFields(
					textOrNull(result, "memoryDate"),
					textOrNull(result, "storeName"),
					purchaseItemsOrEmpty(result),
					null,
					null,
					null
				)
				: new ExtractedFrontFields(
					textOrNull(result, "memoryDate"),
					null,
					List.of(),
					textOrNull(result, "eventName"),
					textOrNull(result, "venue"),
					textOrNull(result, "seat")
				);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.AI_001, exception);
		}
	}

	private List<PurchaseItem> purchaseItemsOrEmpty(JsonNode result) {
		JsonNode items = result.path("purchaseItems");
		if (!items.isArray()) {
			return List.of();
		}

		List<PurchaseItem> purchaseItems = new ArrayList<>();
		for (JsonNode item : items) {
			String name = textOrNull(item, "name");
			JsonNode quantity = item.path("quantity");
			if (name == null || !quantity.isIntegralNumber() || !quantity.canConvertToInt()
				|| quantity.intValue() < 1) {
				continue;
			}
			purchaseItems.add(new PurchaseItem(name, quantity.intValue()));
		}
		return List.copyOf(purchaseItems);
	}

	private String textOrNull(JsonNode result, String fieldName) {
		JsonNode value = result.path(fieldName);
		if (!value.isTextual()) {
			return null;
		}
		String text = value.asText().trim();
		return text.isBlank() ? null : text;
	}

	private String dataUri(byte[] imageBytes, String contentType) {
		return "data:%s;base64,%s".formatted(
			contentType,
			Base64.getEncoder().encodeToString(imageBytes)
		);
	}

	private record FieldDefinition(String name, String description) {
	}
}
