package com.memorydrawer.memorydraft.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

class ImageFileValidatorTests {

	private final ImageFileValidator validator = new ImageFileValidator();

	@Test
	void acceptsJpegPngAndWebpSignatures() {
		assertThat(validator.validate(file("image/jpeg", jpeg())).extension()).isEqualTo("jpg");
		assertThat(validator.validate(file("image/png", png())).extension()).isEqualTo("png");
		assertThat(validator.validate(file("image/webp", webp())).extension()).isEqualTo("webp");
	}

	@Test
	void rejectsEmptyImage() {
		assertError(
			() -> validator.validate(new MockMultipartFile("image", new byte[0])),
			ErrorCode.IMAGE_001
		);
	}

	@Test
	void rejectsUnsupportedOrDisguisedImage() {
		assertError(
			() -> validator.validate(file("image/png", jpeg())),
			ErrorCode.IMAGE_003
		);
	}

	@Test
	void rejectsImageLargerThanTenMegabytes() {
		byte[] bytes = new byte[(int)ImageFileValidator.MAX_FILE_SIZE + 1];
		bytes[0] = (byte)0xff;
		bytes[1] = (byte)0xd8;
		bytes[2] = (byte)0xff;

		assertError(
			() -> validator.validate(file("image/jpeg", bytes)),
			ErrorCode.IMAGE_002
		);
	}

	private MockMultipartFile file(String contentType, byte[] bytes) {
		return new MockMultipartFile("image", "document", contentType, bytes);
	}

	private byte[] jpeg() {
		return new byte[] {(byte)0xff, (byte)0xd8, (byte)0xff, 0x00};
	}

	private byte[] png() {
		return new byte[] {
			(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00
		};
	}

	private byte[] webp() {
		return new byte[] {
			'R', 'I', 'F', 'F', 0x04, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'
		};
	}

	private void assertError(Runnable action, ErrorCode expected) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(ApiException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(expected)
			);
	}
}
