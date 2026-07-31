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

---

# AI 활용 기록

## 2026-07-30

- 사용 모델·도구: Codex. Upstage Document Parse, Information Extract와 Solar는 실제 호출하지 않음.
- 작업 목적: API 4·5 연동 전에 독립적으로 작성할 수 있는 API 6 티켓 회상 규칙, API 7 카드 저장 검증과 API 8 조회 응답 기반 구현.
- 사용한 프롬프트 요약: `README.md`, `AGENTS.md`, `docs/API_SPEC.md`, `docs/AI_USAGE.md`를 먼저 확인하고, TODO를 임의로 결정하지 않으며, API 6·7·8에서 다른 담당자 코드 없이 구현 가능한 부분만 작성하도록 요청함.
- AI가 제안·수정한 내용: API 8 연도별 서랍·카드 목록과 영수증·티켓 DIRECT·티켓 AI_RECALL·손편지 상세 응답 모델을 작성함. 최근 연도 우선 정렬, 빈 목록 반환, 미정인 같은 연도 카드 순서 보존, 현재 사용자 기준 조회 연결 인터페이스, 다른 사용자 카드와 없는 카드 구분 및 관련 테스트를 작성함. API 6의 티켓 세부 유형 enum, 고정 질문 9개, 질문 ID 중복·누락·유형 불일치·전체 빈 답변 검증, 6.1·6.2·6.3 요청·응답 DTO, Solar 연결 인터페이스와 회상 서비스를 작성함. Solar가 유형을 판단하지 못한 경우 수동 선택 응답으로 변환하고, 제목 생성에는 비어 있지 않은 사용자 답변과 서버 고정 질문만 전달하며, 비어 있거나 여러 줄인 제목 결과를 거부하는 테스트를 추가함. API 7의 카드 저장 요청·응답 DTO와 유형별 검증을 작성하고, 영수증·손편지의 일기 또는 사진 조건, 티켓 DIRECT·AI_RECALL 필드 구분, 티켓 뒷면 사진 금지와 AI 회상 질문 검증을 테스트함.
- 팀원이 직접 결정·수정한 내용: README와 API 명세의 확정 질문 및 enum만 사용하고, 미정인 Solar 설정·사진 제한·날씨·기분·정렬 방향은 구현하지 않기로 결정함.
- 실행한 테스트: `gradlew.bat clean test`, `gradlew.bat build`, `git diff --check`.
- 테스트 결과: 전체 테스트와 빌드 성공, `git diff --check` 통과.
- 발생한 문제와 해결: `LocalDate`가 JSON 배열로 직렬화되는 문제를 테스트에서 발견하여 `yyyy-MM-dd` 문자열 형식으로 고정함. 선택 좌석과 미응답 답변이 `null`로 유지되는 테스트를 추가함. API 6의 Solar 제목 입력에서는 빈 답변을 제외해야 하지만 API 7 카드 저장에서는 미응답을 포함한 질문 세 개를 모두 보관해야 하는 차이를 발견하여 검증 결과를 분리함. API 8 AI 회상 상세 테스트가 과거 DTO의 `memoryText: null`을 포함하던 문제를 발견하고, 명세대로 해당 필드 자체가 응답에 나타나지 않도록 상세 유형을 분리함.
- 관련 PR 또는 커밋: 없음. 커밋·push·PR을 진행하지 않음.

## 2026-07-31

- 사용 모델·도구: Codex. Upstage Document Parse, Information Extract와 Solar는 실제 호출하지 않음.
- 작업 목적: API 8의 같은 연도 카드 조회 순서를 팀 결정에 따라 오래된 날짜부터 최신 날짜 순서로 확정.
- 사용한 프롬프트 요약: 같은 연도 서랍 안의 카드를 오래된 순으로 반환하도록 백엔드 코드와 문서를 수정하고, 완료 후 프론트엔드 전달 문구를 작성하도록 요청함.
- AI가 제안·수정한 내용: 카드 조회 결과를 `memoryDate` 오름차순으로 정렬하고, 조회 원본 순서가 뒤섞여 있어도 오래된 카드가 먼저 반환되는 테스트를 추가함. README와 API 명세의 설명·응답 예시·TODO를 확정된 정렬 기준에 맞게 수정함.
- 팀원이 직접 결정·수정한 내용: 같은 연도 카드의 날짜 정렬 방향을 오래된 순으로 확정함.
- 실행한 테스트: `gradlew.bat clean test`, `gradlew.bat build`, `git diff --check`.
- 테스트 결과: 전체 테스트와 빌드 성공, `git diff --check` 통과.
- 발생한 문제와 해결: 없음.
- 관련 PR 또는 커밋: 없음. 커밋·push·PR을 진행하지 않음.

