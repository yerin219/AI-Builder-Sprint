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
4. 영수증은 `memoryDate`, `storeName`, 중첩 객체 배열 `purchaseItems[{name, quantity}]`를, 티켓은 `memoryDate`, `eventName`, `venue`, `seat`만 요청합니다.
5. 영수증 품목은 실제 구매·주문 항목만 남깁니다. 소계·합계·부가세·할인·쿠폰·포인트·결제수단·카드번호·승인번호·주문번호·사업자정보·광고 문구를 제외하고, 카페·음식점의 ICE·샷 추가·포장·사이즈업 옵션도 제외하되 마트의 실제 상품명은 유지합니다.
6. 영수증 품목 선정을 위한 Solar 호출은 추가하지 않으며, 사용자가 API 5에서 이름·수량을 수정하고 항목을 추가·삭제·선택해 최종 확정합니다.
7. 손편지는 외부 API를 호출하지 않고 API 3의 `parsedContent` 본문을 재사용합니다.
8. Information Extract가 불확실한 필드에 반환한 빈 문자열과 날짜 형식이 잘못된 값은 `null`로, 유효한 영수증 품목이 없으면 `purchaseItems: []`로 정규화합니다.
9. 외부 호출과 결과 검증이 성공한 뒤에만 앞면 후보를 저장하고 `FRONT_PENDING`으로 변경합니다.

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

### API 6·7·8 계약 감사 및 프론트 연동 보완

- 사용 모델·도구: Codex, Git, Gradle, Node.js E2E, 로컬 MySQL, 실제 Upstage API. 단위·Controller 테스트에서는 Mock 서버를 사용하고 최종 E2E에서만 개인정보 없는 합성 티켓 이미지를 실제 Upstage로 전송함.
- 작업 목적: 최신 `backend` 병합본의 티켓 AI 회상, 카드 저장, 서랍 조회 구현을 `docs/API_SPEC.md`와 다시 대조하고 프론트 연동에 필요한 누락을 보완함.
- 사용한 프롬프트 요약: API 명세를 기준으로 API 6·7·8 백엔드를 점검하고 부족한 부분을 수정한 뒤, 기존 API 3·4·5와 같은 방식으로 프론트와 연결하고 반복 검증하도록 요청함.
- AI가 제안·수정한 내용: 빈 티켓 subtype은 `TICKET_001`, null·개수 불일치 답변은 `TICKET_003`으로 일관되게 처리함. Solar 구조화 응답의 대상 필드가 문자열인지 검증하고 잘못된 UUID 경로도 공통 오류 envelope로 반환함. 카드 조회 응답이 제공하던 `/files/cards/{cardId}/front`와 `/files/cards/{cardId}/back/{index}`를 실제 인증 이미지 endpoint로 연결하고, 카드 소유권·1부터 시작하는 뒷면 사진 index·저장 확장자 기반 media type을 검증함. 보호 이미지의 Bearer 인증·binary 응답 계약을 API 명세에 추가하고 장기 만료 방식만 TODO로 유지함.
- 팀원이 직접 결정·수정한 내용: 명세 TODO인 제목 길이, 추가 사진 제한, 임시 기록 만료 즉시 차단 정책은 확정하지 않음. 이미지 endpoint에는 기존 전역 Bearer 인증과 카드 소유권 정책을 그대로 적용함.
- 실행한 테스트: 티켓 회상·카드 이미지 대상 Gradle 테스트, 전체 `gradlew.bat test`, 전체 `gradlew.bat build`, `git diff --check`, 별도 18080 서버에서 회원가입→API 3→4→5→6→7→8 실제 E2E.
- 테스트 결과: 대상 테스트 22개 통과, 전체 테스트 91개 통과, 전체 빌드 및 diff 검사 성공. E2E에서 `CONCERT_PERFORMANCE` 추천, 질문 3개, Solar 제목, DIRECT·AI_RECALL 카드 2장, 2026년 서랍·상세·PNG 이미지 조회가 성공했고 잘못된 답변 `TICKET_003`, 중복 저장 `DRAFT_003`, 무토큰 `AUTH_001`, 타 사용자 카드·이미지 `CARD_001`을 확인함.
- 발생한 문제와 해결: API 8 응답은 이미지 URL을 만들지만 실제 파일 제공 endpoint가 없어 404가 발생하는 문제를 찾아 저장소 읽기와 보호된 이미지 Controller를 추가함. Bean Validation이 도메인 오류 코드를 선점하던 문제는 DTO 제약을 서비스 검증으로 넘겨 명세 오류 코드로 통일함. 사용자 스크린샷의 외부 전송은 중단하고 코드가 생성한 무개인정보 합성 티켓으로 실제 E2E를 수행함.
- 관련 브랜치: `feature/be-api6-8-fixes`. 기능 브랜치 push 완료, PR은 진행하지 않음.

