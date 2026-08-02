package com.memorydrawer.card.image;

import org.springframework.http.MediaType;

public record CardImageResource(
	byte[] bytes,
	MediaType mediaType
) {
	public CardImageResource {
		bytes = bytes.clone();
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}
}
