package com.memorydrawer.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode));
	}

	@ExceptionHandler({
		MethodArgumentNotValidException.class,
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ApiErrorResponse> handleValidationException() {
		return response(ErrorCode.VALIDATION_001);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded() {
		return response(ErrorCode.IMAGE_002);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingRequestPart(
		MissingServletRequestPartException exception
	) {
		ErrorCode errorCode = "card".equals(exception.getRequestPartName())
			? ErrorCode.VALIDATION_001
			: ErrorCode.IMAGE_001;
		return response(errorCode);
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ApiErrorResponse> handleMultipartException() {
		return response(ErrorCode.IMAGE_001);
	}

	private ResponseEntity<ApiErrorResponse> response(ErrorCode errorCode) {
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiErrorResponse.from(errorCode));
	}
}