### API 9~12 최종 저장·조회 안정성 점검

- 사용 모델·도구: Codex, Gradle Wrapper, JUnit 5, Mockito.
- 작업 목적: 최종 카드 저장과 서랍 조회 흐름을 최신 API 번호 기준으로 재점검하고, 저장 실패 시 데이터와 파일이 어긋나는 문제를 방지함.
- AI가 제안·수정한 내용: `/cards`의 필수 `card` JSON 파트 누락을 `VALIDATION_001`로 구분하고, DB 저장 중 일반 런타임 오류가 발생해도 저장된 뒷면 사진을 삭제한 뒤 `CARD_003`으로 응답하도록 보완함. 사진 파일 쓰기 도중 실패한 경우에도 새로 생성된 부분 파일이 정리 대상에 포함되도록 저장 순서를 수정함.
- 실행한 테스트: 관련 `CardCreateServiceTest`, `GlobalExceptionHandlerTest`, 전체 `gradlew.bat test`, `git diff --check`.
- 팀 결정이 필요한 입력 길이·사진 개수·토큰 만료·이미지 URL 장기 정책은 기존 TODO로 유지함.
- 관련 브랜치: `feature/be-api6-8-fixes`.
# AI 활용 기록

## 2026-07-31 API 6·7·8 프론트엔드 연동

- 사용 모델·도구: Codex, Node.js 내장 테스트와 E2E, oxlint, Vite, 로컬 MySQL, 실제 Upstage API. 실제 E2E에는 개인정보 없는 코드 생성 합성 티켓만 사용함.
- 작업 목적: API 3·4·5가 연결된 프론트 브랜치에 티켓 AI 회상, 카드 저장, 서랍·카드 조회 흐름을 API 명세대로 연결하고 회귀 테스트를 추가함.
- 사용한 프롬프트 요약: API 6·7·8의 백엔드 구현을 점검·보완한 뒤 이전 API 3·4·5 연동과 같은 절차로 프론트 연결과 반복 검증을 수행하도록 요청함.
- AI가 제안·수정한 내용: API 6의 subtype 추천·고정 질문·제목 후보 요청, API 7의 DIRECT·AI_RECALL multipart 저장, API 8의 서랍·연도별 목록·상세 요청 계약 테스트를 추가함. DIRECT 저장 화면의 AI 버튼이 실제 회상 시작 화면으로 이동하도록 수정하고, subtype·질문·작성 중 답변·제목 후보를 draft별 세션에 보관해 새로고침 후 복구하며 최종 저장 후 제거함. 요청 취소 시 `AbortError`를 보존하고, 명세의 `/files/cards/...` URL을 API base URL 아래의 보호된 이미지 endpoint로 해석하도록 수정함.
- 팀원이 직접 결정·수정한 내용: 백엔드가 반환하는 목록 순서를 프론트가 다시 정렬하지 않으며, 명세 TODO인 같은 날짜 tie-breaker와 이미지 만료 정책은 임의로 추가하지 않음.
- 실행한 테스트: `npm test` 반복 실행, `npm run lint`, `npm run build`, `git diff --check`, 별도 18080 서버에서 `npm run test:e2e:api6-8` 실제 호출.
- 테스트 결과: 프론트 계약 테스트 27개 전부 통과, lint 오류 0개, Vite production build 성공(57 modules). E2E에서 API 6 추천·질문 3개·제목, API 7 DIRECT·AI_RECALL 저장, API 8 서랍·상세·보호 이미지와 주요 오류 응답이 모두 통과함.
- 발생한 문제와 해결: DIRECT 화면에서 AI_RECALL만 선택하면 회상 질문 없이 저장되어 검증 오류가 나던 막다른 흐름을 실제 API 6 시작 경로로 연결함. 앱 context path가 `/api`인데 이미지 URL이 `/files/...`라 직접 fetch 시 404가 되던 문제는 공통 API URL 해석 함수를 사용해 `/api/files/...`로 요청하도록 해결함. 사용자 스크린샷의 외부 전송 대신 테스트 스크립트가 합성 PNG를 자체 생성하도록 구성함.
- 관련 브랜치: `feature/fe-api6-8-integration`. 기능 브랜치 push 완료, PR은 진행하지 않음.

