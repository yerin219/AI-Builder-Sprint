package com.memorydrawer.ai.documentparse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

import com.memorydrawer.ai.config.UpstageProperties;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.image.ValidatedImage;

class DocumentParseClientTests {

	private MockRestServiceServer server;
	private DocumentParseClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upstage.ai");
		server = MockRestServiceServer.bindTo(builder).build();

		UpstageProperties properties = new UpstageProperties();
		properties.setApiKey("test-placeholder");
		properties.setBaseUrl("https://api.upstage.ai");
		properties.setDocumentParseModel("document-parse");
		properties.setConnectTimeout(Duration.ofSeconds(1));
		properties.setRequestTimeout(Duration.ofSeconds(1));
		client = new DocumentParseClient(builder.build(), properties);
	}

	@Test
	void sendsDocumentParseMultipartRequestWithoutReactiveStreams() {
		server.expect(requestTo("https://api.upstage.ai/v1/document-digitization"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(containsString("name=\"document\"")))
			.andExpect(content().string(containsString("filename=\"document.jpg\"")))
			.andExpect(content().string(containsString("Content-Type: image/jpeg")))
			.andExpect(content().string(containsString("name=\"model\"")))
			.andExpect(content().string(containsString("document-parse")))
			.andExpect(content().string(containsString("name=\"ocr\"")))
			.andExpect(content().string(containsString("force")))
			.andRespond(withSuccess(
				"""
					{
					  "content": {
					    "text": "ticket content",
					    "html": "<p>ticket content</p>"
					  }
					}
					""",
				MediaType.APPLICATION_JSON
			));

		ParsedContent result = client.parse(
			new ValidatedImage(
				new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF},
				"image/jpeg",
				"jpg"
			)
		);

		assertThat(result.text()).isEqualTo("ticket content");
		assertThat(result.html()).isEqualTo("<p>ticket content</p>");
		server.verify();
	}
}
