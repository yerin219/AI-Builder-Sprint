package com.memorydrawer.common.error;

public record ApiErrorResponse(
	boolean success,
	String code,
	String message,
	Object data
) {

	public static ApiErrorResponse from(ErrorCode errorCode) {
		return new ApiErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), null);
	}
}