## 2026-07-31 API 9~12 최종 화면 흐름 보완

- 사용 모델·도구: Codex, Node.js 내장 테스트, oxlint, Vite.
- 작업 목적: 제목 생성 이후 카드 뒷면 작성부터 미리보기·저장·서랍·카드 상세까지 사용자가 화면 버튼만으로 완료할 수 있게 점검함.
- AI가 제안·수정한 내용: 저장 전에 흰색 카드 앞·뒷면을 함께 확인하고 수정 단계로 돌아갈 수 있는 최종 미리보기를 추가함. 저장 완료 후 서랍 이동과 카드 간 이동은 완료된 저장 화면이 뒤로가기에 다시 나타나지 않도록 history를 교체함. 명세 밖 외부 절대 이미지 URL은 거부해 Bearer 토큰이 외부로 전달되지 않게 했고, 이미 연결된 TODO 주석을 정리함.
- 실행한 테스트: `npm test`, `npm run lint`, `npm run build`, `git diff --check`.
- 팀 결정이 필요한 입력 제한·이미지 만료 정책과 명시적 제외 기능은 추가하지 않음.
- 관련 브랜치: `feature/fe-api6-8-integration`.

## 2026-08-01 영수증 구매 품목과 경과일 명세 보완

- 사용 모델·도구: Codex, Git diff 검사. Upstage API는 실제 호출하지 않음.
- 작업 목적: 기존 API 흐름을 유지하면서 영수증 구매 품목을 앞면 후보부터 저장·조회까지 연결하고, 서랍 카드의 `memoryDate` 기반 경과일 표시 규칙을 문서화함.
- 사용한 프롬프트 요약: 원본 이미지 Information Extract에 `purchaseItems` 중첩 배열을 추가하고 결제·사업자·광고·옵션 행을 제외하며, 사용자가 최종 품목을 편집·선택하도록 명세를 수정하되 Solar·신규 API·별도 DB 컬럼은 추가하지 않도록 요청함. 경과일은 사용자 기기 날짜로 계산하고 1분마다 갱신하며 미래 날짜도 처리하도록 요청함.
- AI가 제안·수정한 내용: API 4 `frontCandidate`, API 5 확정 `front`, API 11 목록과 API 12 상세의 `purchaseItems` 계약·예시를 추가함. 영수증 추출 제외 기준, 항목 검증, 사용자 최종 확인, Information Extract 원본 이미지 독립 호출과 Solar 미사용 원칙을 명시함. 경과일의 당일·과거·미래 표시와 1분 갱신 규칙을 추가함.
- 팀원이 직접 결정·수정한 내용: 대표 품목은 AI가 자동 확정하지 않고 사용자가 최종 선택하며, 기존 API와 앞면 저장 구조를 재사용하고 별도 DB 컬럼은 만들지 않기로 결정함.
- 실행한 검증: 문서 대상 `git diff --check`만 실행하고 광범위한 테스트는 실행하지 않음.
- 발생한 문제와 해결: 기존 작업 중인 소스·테스트 변경은 건드리지 않고 문서 두 파일만 수정함.
- 관련 PR 또는 커밋: 없음. 커밋·push·PR을 진행하지 않음.

