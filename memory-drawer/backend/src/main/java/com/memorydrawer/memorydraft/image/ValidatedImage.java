package com.memorydrawer.memorydraft.image;

public record ValidatedImage(
	byte[] bytes,
	String contentType,
	String extension
) {

	public String uploadFilename() {
		return "document." + extension;
	}
}
