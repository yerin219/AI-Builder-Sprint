package com.memorydrawer.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	VALIDATION_001(HttpStatus.BAD_REQUEST, "VALIDATION_001", "요청값을 확인해주세요."),
	AUTH_001(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증 토큰이 없거나 유효하지 않습니다."),
	AUTH_002(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 일치하지 않습니다."),
	AUTH_003(HttpStatus.CONFLICT, "AUTH_003", "이미 사용 중인 이메일입니다."),
	IMAGE_001(HttpStatus.BAD_REQUEST, "IMAGE_001", "이미지 파일을 확인해주세요."),
	IMAGE_002(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_002", "이미지 파일 크기는 10MB 이하여야 합니다."),
	IMAGE_003(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_003", "지원하지 않는 이미지 형식입니다."),
	DOCUMENT_001(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENT_001", "문서를 분석할 수 없습니다."),
	DOCUMENT_002(HttpStatus.BAD_REQUEST, "DOCUMENT_002", "지원하지 않는 문서 유형입니다."),
	DRAFT_001(HttpStatus.NOT_FOUND, "DRAFT_001", "임시 기록을 찾을 수 없습니다."),
	DRAFT_002(HttpStatus.CONFLICT, "DRAFT_002", "현재 단계에서는 요청을 처리할 수 없습니다."),
	DRAFT_004(HttpStatus.FORBIDDEN, "DRAFT_004", "다른 사용자의 임시 기록에는 접근할 수 없습니다."),
	AI_001(HttpStatus.SERVICE_UNAVAILABLE, "AI_001", "문서 분석 서비스에 일시적인 오류가 발생했습니다."),
	AI_002(HttpStatus.SERVICE_UNAVAILABLE, "AI_002", "문서 유형 판단 서비스에 일시적인 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