## 2026-08-01 제목 생성 품질 개선 및 통합 문서화

- 사용 모델·도구: Codex, Gradle Wrapper, JUnit 5, Git. Upstage API는 실제 호출하지 않고 Mock 서버로 요청 형식을 검증함.
- 작업 목적: 티켓 회상 답변 세 개를 쉼표로 이어 붙이던 제목 후보를 하나의 핵심 장면이나 의미로 압축하도록 Solar 요청을 개선하고, 영수증 구매 품목·카드 경과일과 함께 README·API 명세·에이전트 지침을 실제 구현에 맞춤.
- 사용한 프롬프트 요약: 답변 나열·단순 연결을 금지하고 공백 포함 8~24자의 명사구 또는 짧은 문장을 권장하며, 사용자 답변에 없는 사실은 만들지 않도록 요청함.
- AI가 제안·수정한 내용: 제목 생성 프롬프트에 나열 금지·핵심 장면 압축·권장 길이 규칙을 추가하고 제목 생성에만 `temperature: 0.3`을 적용함. 문서 유형과 티켓 세부 유형 분류는 기존 `temperature: 0`을 유지함. README와 API 명세에 영수증 `purchaseItems`, 사용자 최종 선택, 경과일 표시, 제목 생성 규칙을 함께 기록함.
- 팀원이 직접 결정·수정한 내용: AI 제목은 최종값이 아닌 사용자 수정 가능한 후보로 유지하고, 8~24자는 생성 권장 범위로만 사용하며 미확정 API 최대 길이 제한으로 강제하지 않음.
- 실행한 검증: 제목 생성 관련 Gradle 테스트, 백엔드 전체 `gradlew.bat test`, 프론트엔드 `npm test`, `npm run lint`, `npm run build`, `git diff --check`.
- 검증 결과: 제목 생성 관련 테스트 6개와 백엔드 전체 테스트 통과, 프론트엔드 테스트 32개·lint·Vite production build 통과, diff 검사 통과.
- 발생한 문제와 해결: 기본 Gradle 저장 경로가 쓰기 불가능한 `C:\\.gradle`로 지정돼 사용자 Gradle 캐시를 명시해 테스트를 실행함. 백엔드 `build`는 compile·패키징 후 OneDrive가 기존 테스트 HTML 리포트를 일반 파일로 제공하지 않아 리포트 스냅샷 단계에서 실패했지만, 전체 `test` 태스크는 별도로 정상 완료함.
- 관련 PR 또는 커밋: 통합 PR 생성 후 갱신 예정.

### 최신 카드 관리 병합 호환 보완

- 최신 `origin/backend`·`origin/frontend`의 저장 카드 수정·삭제 기능을 병합하면서 카드 상세 화면의 경과일 갱신과 수정·삭제 UI를 함께 유지함.
- 영수증 카드 수정 요청에 `purchaseItems` 편집을 연결하고, 구버전 요청이 해당 필드를 생략하면 기존 저장 품목을 보존하도록 백엔드 호환 처리를 추가함.
- 관련 구매 품목 수정·앞면 확정·Solar 제목 테스트와 프론트 테스트 32개, lint, production build가 통과함. 이후 백엔드 전체 재검증은 사용자 요청으로 중단함.

