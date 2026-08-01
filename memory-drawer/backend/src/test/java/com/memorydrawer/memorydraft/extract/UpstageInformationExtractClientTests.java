package com.memorydrawer.memorydraft.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorydrawer.ai.config.UpstageProperties;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.receipt.PurchaseItem;

class UpstageInformationExtractClientTests {

	private MockRestServiceServer server;
	private UpstageInformationExtractClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upstage.ai");
		server = MockRestServiceServer.bindTo(builder).build();

		UpstageProperties properties = new UpstageProperties();
		properties.setApiKey("test-placeholder");
		properties.setBaseUrl("https://api.upstage.ai");
		properties.setInformationExtractModel("information-extract");
		properties.setConnectTimeout(Duration.ofSeconds(1));
		properties.setRequestTimeout(Duration.ofSeconds(1));
		client = new UpstageInformationExtractClient(
			builder.build(),
			properties,
			new ObjectMapper()
		);
	}

	@Test
	void sendsOriginalImageAndReceiptSchemaToInformationExtract() {
		server.expect(requestTo(
				"https://api.upstage.ai/v1/information-extraction/chat/completions"
			))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(containsString("\"model\":\"information-extract\"")))
			.andExpect(content().string(containsString("\"memoryDate\"")))
			.andExpect(content().string(containsString("\"storeName\"")))
			.andExpect(content().string(containsString("\"purchaseItems\"")))
			.andExpect(content().string(containsString("\"type\":\"array\"")))
			.andExpect(content().string(containsString("\"quantity\"")))
			.andExpect(content().string(containsString("data:image/jpeg;base64,/9j/")))
			.andExpect(content().string(not(containsString("\"type\":\"text\""))))
			.andExpect(content().string(containsString("\"type\":\"string\"")))
			.andExpect(content().string(not(containsString("[\"string\",\"null\"]"))))
			.andRespond(withSuccess(
				"""
					{
					  "choices": [
					    {
					      "message": {
					        "content": "{\\"memoryDate\\":\\"2026-07-25\\",\\"storeName\\":\\"서면카페\\",\\"purchaseItems\\":[{\\"name\\":\\"아이스 아메리카노\\",\\"quantity\\":2},{\\"name\\":\\"치즈케이크\\",\\"quantity\\":1}]}"
					      }
					    }
					  ]
					}
					""",
				MediaType.APPLICATION_JSON
			));

		ExtractedFrontFields result = client.extract(
			DocumentType.RECEIPT,
			new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF},
			"image/jpeg"
		);

		assertThat(result.memoryDate()).isEqualTo("2026-07-25");
		assertThat(result.storeName()).isEqualTo("서면카페");
		assertThat(result.purchaseItems()).containsExactly(
			new PurchaseItem("아이스 아메리카노", 2),
			new PurchaseItem("치즈케이크", 1)
		);
		assertThat(result.eventName()).isNull();
		server.verify();
	}

	@Test
	void mapsMalformedInformationExtractResponseToAiError() {
		server.expect(requestTo(
				"https://api.upstage.ai/v1/information-extraction/chat/completions"
			))
			.andRespond(withSuccess(
				"""
					{
					  "choices": [
					    {
					      "message": {
					        "content": "not-json"
					      }
					    }
					  ]
					}
					""",
				MediaType.APPLICATION_JSON
			));

		assertThatThrownBy(() -> client.extract(
			DocumentType.TICKET,
			new byte[] {1},
			"image/png"
		))
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_001)
			);
		server.verify();
	}
}