### API 6·7·8 Postman 준비

- 사용 모델·도구: Codex. Upstage Document Parse, Information Extract와 Solar는 실제 호출하지 않음.
- 작업 목적: API 4·5 구현 정보에 맞춘 API 6·7·8 Postman 요청 컬렉션과 실행 안내 준비.
- 사용한 프롬프트 요약: 1조가 전달한 API 1~5 연동·Postman 정보를 참고해 API 6·7·8용 테스트 컬렉션과 안내를 작성하도록 요청함.
- AI가 제안·수정한 내용: 비밀값 없이 사용할 수 있는 컬렉션 변수, API 6 세 요청, API 7 영수증·손편지·티켓 DIRECT·AI_RECALL 저장 요청, API 8 세 조회 요청과 응답 검증 스크립트를 작성함.
- 팀원이 직접 결정·수정한 내용: API 4·5 브랜치가 병합되기 전에는 컬렉션을 실행 가능한 완료물로 보지 않고, 요청 형식·순서 준비용으로만 사용하기로 함.
- 실행한 테스트: PowerShell `ConvertFrom-Json`을 이용한 Postman 컬렉션 JSON 형식 검증, `git diff --check`.
- 테스트 결과: Postman 컬렉션 JSON 형식 검증 성공, `git diff --check` 통과.
- 발생한 문제와 해결: API 4·5 코드가 아직 현재 브랜치에 병합되지 않아 실제 Controller 호출은 검증할 수 없음.
- 관련 PR 또는 커밋: 없음. 커밋·push·PR을 진행하지 않음.

### 남은 연동 작업

- 영수증·손편지 `backPhotos`의 최종 개수 제한과 파일 조회 방식 확정
- 이미지 URL의 인증 방식과 만료 여부 확정

### API 6·7·8 Controller·DB 연동 및 실제 호출 검증

- 사용 모델·도구: Codex, Git, Gradle, Postman, 로컬 MySQL 8.4, 실제 Upstage Solar API.
- 작업 목적: 병합된 API 1~5의 인증 사용자, `MemoryDraft`, `FRONT_CONFIRMED` 앞면을 기준으로 API 6·7·8을 실제 Controller와 DB에 연결.
- 사용한 프롬프트 요약: API 6 티켓 회상, API 7 최종 카드 저장, API 8 서랍·카드 조회를 기존 인증·공통 응답·DB 구조에 연결하고 Postman으로 순서대로 검증하도록 요청함.
- AI가 제안·수정한 내용: 티켓 세부 유형과 질문 조회 Controller, 확정 유형 저장, Solar 세부 유형·제목 생성 게이트웨이, 카드 엔티티·저장소·multipart 저장 Controller, 중복 저장 방지와 draft `SAVED` 전환, 사용자별 서랍·카드 조회 구현체와 Controller, 기존 테이블 마이그레이션과 카드 스키마를 구현함.
- 팀원이 직접 결정·수정한 내용: 티켓 DIRECT와 AI_RECALL에 서로 다른 draft를 사용하고, 실제 티켓 이미지와 사용자 답변으로 Postman 호출 순서를 검증함.
- 실행한 검증: `gradlew.bat compileJava --console=plain`, `gradlew.bat test --console=plain`, Postman API 6.1·6.2·6.3·7.3·7.4·8.1·8.2·8.3 실제 호출.
- 검증 결과: 메인 코드 컴파일과 전체 테스트 성공. `CONCERT_PERFORMANCE` 추천, 고정 질문 3개, Solar 제목 생성, DIRECT·AI_RECALL 카드 2장 저장, 2025년 서랍·목록·두 저장 방식의 상세 조회 성공.
- 발생한 문제와 해결: `CardCreateService`에 생성자가 두 개라 Spring이 생성자를 선택하지 못한 문제를 확인해 운영 생성자에 `@Autowired`를 명시함. 이미 저장된 draft 재호출은 `DRAFT_003`으로 차단되고 기존 카드가 정상 조회됨을 확인함.
- 관련 브랜치: `feature/be-ticket-recall`.
