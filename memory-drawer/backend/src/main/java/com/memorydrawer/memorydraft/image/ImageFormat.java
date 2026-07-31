package com.memorydrawer.memorydraft.image;

import java.nio.charset.StandardCharsets;
import java.util.Set;

enum ImageFormat {

	JPEG("jpg", "image/jpeg", Set.of("image/jpeg", "image/jpg")) {
		@Override
		boolean matches(byte[] bytes) {
			return bytes.length >= 3
				&& unsigned(bytes[0]) == 0xff
				&& unsigned(bytes[1]) == 0xd8
				&& unsigned(bytes[2]) == 0xff;
		}
	},
	PNG("png", "image/png", Set.of("image/png")) {
		private final byte[] signature = new byte[] {
			(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
		};

		@Override
		boolean matches(byte[] bytes) {
			if (bytes.length < signature.length) {
				return false;
			}
			for (int index = 0; index < signature.length; index++) {
				if (bytes[index] != signature[index]) {
					return false;
				}
			}
			return true;
		}
	},
	WEBP("webp", "image/webp", Set.of("image/webp")) {
		@Override
		boolean matches(byte[] bytes) {
			return bytes.length >= 12
				&& ascii(bytes, 0, 4).equals("RIFF")
				&& ascii(bytes, 8, 4).equals("WEBP");
		}
	};

	private final String extension;
	private final String canonicalContentType;
	private final Set<String> acceptedContentTypes;

	ImageFormat(String extension, String canonicalContentType, Set<String> acceptedContentTypes) {
		this.extension = extension;
		this.canonicalContentType = canonicalContentType;
		this.acceptedContentTypes = acceptedContentTypes;
	}

	abstract boolean matches(byte[] bytes);

	boolean acceptsContentType(String contentType) {
		return contentType != null && acceptedContentTypes.contains(contentType.toLowerCase());
	}

	String extension() {
		return extension;
	}

	String canonicalContentType() {
		return canonicalContentType;
	}

	static int unsigned(byte value) {
		return value & 0xff;
	}

	static String ascii(byte[] bytes, int offset, int length) {
		return new String(bytes, offset, length, StandardCharsets.US_ASCII);
	}
}
