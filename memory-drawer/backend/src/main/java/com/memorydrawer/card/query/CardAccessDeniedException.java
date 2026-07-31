package com.memorydrawer.card.query;

import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

public final class CardAccessDeniedException extends ApiException {

	public static final String ERROR_CODE = "CARD_001";

	public CardAccessDeniedException() {
		super(ErrorCode.CARD_001);
	}
}
