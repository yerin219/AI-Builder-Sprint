package com.memorydrawer.memorydraft.domain;

public record ParsedContent(
	String text,
	String html
) {

	public ParsedContent {
		text = text == null ? "" : text.trim();
		html = html == null ? "" : html.trim();
		if (text.isBlank() && html.isBlank()) {
			throw new IllegalArgumentException("parsedContent must contain text or html");
		}
	}

	public String solarInput() {
		return html.isBlank() ? text : html;
	}
}
