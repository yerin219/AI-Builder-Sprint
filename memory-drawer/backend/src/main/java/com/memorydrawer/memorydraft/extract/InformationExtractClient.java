package com.memorydrawer.memorydraft.extract;

import com.memorydrawer.memorydraft.domain.DocumentType;

public interface InformationExtractClient {

	ExtractedFrontFields extract(
		DocumentType documentType,
		byte[] imageBytes,
		String contentType
	);
}
