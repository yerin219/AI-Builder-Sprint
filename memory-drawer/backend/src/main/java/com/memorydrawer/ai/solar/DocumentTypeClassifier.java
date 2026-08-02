package com.memorydrawer.ai.solar;

import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.ParsedContent;

public interface DocumentTypeClassifier {

	DocumentType classify(ParsedContent parsedContent);
}
