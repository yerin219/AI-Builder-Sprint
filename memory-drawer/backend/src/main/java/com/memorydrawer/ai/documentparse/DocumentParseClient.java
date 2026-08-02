package com.memorydrawer.ai.documentparse;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.memorydrawer.ai.config.UpstageProperties;
import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;
import com.memorydrawer.memorydraft.domain.ParsedContent;
import com.memorydrawer.memorydraft.image.ValidatedImage;

@Component
public class DocumentParseClient {

	private final RestClient restClient;
	private final UpstageProperties properties;

	public DocumentParseClient(RestClient upstageRestClient, UpstageProperties properties) {
		this.restClient = upstageRestClient;
		this.properties = properties;
	}

	public ParsedContent parse(ValidatedImage image) {
		JsonNode response = callDocumentParse(image);
		return normalize(response);
	}

	private JsonNode callDocumentParse(ValidatedImage image) {
		HttpHeaders documentHeaders = new HttpHeaders();
		documentHeaders.setContentType(MediaType.parseMediaType(image.contentType()));

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add(
			"document",
			new HttpEntity<>(
				new NamedByteArrayResource(image.bytes(), image.uploadFilename()),
				documentHeaders
			)
		);
		body.add("model", properties.getDocumentParseModel());
		body.add("ocr", "force");

		try {
			return restClient.post()
				.uri("/v1/document-digitization")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(JsonNode.class);
		} catch (RestClientException exception) {
			throw new ApiException(ErrorCode.AI_001, exception);
		}
	}

	private ParsedContent normalize(JsonNode response) {
		if (response == null || response.isNull()) {
			throw new ApiException(ErrorCode.DOCUMENT_001);
		}

		JsonNode content = response.path("content");
		String html = content.path("html").asText("").trim();
		String text = content.path("text").asText("").trim();

		if (content.isTextual() && text.isBlank()) {
			text = content.asText("").trim();
		}
		if (html.isBlank()) {
			html = collectElementContent(response.path("elements"), "html");
		}
		if (text.isBlank()) {
			text = collectElementContent(response.path("elements"), "text");
		}
		if (text.isBlank() && !html.isBlank()) {
			text = htmlToText(html);
		}
		if (text.isBlank() && html.isBlank()) {
			throw new ApiException(ErrorCode.DOCUMENT_001);
		}

		return new ParsedContent(text, html);
	}

	private String collectElementContent(JsonNode elements, String fieldName) {
		if (!elements.isArray()) {
			return "";
		}

		List<String> values = new ArrayList<>();
		for (JsonNode element : elements) {
			JsonNode elementContent = element.path("content");
			String value = elementContent.path(fieldName).asText("").trim();
			if (!value.isBlank()) {
				values.add(value);
			}
		}
		return String.join("\n", values);
	}

	private String htmlToText(String html) {
		return html
			.replaceAll("(?i)<br\\s*/?>", "\n")
			.replaceAll("(?i)</(p|div|tr|li|h[1-6])>", "\n")
			.replaceAll("<[^>]+>", " ")
			.replace("&nbsp;", " ")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&#39;", "'")
			.replace("&amp;", "&")
			.replaceAll("[\\t\\x0B\\f\\r ]+", " ")
			.replaceAll("\\n\\s*\\n+", "\n")
			.trim();
	}

	private static final class NamedByteArrayResource extends ByteArrayResource {

		private final String filename;

		private NamedByteArrayResource(byte[] byteArray, String filename) {
			super(byteArray);
			this.filename = filename;
		}

		@Override
		public String getFilename() {
			return filename;
		}
	}
}