## 2026-08-01 공통 로고 적용

- 사용 모델·도구: Codex, 이미지 편집, Vite.
- 작업 목적: 사용자가 제공한 기억서랍 로고를 모든 프론트 화면의 상단 중앙에 공통으로 표시함.
- 사용한 프롬프트 요약: 가로형 로고 이미지의 바깥 배경을 사이트 배경색과 자연스럽게 맞추고 전체 화면 상단에 적용하도록 요청함.
- AI가 제안·수정한 내용: 공통 앱 셸에 로고 헤더를 추가하고, 로그인·회원가입 화면의 기존 CSS 서랍 아이콘을 제거함. 로고 배경을 사이트 색상에 맞춰 보정하고 모바일 너비에 맞게 크기를 제한함.
- 팀원이 직접 결정·수정한 내용: 사용자가 최종 가로형 로고 이미지와 상단 중앙 배치를 선택함.
- 실행한 테스트: `npm.cmd run build`, `git diff --check`.
- 테스트 결과: Vite production build 성공(59 modules), 공백 오류 없음.
- 관련 브랜치: `codex/global-logo`.

## 2026-08-01 로그아웃·페이지별 도움말

- 사용 모델·도구: Codex, React Router, Vite, oxlint.
- 작업 목적: 로그인 사용자가 화면 상단에서 로그아웃할 수 있게 하고, 각 화면에서 필요한 사용법을 바로 확인할 수 있는 도움말을 제공함.
- 사용한 프롬프트 요약: 로고 높이 안의 오른쪽에 로그아웃을 배치하고, 기억 담기 버튼과 같은 갈색의 원형 물음표 버튼에서 현재 페이지에 맞는 짧은 안내를 표시하도록 요청함.
- AI가 제안·수정한 내용: 세션 토큰 삭제 후 로그인 화면으로 이동하는 로그아웃 동작, 경로별 도움말 문구, 모달 열기·닫기와 반응형 상단 배치를 구현함. 공통 로고를 누르면 홈 화면으로 이동하도록 연결함.
- 팀원이 직접 결정·수정한 내용: 도움말은 갈색 원형 물음표, 로그아웃은 로고 오른쪽, 도움말 문구는 현재 화면의 핵심 사용법만 간단히 안내하도록 결정함.
- 실행한 테스트: `npm.cmd run build`, `npm.cmd run lint`, `git diff --check`.
- 테스트 결과: Vite production build 성공(59 modules), lint 오류 없음, 공백 오류 없음.
- 관련 브랜치: `codex/logout-button`.

### 손편지 최종 카드 텍스트 전용 표시

- 연도별 카드 목록과 카드 상세 앞면에서 손편지 `frontImageUrl`을 렌더링하던 동작을 제거하고, Document Parse 후 사용자가 확인한 `ocrText`만 표시하도록 수정함.
- 저장 카드 목록·상세 API의 손편지 앞면 응답도 `ocrText`만 제공하도록 축소해 프론트에서 원본 이미지를 다시 표시할 여지를 제거함.
- 최신 `origin/develop`을 기준으로 프론트 테스트 33개와 Vite production build, `git diff --check`를 통과함. 백엔드 응답 대상 테스트는 develop 동기화 전 통과했으며 동기화 후 재실행은 기존 테스트 중단 요청에 따라 생략함.

## 2026-08-01 회상 답변 종합 제목 생성 보완

