package com.memorydrawer.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void mapsMissingCardJsonPartToValidationError() {
		var response = handler.handleMissingRequestPart(
			new MissingServletRequestPartException("card")
		);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("VALIDATION_001");
	}

	@Test
	void keepsMissingImagePartAsImageError() {
		var response = handler.handleMissingRequestPart(
			new MissingServletRequestPartException("image")
		);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("IMAGE_001");
	}
}
