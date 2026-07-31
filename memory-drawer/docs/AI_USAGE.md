# 기억서랍 AI 연동 기준

## API 3 이미지 분석

`POST /memory-drafts/analyze`는 다음 순서로 처리합니다.

1. 인증된 사용자의 JPEG, PNG 또는 WebP 이미지를 최대 10MB까지 받습니다.
2. 원본 이미지를 Upstage Document Parse의 `document-parse` 모델에 한 번만 전달합니다.
3. 유효한 전체 본문과 HTML 구조를 내부 `parsedContent`로 정규화합니다.
4. 정규화한 Document Parse 결과만 Solar `solar-pro3`에 전달합니다.
5. Solar 결과를 `RECEIPT`, `TICKET`, `LETTER`, `UNKNOWN` 중 하나로 검증합니다.
6. 성공한 경우에만 원본 이미지와 `TYPE_PENDING` 임시 기록을 저장합니다.

API 3에서는 Information Extract를 호출하지 않습니다.

## API 4 문서 유형 확정과 Information Extract

1. 인증 사용자의 `draftId`인지 확인합니다.
2. `TYPE_PENDING` 상태에서만 확정된 `documentType`을 받습니다.
3. 영수증과 티켓은 저장한 원본 이미지를 `information-extract` 모델에 전달합니다.
   Upstage 요청의 메시지에는 원본 이미지 `image_url` 항목 하나만 전달합니다.
4. 영수증은 `memoryDate`, `storeName`만, 티켓은 `memoryDate`, `eventName`, `venue`, `seat`만 요청합니다.
5. 손편지는 외부 API를 호출하지 않고 API 3의 `parsedContent` 본문을 재사용합니다.
6. Information Extract가 불확실한 필드에 반환한 빈 문자열과 날짜 형식이 잘못된 값은 `null`로 정규화합니다.
7. 외부 호출과 결과 검증이 성공한 뒤에만 앞면 후보를 저장하고 `FRONT_PENDING`으로 변경합니다.

Information Extract 실패 시 `503 AI_001`을 반환하며 임시 기록은 `TYPE_PENDING` 상태로 유지합니다.

## 외부 요청 설정

- Upstage Base URL: `https://api.upstage.ai`
- Document Parse endpoint: `/v1/document-digitization`
- Information Extract endpoint: `/v1/information-extraction/chat/completions`
- Solar endpoint: `/v1/chat/completions`
- 연결 제한 시간: 5초
- 요청 제한 시간: 30초
- 자동 재시도: 0회

Document Parse는 한 이미지에 한 번만 호출해야 하므로 서버에서 자동 재시도하지 않습니다. 실패한 API 3 요청은 이미지와 임시 기록을 남기지 않으며 사용자가 이미지를 다시 전송해 재시도합니다.

## 이미지 정책

- 백엔드 지원 형식: JPEG/JPG, PNG, WebP
- 파일당 최대 용량: 10MB
- HEIC: 프론트엔드에서 JPEG 또는 WebP로 변환한 뒤 API 3에 전송
- 저장소: 서버 로컬 비공개 파일시스템
- 저장 키: 사용자 UUID와 draft UUID를 포함한 충돌 방지 경로
- 미완료 임시 기록과 원본 이미지 보관: 7일
- 외부 AI 또는 DB 저장 실패: 영구 이미지와 임시 기록을 남기지 않음
- 이미지 URL: API 3 응답에 포함하지 않으며 공개 정적 URL로 제공하지 않음

## 보안

- API 키는 `UPSTAGE_API_KEY` 환경 변수에서만 읽습니다.
- 크레딧 코드와 실제 API 키는 소스, 설정, 예시 파일, 로그와 응답에 기록하지 않습니다.
- Upstage 원본 응답, `parsedContent`, 원본 이미지 저장 위치를 프론트엔드에 반환하지 않습니다.
- 손편지 본문을 포함한 문서 내용은 일반 애플리케이션 로그에 기록하지 않습니다.
