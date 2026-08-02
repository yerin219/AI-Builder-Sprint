package com.memorydrawer.card.query;

import com.memorydrawer.common.error.ApiException;
import com.memorydrawer.common.error.ErrorCode;

public final class CardNotFoundException extends ApiException {

	public static final String ERROR_CODE = "CARD_002";

	public CardNotFoundException() {
		super(ErrorCode.CARD_002);
	}
}
