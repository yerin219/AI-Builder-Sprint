package com.memorydrawer.memorydraft.domain;

public enum DocumentType {

	RECEIPT("영수증"),
	TICKET("티켓"),
	LETTER("손편지");

	private final String label;

	DocumentType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
