package com.memorydrawer.memorydraft.image;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

@Component
public class ImageFileValidator {

	static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

	public ValidatedImage validate(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new ApiException(ErrorCode.IMAGE_001);
		}
		if (image.getSize() > MAX_FILE_SIZE) {
			throw new ApiException(ErrorCode.IMAGE_002);
		}

		byte[] bytes = readBytes(image);
		ImageFormat format = Arrays.stream(ImageFormat.values())
			.filter(candidate -> candidate.matches(bytes))
			.filter(candidate -> candidate.acceptsContentType(image.getContentType()))
			.findFirst()
			.orElseThrow(() -> new ApiException(ErrorCode.IMAGE_003));

		return new ValidatedImage(bytes, format.canonicalContentType(), format.extension());
	}

	private byte[] readBytes(MultipartFile image) {
		try {
			return image.getBytes();
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.IMAGE_001, exception);
		}
	}
}