- 사용 모델·도구: Codex, Upstage Solar 연동 코드, Gradle Wrapper, JUnit 5 Mock 서버.
- 작업 목적: 여러 회상 답변을 빠뜨리지 않고 반영하면서도 답변별 핵심어를 쉼표로 나열하지 않는 하나의 연결된 제목을 생성함.
- 사용한 프롬프트 요약: 모든 비어 있지 않은 답변의 사실과 의미를 하나의 장면으로 종합하고, 구분자 제거만 한 단순 연결문이나 답변 일부 누락을 금지하도록 요청함.
- AI가 제안·수정한 내용: 제목 프롬프트에 전체 답변 종합 규칙과 나쁜·좋은 예시를 추가하고, 쉼표·가운뎃점·세미콜론·슬래시·막대 기호가 있는 나열형 후보는 한 번 자동 재생성하도록 보완함. 재생성 후에도 나열형이면 `AI_002`로 처리함.
- 팀원이 직접 결정·수정한 내용: 답변 하나를 대표로 고르는 대신 비어 있지 않은 모든 답변을 제목의 전체 의미에 반영하도록 결정함.
- 실행한 검증: `UpstageTicketRecallSolarGatewayTests`, `TicketRecallServiceTest`, `git diff --check`.
- 검증 결과: 관련 테스트 9개와 diff 검사 통과.
- 발생한 문제와 해결: 첫 실행에서 기존 프롬프트 문구를 정확히 비교하던 테스트 1개가 실패해 새 프롬프트의 동일한 의미 문구로 기대값을 갱신한 뒤 재실행해 통과함.

## 2026-08-01 열린 연도 서랍과 종이 앞면 배치

- 사용 모델·도구: Codex, React, CSS, Spring Boot 조회 DTO, Gradle Wrapper, Node.js 내장 테스트, oxlint, Vite. Upstage API는 호출하지 않음.
- 작업 목적: 연도 서랍의 세로 목록을 실제 열린 나무 서랍 안에 영수증·티켓·손편지 앞면이 펼쳐진 화면으로 바꾸고, 같은 카드가 다시 열어도 안정적인 위치를 유지하게 함.
- 사용한 프롬프트 요약: 사용자가 제공한 열린 서랍 이미지를 배경으로 사용하고 기록 앞면은 서랍 내부에만 배치하며, 업로드 원본 이미지가 아닌 API의 사용자 확정 텍스트로 문서별 앞면을 재구성하도록 요청함. 심각한 오류가 아니면 검증 범위를 최소화하도록 요청함.
- AI가 제안·수정한 내용: API 11 카드 UUID에서 0 이상의 `layoutSeed`를 만들어 반환하고, 프론트는 이를 미리 정한 서랍 슬롯과 기울기에 연결함. 구버전 응답에는 `cardId` 해시 fallback을 사용함. 최근 10장의 영수증·티켓·손편지 앞면을 나무 프레임과 앞판을 제외한 내부 영역에만 표시하고, 첫 클릭은 중앙 확대, 두 번째 클릭은 기존 상세 화면 이동으로 구현함. 영수증은 가게명·구매 품목, 티켓은 행사명·날짜·장소·좌석, 손편지는 날짜·OCR 본문 일부를 표시하며 원본 사진은 렌더링하지 않음.
- 팀원이 직접 결정·수정한 내용: 사용자가 제공한 다섯 번째 이미지를 서랍 배경으로 사용하고, 카드 위치는 무작위처럼 보이되 재접속 시 유지하며, 한 화면에는 최근 10장까지만 펼치기로 결정함.
- 실행한 검증: 프론트 `npm.cmd test`, `npm.cmd run lint`, `npm.cmd run build`; 백엔드 `CardQueryResponseTest`, `JpaCardQuerySourceTest`, `CardQueryServiceTest`; `git diff --check`.
- 검증 결과: 프론트 테스트 35개, lint, Vite production build와 변경된 백엔드 조회 테스트가 통과함.
- 발생한 문제와 해결: 로컬 프로세스와 OneDrive가 기존 `backend/build` 출력 폴더를 잡고 있어 첫 Gradle 실행이 실패함. 기존 폴더를 삭제하지 않고 임시 빌드·프로젝트 캐시 경로로 대상 테스트를 재실행해 통과함.
- 관련 브랜치: `feature/open-drawer-memories`.
