# 기억서랍 API 명세서

> Base URL: `http://localhost:8080/api`
>
> 기본 형식: `application/json; charset=UTF-8`
>
> 이미지 포함 요청: `multipart/form-data`
>
> 인증: `Authorization: Bearer {accessToken}`

이 문서는 기억서랍의 최신 `README.md`와 확정된 실제 앱 이용 순서만을 기준으로 작성한 해커톤 MVP용 API 명세입니다.

API는 사용자의 확인 순서를 다음과 같이 보장합니다.

```text
이미지 업로드
→ Document Parse로 전체 내용 인식
→ Solar가 문서 유형 후보 제안
→ AI 문서 유형 후보 확인
→ 확정된 유형에 맞는 정보 추출
→ 추출 결과 확인·수정
→ 카드 앞면 확정
→ 카드 뒷면 작성
   └─ 티켓에서 AI 질문 선택 시에만 Solar가 세부 유형 후보 제안
→ 카드 최종 저장
```

외부 Upstage API는 백엔드에서만 호출합니다. 프론트엔드는 Document Parse, Information Extract, Solar를 직접 호출하지 않습니다.

---

## 1. API 목록

| 번호 | Method | Endpoint | 기능 | 인증 |
|---:|---|---|---|---|
| 1 | `POST` | `/auth/signup` | 회원가입 | 불필요 |
| 2 | `POST` | `/auth/login` | 로그인 | 불필요 |
| 3 | `POST` | `/memory-drafts/analyze` | 이미지 업로드 및 문서 유형 후보 생성 | 필요 |
| 4 | `POST` | `/memory-drafts/{draftId}/document-type/confirm` | 문서 유형 확정 및 유형별 정보 추출 | 필요 |
| 5 | `PUT` | `/memory-drafts/{draftId}/front/confirm` | 수정된 추출값 확인 및 카드 앞면 확정 | 필요 |
| 6 | `POST` | `/memory-drafts/{draftId}/ticket-recall/subtype-suggestion` | AI 질문 선택 후 티켓 세부 유형 후보 생성 | 필요 |
| 7 | `POST` | `/memory-drafts/{draftId}/ticket-recall/questions` | 티켓 세부 유형 확정 및 고정 질문 전체 조회 | 필요 |
| 8 | `POST` | `/memory-drafts/{draftId}/ticket-recall/title` | 답변을 바탕으로 제목 한 줄 생성 | 필요 |
| 9 | `POST` | `/cards` | 카드 뒷면과 함께 최종 카드 저장 | 필요 |
| 10 | `GET` | `/drawers` | 연도별 서랍 목록 조회 | 필요 |
| 11 | `GET` | `/drawers/{year}/cards` | 특정 연도의 카드 목록 조회 | 필요 |
| 12 | `GET` | `/cards/{cardId}` | 카드 앞·뒷면 상세 조회 | 필요 |
| 13 | `PUT` | `/cards/{cardId}` | 저장된 카드의 앞·뒷면 텍스트 정보 수정 | 필요 |
| 14 | `DELETE` | `/cards/{cardId}` | 저장된 카드 삭제 | 필요 |

README에 없는 로그아웃, 내 정보, 검색, 공유 API는 포함하지 않습니다.

---

## 2. 공통 규칙

### 2.1 성공 응답

```json
{
  "success": true,
  "message": "요청이 완료되었습니다.",
  "data": {}
}
```

### 2.2 실패 응답

```json
{
  "success": false,
  "code": "VALIDATION_001",
  "message": "요청값을 확인해주세요.",
  "data": null
}
```

### 2.3 인증 헤더

회원가입과 로그인을 제외한 모든 요청에 다음 헤더를 포함합니다.

```http
Authorization: Bearer {accessToken}
```

- 액세스 토큰은 로그인 응답으로 발급한 HS256 JWT입니다.
- JWT의 `sub`에는 사용자 UUID가 들어가며, 백엔드는 요청 본문의 사용자 ID가 아니라 검증된 `sub`를 사용합니다.
- 서명이 올바르지 않거나 만료된 토큰은 `401 AUTH_001`로 처리합니다.

### 2.4 날짜와 식별자

| 항목 | 형식 | 예시 |
|---|---|---|
| 실제 추억 날짜 | `YYYY-MM-DD` | `2026-07-25` |
| 생성 시각 | ISO 8601 | `2026-07-28T21:30:00+09:00` |
| 식별자 | UUID 문자열 | `9bb06555-85de-46e2-b44e-8f67eb8e08d2` |

- 카드가 들어갈 서랍 연도는 `memoryDate`에서 계산합니다.
- AI가 날짜를 찾지 못해도 오늘 날짜를 자동 입력하지 않습니다.
- 손편지는 날짜를 자동 추출하지 않으므로 사용자가 직접 입력합니다.
- `오늘로부터 n일 전` 표시는 API 필드가 아니라 프론트엔드가 카드의 `memoryDate`와 사용자 기기의 로컬 오늘 날짜를 날짜 단위로 비교해 계산합니다.
- 프론트엔드는 페이지를 열 때 계산하고 1분마다 오늘 날짜를 다시 확인합니다. 당일은 `오늘의 기억`, 365일 미만은 `오늘로부터 n일 전·후`, 365일 이상은 365일을 1년으로 계산해 `오늘로부터 n년 n일 전·후`로 표시합니다. 남은 일수가 0일이면 일수는 생략합니다.

### 2.5 `null`, 빈 문자열, 빈 배열

- AI가 확신할 수 없는 값은 추측하지 않고 `null`로 반환합니다.
- 문자열을 입력하지 않은 경우 `""` 대신 `null`을 사용합니다.
- 목록 결과가 없으면 `404`가 아니라 빈 배열 `[]`을 반환합니다.
- 좌석처럼 선택 항목이 `null`이면 최종 카드 UI에서 값뿐 아니라 `좌석`이라는 항목명도 표시하지 않습니다.

### 2.6 공통 enum

#### 문서 유형 `documentType`

| 값 | 의미 |
|---|---|
| `RECEIPT` | 영수증 |
| `TICKET` | 티켓 |
| `LETTER` | 손편지 |

#### 티켓 세부 유형 `ticketSubtype`

| 값 | 의미 |
|---|---|
| `CONCERT_PERFORMANCE` | 콘서트·공연 |
| `MOVIE` | 영화 |
| `EXHIBITION` | 전시 |

티켓 세부 유형은 사용자가 `AI 질문으로 떠올리기`를 선택한 경우에만 화면에 표시합니다. `직접 기록하기`에서는 묻거나 저장하지 않습니다.

#### 티켓 작성 방식 `writingMode`

| 값 | 의미 |
|---|---|
| `DIRECT` | 제목과 추억을 직접 작성 |
| `AI_RECALL` | 유형별 질문에 답하고 AI 제목 사용 |

#### 손편지 처리 메타데이터 `frontImageMode`

| 값 | 의미 |
|---|---|
| `ORIGINAL` | 원본 이미지 사용 |
| `BACKGROUND_REMOVED` | 단순 배경 제거 결과 사용 |

이 값은 앞면 확정 과정의 기존 처리 메타데이터입니다. 저장된 카드의 목록·상세 응답에는 포함하지 않으며 프론트엔드는 손편지 원본 또는 배경 제거 이미지를 카드에 렌더링하지 않습니다.

#### 임시 기록 상태 `draftStatus`

| 값 | 의미 |
|---|---|
| `TYPE_PENDING` | AI가 제안한 문서 유형을 사용자가 확인하기 전 |
| `FRONT_PENDING` | 문서 유형은 확정됐지만 추출 결과를 확인하기 전 |
| `FRONT_CONFIRMED` | 사용자가 수정된 추출값을 확인해 앞면이 확정됨 |
| `SAVED` | 최종 카드가 저장됨 |

### 2.7 사용자 소유권

- 모든 임시 기록과 카드는 로그인 사용자에게 귀속됩니다.
- 다른 사용자의 `draftId` 또는 `cardId`로 접근하면 `403 Forbidden`을 반환합니다.
- 백엔드는 요청 본문의 사용자 ID를 신뢰하지 않고 인증 토큰의 사용자 ID를 사용합니다.

### 2.8 카드 배경 규칙

- 영수증·티켓·손편지 카드의 배경은 모두 흰색으로 통일합니다.
- 영수증과 티켓 앞면에는 사용자가 확인한 Upstage 추출값만 표시하고, 분석에 사용한 원본 이미지는 표시하지 않습니다.
- 손편지 앞면에도 업로드 원본 이미지를 표시하지 않고 사용자가 확인한 `ocrText` 전체 본문만 표시합니다.
- 카드 배경색 선택·변경 기능을 제공하지 않습니다.
- 카드 배경색과 관련된 필드를 요청·응답에 포함하지 않습니다.
- 흰색 배경은 프론트엔드 표시 규칙이며 백엔드가 별도 값으로 저장하거나 반환하지 않습니다.

### 2.9 보호된 카드 이미지 URL

영수증·손편지 카드의 `backPhotoUrls`는 Base URL 아래에서 조회하는 상대 API 경로입니다. 저장된 카드의 목록·상세 응답에는 문서 유형과 관계없이 `frontImageUrl`을 포함하지 않습니다.

```http
GET /files/cards/{cardId}/front
GET /files/cards/{cardId}/back/{index}
Authorization: Bearer {accessToken}
```

- 업로드 원본은 내부 보관할 수 있지만 프론트엔드는 손편지 카드 앞면을 위해 `/files/cards/{cardId}/front`를 요청하거나 렌더링하지 않습니다.
- `index`는 `1`부터 시작하며 상세 응답의 `backPhotoUrls` 순서를 따릅니다.
- 성공 응답은 공통 JSON envelope가 아닌 저장된 이미지 binary이며 `Content-Type`은 `image/jpeg`, `image/png`, `image/webp` 중 하나입니다.
- 성공 응답은 `Cache-Control: no-store`를 사용합니다.
- 오류 응답은 공통 실패 envelope를 사용합니다. 토큰 누락·만료는 `401 AUTH_001`, 다른 사용자의 카드는 `403 CARD_001`, 카드 또는 해당 사진이 없으면 `404 CARD_002`입니다.
- 이미지 URL의 장기 만료·갱신 방식은 12절 TODO에 남깁니다.

---

## 3. 임시 기록 처리 방식

이미지는 처음 분석할 때 한 번만 업로드합니다. 백엔드는 이미지와 분석 중간 결과를 `draftId`에 연결해 임시 보관하고, 최종 카드 저장 시 해당 임시 기록을 사용합니다.

```mermaid
stateDiagram-v2
    [*] --> TYPE_PENDING: 이미지 분석
    TYPE_PENDING --> FRONT_PENDING: 문서 유형 확정
    FRONT_PENDING --> FRONT_CONFIRMED: 추출 결과 확인
    FRONT_CONFIRMED --> SAVED: 카드 최종 저장
```

- 문서 유형을 바꾸면 해당 유형의 스키마로 정보를 다시 추출합니다.
- 사용자는 최종 저장 전까지 앞면 확인 API를 다시 호출해 값을 수정할 수 있습니다.
- `SAVED` 상태의 임시 기록으로 카드를 중복 생성할 수 없습니다.
- 임시 기록 보관 시간은 아직 정하지 않았으므로 공통 TODO에 남깁니다.

---

## 4. 인증 API

### 4.1 회원가입

`POST /auth/signup`

#### Request

```json
{
  "email": "memory@example.com",
  "password": "1234567890"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `email` | String | O | 로그인에 사용할 이메일 |
| `password` | String | O | 1~10자 비밀번호. 공백만으로 구성할 수 없으며 문자 조합 제한은 없음 |

#### Response

`201 Created`

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": "3de81bcd-7939-4a70-b71b-bb205e12ed63",
    "email": "memory@example.com"
  }
}
```

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `VALIDATION_001` | 이메일 형식 오류, 비밀번호 누락·공백 또는 10자 초과 |
| 409 | `AUTH_003` | 이미 사용 중인 이메일 |

> 비밀번호 정책: 1자 이상 10자 이하이며, 공백만으로 구성할 수 없습니다. 대문자·소문자·숫자·특수문자 조합은 강제하지 않습니다.

### 4.2 로그인

`POST /auth/login`

#### Request

```json
{
  "email": "memory@example.com",
  "password": "1234567890"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `email` | String | O | 가입할 때 사용한 이메일 |
| `password` | String | O | 가입할 때 설정한 1~10자 비밀번호 |

#### Response

`200 OK`

```json
{
  "success": true,
  "message": "로그인되었습니다.",
  "data": {
    "userId": "3de81bcd-7939-4a70-b71b-bb205e12ed63",
    "accessToken": "<JWT_ACCESS_TOKEN>",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

- `accessToken`은 이후 인증이 필요한 API의 `Authorization: Bearer {accessToken}` 헤더에 사용합니다.
- `expiresIn`은 `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS` 환경 변수로 설정한 초 단위 값입니다.
- 토큰 서명 키는 32자 이상의 `JWT_SECRET` 환경 변수에서 읽으며 코드나 Git에 저장하지 않습니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `VALIDATION_001` | 이메일 형식 오류, 비밀번호 누락·공백 또는 10자 초과 |
| 401 | `AUTH_002` | 이메일 또는 비밀번호 불일치 |

---

## 5. 문서 분석 및 카드 앞면 API

### 5.1 이미지 분석 및 문서 유형 후보 생성

`POST /memory-drafts/analyze`

사용자가 촬영하거나 앨범에서 선택한 이미지를 업로드합니다. 백엔드는 Document Parse로 이미지의 전체 텍스트와 문서 구조를 인식한 뒤, 그 결과만 Solar에 전달해 `RECEIPT`, `TICKET`, `LETTER` 중 문서 유형 후보를 만듭니다. 이 단계에서는 Information Extract를 호출하지 않으며 유형별 추출 정보도 반환하지 않습니다.

Document Parse 결과는 `draftId`에 연결해 내부 보관합니다. 이후 손편지 본문 표시와 티켓 세부 유형 추정에서 같은 결과를 재사용하므로 동일 이미지를 다시 Document Parse에 보내지 않습니다.

#### Content-Type

`multipart/form-data`

#### Request Parts

| 파트 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `image` | File | O | 프론트엔드에서 방향과 크기를 보정한 문서 이미지 |

업로드 정책은 다음과 같습니다.

- 백엔드 지원 형식은 JPEG/JPG, PNG, WebP입니다.
- 파일당 최대 용량은 10MB입니다.
- HEIC 이미지는 프론트엔드에서 JPEG 또는 WebP로 변환한 뒤 전송합니다.
- 확장자나 선언된 MIME 타입만 신뢰하지 않고 실제 파일 시그니처를 함께 검증합니다.
- 저장 위치는 서버의 비공개 로컬 파일시스템이며 API 3 응답에 이미지 URL이나 저장 경로를 포함하지 않습니다.
- 충돌 방지를 위해 사용자 UUID와 `draftId`를 포함한 저장 키를 사용합니다.
- 미완료 임시 기록과 원본 이미지는 7일 동안 보관합니다.

#### Response — 티켓으로 판단한 경우

`200 OK`

```json
{
  "success": true,
  "message": "문서 유형을 분석했습니다.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "suggestedDocumentType": "TICKET",
    "typeCard": {
      "type": "TICKET",
      "label": "티켓"
    },
    "requiresManualSelection": false,
    "draftStatus": "TYPE_PENDING",
    "nextAction": "CONFIRM_DOCUMENT_TYPE"
  }
}
```

`typeCard`는 프론트엔드가 영수증·티켓·손편지 프레임을 선택하기 위한 데이터입니다. API가 프레임 이미지 파일을 반환하는 것은 아닙니다.

#### Response — 유형을 확신할 수 없는 경우

`200 OK`

```json
{
  "success": true,
  "message": "문서 유형을 직접 선택해주세요.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "suggestedDocumentType": null,
    "typeCard": null,
    "requiresManualSelection": true,
    "draftStatus": "TYPE_PENDING",
    "nextAction": "SELECT_DOCUMENT_TYPE"
  }
}
```

이 경우 프론트엔드는 영수증·티켓·손편지 선택지를 보여줍니다.

#### 이 API에서 반환하지 않는 값

- 날짜, 가게명, 행사명, 장소, 좌석, 손편지 본문
- 원본 이미지 URL
- 티켓 세부 유형

따라서 프론트엔드는 이 단계에서 문서 유형 카드만 보여주고 추출 결과 화면으로 먼저 넘어가지 않습니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `IMAGE_001` | 이미지 파일이 없음 |
| 413 | `IMAGE_002` | 이미지 용량 제한 초과 |
| 415 | `IMAGE_003` | 지원하지 않는 이미지 형식 |
| 422 | `DOCUMENT_001` | 흐림·잘림 등으로 문서 내용을 분석할 수 없음 |
| 503 | `AI_001` | Document Parse 서비스 일시 오류 |
| 503 | `AI_002` | Solar 문서 유형 판단 일시 오류 |

API 3에서는 Document Parse와 Solar 호출 및 응답 검증이 모두 성공한 뒤에만 원본 이미지와 `TYPE_PENDING` 임시 기록을 저장합니다. 이 최초 요청이 실패하면 프론트엔드에 재사용 가능한 `draftId`가 아직 없으므로 이미지와 임시 기록을 남기지 않고 사용자가 이미지를 다시 전송해 재시도합니다.

### 5.2 문서 유형 확정 및 유형별 정보 추출

`POST /memory-drafts/{draftId}/document-type/confirm`

AI의 판단이 맞으면 제안된 유형을, 틀리면 사용자가 직접 선택한 유형을 전송합니다. 영수증과 티켓은 Information Extract를 호출해 확정된 유형의 스키마에 맞는 정보만 추출합니다. 손편지는 앞 단계에서 저장한 Document Parse 결과의 전체 본문을 재사용하며 Information Extract를 호출하지 않습니다.

#### Path Parameter

| 변수 | 타입 | 설명 |
|---|---|---|
| `draftId` | UUID | 이미지 분석 API에서 받은 임시 기록 ID |

#### Request

```json
{
  "documentType": "TICKET"
}
```

#### 기록 유형별 앞면 필드

| 기록 유형 | 사용하는 Upstage 결과 | 필드 | 처리 원칙 |
|---|---|---|---|
| 영수증 | Information Extract | `memoryDate`, `storeName`, `purchaseItems` | 실제 구매·주문 품목 후보만 입력 |
| 티켓 | Information Extract | `memoryDate`, `eventName`, `venue`, `seat` | 확신할 수 있는 값만 입력 |
| 손편지 | 저장된 Document Parse 결과 | `ocrText` | 인식된 전체 본문 사용 |
| 손편지 공통 날짜 | 사용하지 않음 | `memoryDate` | 자동 추출하지 않고 `null` 반환 |

- Information Extract 결과의 날짜는 백엔드가 `YYYY-MM-DD`로 정규화합니다.
- 날짜를 정규화할 수 없거나 값이 불확실하면 추측하지 않고 `null`로 반환합니다.
- 유형별로 정의하지 않은 필드는 Information Extract 결과에 있더라도 프론트엔드 응답에 포함하지 않습니다.
- 영수증의 `purchaseItems`는 `{ "name": String, "quantity": Integer }` 객체 배열이며 품목을 찾지 못하면 `[]`을 반환합니다.
- `purchaseItems`가 비어 있으면 `emptyFields`에도 `"purchaseItems"`를 포함해 사용자가 직접 추가할 수 있게 합니다.

#### Response — 영수증 예시

`200 OK`

```json
{
  "success": true,
  "message": "영수증 정보를 추출했습니다.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "documentType": "RECEIPT",
    "frontCandidate": {
      "memoryDate": "2026-07-25",
      "storeName": "서면카페",
      "purchaseItems": [
        {
          "name": "아이스 아메리카노",
          "quantity": 2
        },
        {
          "name": "치즈케이크",
          "quantity": 1
        }
      ]
    },
    "emptyFields": [],
    "draftStatus": "FRONT_PENDING",
    "nextAction": "CONFIRM_FRONT"
  }
}
```

#### Response — 티켓 예시

`200 OK`

```json
{
  "success": true,
  "message": "티켓 정보를 추출했습니다.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "documentType": "TICKET",
    "frontCandidate": {
      "memoryDate": "2026-07-25",
      "eventName": "흠뻑쇼",
      "venue": "부산아시아드주경기장",
      "seat": null
    },
    "emptyFields": [
      "seat"
    ],
    "draftStatus": "FRONT_PENDING",
    "nextAction": "CONFIRM_FRONT"
  }
}
```

#### Response — 손편지 예시

```json
{
  "success": true,
  "message": "손편지 내용을 추출했습니다.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "documentType": "LETTER",
    "frontCandidate": {
      "memoryDate": null,
      "ocrText": "오늘 함께해 줘서 정말 고마워."
    },
    "emptyFields": [
      "memoryDate"
    ],
    "draftStatus": "FRONT_PENDING",
    "nextAction": "CONFIRM_FRONT"
  }
}
```

#### 화면 처리 원칙

- 응답에는 추출 결과만 포함하고 `originalImageUrl`을 포함하지 않습니다.
- 프론트엔드는 추출 결과 확인 화면에 원본 이미지를 함께 표시하지 않습니다.
- `null` 필드는 빈 입력칸으로 보여주며 AI가 만든 추정값으로 채우지 않습니다.
- 사용자는 값이 틀리면 수정하고 빈칸은 직접 입력할 수 있습니다.
- 영수증 품목은 후보로만 표시하며 사용자가 이름·수량을 수정하고 품목을 삭제·추가하거나 선택 해제한 뒤 최종 확정합니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `DOCUMENT_002` | 지원하지 않는 문서 유형 |
| 404 | `DRAFT_001` | 임시 기록을 찾을 수 없음 |
| 409 | `DRAFT_002` | 현재 단계에서 유형을 확정할 수 없음 |
| 403 | `DRAFT_004` | 다른 사용자의 임시 기록 접근 |
| 503 | `AI_001` | 정보 추출 서비스 일시 오류 |

### 5.3 수정된 추출값 확인 및 카드 앞면 확정

`PUT /memory-drafts/{draftId}/front/confirm`

프론트엔드는 AI가 추출한 최초 값이 아니라 사용자가 확인·수정한 최종값을 전송합니다. 이 요청이 성공해야 카드 앞면이 확정됩니다.

#### 영수증 Request

```json
{
  "memoryDate": "2026-07-25",
  "front": {
    "storeName": "서면카페",
    "purchaseItems": [
      {
        "name": "아이스 아메리카노",
        "quantity": 2
      },
      {
        "name": "치즈케이크",
        "quantity": 1
      }
    ]
  }
}
```

#### 티켓 Request

```json
{
  "memoryDate": "2026-07-25",
  "front": {
    "eventName": "흠뻑쇼",
    "venue": "부산아시아드주경기장",
    "seat": null
  }
}
```

#### 손편지 Request

```json
{
  "memoryDate": "2026-03-18",
  "front": {
    "ocrText": "오늘 함께해 줘서 정말 고마워."
  }
}
```

#### 유형별 검증

| 기록 유형 | 필수 | 선택 |
|---|---|---|
| 영수증 | `memoryDate`, `front.storeName`, `front.purchaseItems` | 없음 |
| 티켓 | `memoryDate`, `front.eventName`, `front.venue` | `front.seat` |
| 손편지 | `memoryDate`, `front.ocrText` | 없음 |

좌석이 `null`이면 정상 요청으로 처리하며 완성 카드에서는 좌석 항목명까지 숨깁니다.
영수증의 `purchaseItems`는 빈 배열을 허용합니다. 항목이 있으면 `name`은 비어 있지 않아야 하고 `quantity`는 1 이상의 정수여야 하며, UI의 선택 상태는 보내지 않고 사용자가 최종 선택한 항목만 배열에 담습니다. 신규 클라이언트는 이 필드를 보내야 하며, 기존 클라이언트 호환을 위해 필드가 누락된 요청만 `[]`로 정규화합니다.

#### Response — 영수증 예시

```json
{
  "success": true,
  "message": "카드 앞면이 확정되었습니다.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "documentType": "RECEIPT",
    "memoryDate": "2026-07-25",
    "front": {
      "storeName": "서면카페",
      "purchaseItems": [
        {
          "name": "아이스 아메리카노",
          "quantity": 2
        },
        {
          "name": "치즈케이크",
          "quantity": 1
        }
      ]
    },
    "draftStatus": "FRONT_CONFIRMED",
    "nextAction": "WRITE_BACK"
  }
}
```

#### Response — 티켓 예시

`200 OK`

```json
{
  "success": true,
  "message": "카드 앞면이 확정되었습니다.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "documentType": "TICKET",
    "memoryDate": "2026-07-25",
    "front": {
      "eventName": "흠뻑쇼",
      "venue": "부산아시아드주경기장",
      "seat": null
    },
    "draftStatus": "FRONT_CONFIRMED",
    "nextAction": "WRITE_BACK"
  }
}
```

- 티켓도 영수증·손편지와 동일하게 흰색 카드 배경을 사용합니다.
- 카드 배경색은 이 응답에서 추출하거나 반환하지 않습니다.
- 이 단계에서는 티켓 세부 유형을 추정하거나 반환하지 않습니다. 사용자가 뒷면에서 `AI 질문으로 떠올리기`를 선택했을 때만 6.1 API를 호출합니다.

#### Response — 손편지 본문 확정 예시

```json
{
  "success": true,
  "message": "카드 앞면이 확정되었습니다.",
  "data": {
    "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
    "documentType": "LETTER",
    "memoryDate": "2026-03-18",
    "front": {
      "ocrText": "오늘 함께해 줘서 정말 고마워.",
      "frontImageMode": "BACKGROUND_REMOVED"
    },
    "draftStatus": "FRONT_CONFIRMED",
    "nextAction": "WRITE_BACK"
  }
}
```

`frontImageMode`는 사용자가 보내거나 수정하는 필드가 아닌 기존 처리 메타데이터입니다. 최종 카드 목록·상세 화면은 이 값과 무관하게 `ocrText`만 사용합니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `VALIDATION_001` | 유형별 필수값 누락 또는 날짜 형식 오류 |
| 404 | `DRAFT_001` | 임시 기록을 찾을 수 없음 |
| 409 | `DRAFT_002` | 문서 유형을 확정하지 않은 상태 |
| 403 | `DRAFT_004` | 다른 사용자의 임시 기록 접근 |

---

## 6. 티켓 AI 회상 API

이 API는 다음 조건을 모두 만족할 때만 사용합니다.

- `documentType`이 `TICKET`
- 카드 앞면이 `FRONT_CONFIRMED`
- 사용자가 `AI 질문으로 떠올리기`를 선택함

사용자가 `직접 기록하기`를 선택하면 이 절의 API를 호출하지 않습니다.

### 6.1 티켓 세부 유형 후보 생성

`POST /memory-drafts/{draftId}/ticket-recall/subtype-suggestion`

사용자가 `AI 질문으로 떠올리기`를 선택한 직후 호출합니다. 백엔드는 이미지 분석 단계에서 저장한 Document Parse 결과를 Solar에 전달하고, 티켓을 `CONCERT_PERFORMANCE`, `MOVIE`, `EXHIBITION` 중 하나로 추정합니다.

이 요청에는 Body가 없습니다.

#### Response — 유형을 추정한 경우

`200 OK`

```json
{
  "success": true,
  "message": "티켓 세부 유형을 분석했습니다.",
  "data": {
    "suggestedTicketSubtype": "CONCERT_PERFORMANCE",
    "requiresManualSelection": false,
    "nextAction": "CONFIRM_TICKET_SUBTYPE"
  }
}
```

#### Response — 유형을 확신할 수 없는 경우

`200 OK`

```json
{
  "success": true,
  "message": "티켓 세부 유형을 직접 선택해주세요.",
  "data": {
    "suggestedTicketSubtype": null,
    "requiresManualSelection": true,
    "nextAction": "SELECT_TICKET_SUBTYPE"
  }
}
```

- Solar가 판단하지 못해도 AI 질문 흐름을 중단하지 않고 사용자가 직접 유형을 선택하게 합니다.
- 이 API는 질문을 만들거나 제목을 생성하지 않습니다.
- 앞면 확정 전이거나 티켓이 아닌 임시 기록에는 사용할 수 없습니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 403 | `DRAFT_004` | 다른 사용자의 임시 기록 접근 |
| 404 | `DRAFT_001` | 임시 기록을 찾을 수 없음 |
| 409 | `TICKET_002` | 티켓이 아니거나 앞면이 확정되지 않음 |
| 503 | `AI_002` | Solar 티켓 세부 유형 판단 실패 |

### 6.2 티켓 세부 유형 확정 및 질문 전체 조회

`POST /memory-drafts/{draftId}/ticket-recall/questions`

프론트엔드는 6.1 응답의 `suggestedTicketSubtype`를 보여줍니다. 사용자가 맞다고 확인하거나 다른 유형으로 바꾼 뒤 최종 유형을 요청에 담습니다. 백엔드는 이 값을 해당 `draftId`의 확정된 티켓 세부 유형으로 저장하고, 서버 질문 은행의 질문을 반환합니다.

#### Request

```json
{
  "ticketSubtype": "CONCERT_PERFORMANCE"
}
```

#### Response

`200 OK`

```json
{
  "success": true,
  "message": "회상 질문을 조회했습니다.",
  "data": {
    "ticketSubtype": "CONCERT_PERFORMANCE",
    "questions": [
      {
        "questionId": "CONCERT_PERFORMANCE_1",
        "order": 1,
        "text": "가장 벅찼던 순간은 언제였나요?"
      },
      {
        "questionId": "CONCERT_PERFORMANCE_2",
        "order": 2,
        "text": "그날 떼창하거나 함성을 질렀던 순간이 있나요?"
      },
      {
        "questionId": "CONCERT_PERFORMANCE_3",
        "order": 3,
        "text": "어떤 곡에서 마음이 가장 크게 움직였나요?"
      }
    ]
  }
}
```

이 API는 질문을 두 개만 선택하지 않습니다. 확정된 유형의 질문 세 개를 모두, 정해진 순서대로 반환합니다.

#### 질문 은행

| 티켓 세부 유형 | 질문 |
|---|---|
| `CONCERT_PERFORMANCE` | 가장 벅찼던 순간은 언제였나요?<br>그날 떼창하거나 함성을 질렀던 순간이 있나요?<br>어떤 곡에서 마음이 가장 크게 움직였나요? |
| `MOVIE` | 어떤 계기로 이 영화를 봤나요?<br>가장 마음에 남은 장면이나 대사가 있나요?<br>영화가 끝난 뒤 어떤 이야기를 나눴나요? |
| `EXHIBITION` | 어떤 계기로 이 전시를 보게 되었나요?<br>가장 마음에 남은 작품은 무엇인가요?<br>관람이 끝난 뒤 어떤 이야기를 나눴나요? |

질문 ID는 각 표의 질문 순서대로 `CONCERT_PERFORMANCE_1`~`CONCERT_PERFORMANCE_3`, `MOVIE_1`~`MOVIE_3`, `EXHIBITION_1`~`EXHIBITION_3`을 사용합니다.

질문은 고정 질문 은행에서 가져오므로 이 API에서는 Solar를 호출하지 않습니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `TICKET_001` | 지원하지 않는 티켓 세부 유형 |
| 403 | `DRAFT_004` | 다른 사용자의 임시 기록 접근 |
| 404 | `DRAFT_001` | 임시 기록을 찾을 수 없음 |
| 409 | `TICKET_002` | 티켓이 아니거나 앞면이 확정되지 않음 |

### 6.3 답변을 바탕으로 제목 한 줄 생성

`POST /memory-drafts/{draftId}/ticket-recall/title`

백엔드는 `questionId`를 서버 질문 은행과 대조한 뒤, 고정 질문에 대한 사용자의 답변만을 근거로 Solar가 제목 한 줄을 생성하도록 요청합니다.

#### Request

```json
{
  "answers": [
    {
      "questionId": "CONCERT_PERFORMANCE_1",
      "answer": "마지막 곡을 모두 함께 부르던 순간이 가장 벅찼어요."
    },
    {
      "questionId": "CONCERT_PERFORMANCE_2",
      "answer": "앙코르 때 다 같이 떼창했어요."
    },
    {
      "questionId": "CONCERT_PERFORMANCE_3",
      "answer": "마지막 앙코르곡이 가장 기억에 남아요."
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `answers` | Array | O | 화면에 표시한 질문과 사용자 답변 |
| `answers[].questionId` | String | O | 질문 은행의 질문 ID |
| `answers[].answer` | String 또는 null | 조건부 | 하나 이상의 답변은 비어 있지 않아야 함 |

클라이언트는 화면에 표시된 세 질문의 `questionId`를 각각 한 번씩 보내며, 답하지 않은 질문은 `answer: null`로 보냅니다. 질문 문구는 보내지 않습니다. 백엔드는 `questionId`에 해당하는 고정 질문을 서버 질문 은행에서 조회하고, 임의로 바뀐 질문이 Solar 입력이나 카드 저장에 들어가지 않도록 검증합니다.

#### Response

`200 OK`

```json
{
  "success": true,
  "message": "제목을 생성했습니다.",
  "data": {
    "titleCandidate": "함께 부른 마지막 앙코르"
  }
}
```

#### 생성 규칙

- `titleCandidate` 한 줄만 반환합니다.
- `oneLineMemoryCandidate`, `memoryTextCandidate` 등 별도의 한 줄 추억은 생성하지 않습니다.
- Solar에는 비어 있지 않은 사용자 답변만 사실 근거로 전달합니다.
- 비어 있지 않은 모든 답변의 사실과 의미가 제목 전체에 기여해야 하며 특정 답변을 임의로 버리지 않습니다.
- 여러 답변을 쉼표·가운뎃점·세미콜론·슬래시·막대 기호로 나열하거나 서로 다른 답변의 문구를 그대로 이어 붙이지 않습니다.
- 답변별 핵심어를 차례대로 소개하지 않고 답변 사이의 관계를 연결해 하나의 장면이나 의미를 명사구 또는 짧은 문장으로 종합합니다.
- 나열형 후보를 받으면 원본 답변 전체로 한 번 재생성하며, 재생성 결과도 나열형이면 `503 AI_002`로 처리합니다.
- 제목은 공백을 포함해 8~24자를 권장합니다. 이는 생성 품질을 위한 지침이며 공개 API의 강제 길이 제한은 아닙니다.
- 날짜·장소·티켓 앞면 정보·동행인·날씨·기분은 제목 생성을 위한 추가 사실로 전달하지 않습니다.
- 사용자 답변에 없는 사실·인물·감정을 추가하지 않습니다.
- 답변의 의미를 바꾸거나 감정을 과장하지 않습니다.
- 날짜와 장소를 다시 질문하지 않습니다.
- 프론트엔드는 생성 결과를 수정 가능한 입력칸에 보여주고 사용자가 최종 확정하게 합니다.

Solar 호출이 실패하면 사용자의 답변을 그대로 유지하고 제목을 직접 입력하도록 안내합니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `TICKET_003` | 답변이 모두 비어 있거나 질문 ID가 유형과 맞지 않음 |
| 403 | `DRAFT_004` | 다른 사용자의 임시 기록 접근 |
| 404 | `DRAFT_001` | 임시 기록을 찾을 수 없음 |
| 409 | `TICKET_002` | 티켓이 아니거나 앞면이 확정되지 않음 |
| 503 | `AI_002` | Solar 제목 생성 실패 |

---

## 7. 추억 카드 저장 API

### 7.1 최종 카드 저장

`POST /cards`

사용자가 카드 앞·뒷면을 미리 본 뒤 저장을 누르면 호출합니다. 앞면은 `draftId`에 연결된 사용자 확정값을 사용하고, 요청에는 뒷면의 최종값을 담습니다.

#### Content-Type

`multipart/form-data`

#### Request Parts

| 파트 | 타입 | 필수 | 적용 유형 | 설명 |
|---|---|---:|---|---|
| `card` | JSON 문자열 | O | 전체 | 아래 유형별 카드 데이터 |
| `backPhotos` | File[] | X | 영수증·손편지 | 뒷면에 추가할 사진 |

원본 문서 이미지는 분석 단계에서 이미 `draftId`에 연결했으므로 다시 업로드하지 않습니다.

### 7.2 영수증 카드 Request

```json
{
  "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
  "back": {
    "companions": [
      "민지"
    ],
    "weather": "맑음",
    "mood": "행복",
    "diaryText": "오랜만에 만나서 오래 이야기한 날."
  }
}
```

- `diaryText`와 `backPhotos` 중 하나 이상을 입력합니다.
- 둘을 함께 입력하는 것도 허용합니다.

### 7.3 손편지 카드 Request

```json
{
  "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
  "back": {
    "companions": [
      "지수"
    ],
    "weather": "흐림",
    "mood": "감동",
    "diaryText": "생일에 받은 편지를 다시 읽어 보았다."
  }
}
```

- 영수증과 마찬가지로 `diaryText`와 `backPhotos` 중 하나 이상을 입력합니다.

### 7.4 티켓 직접 기록 Request

```json
{
  "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
  "back": {
    "companions": [
      "현수"
    ],
    "weather": "맑음",
    "mood": "벅참",
    "writingMode": "DIRECT",
    "title": "여름밤의 흠뻑쇼",
    "memoryText": "마지막 앙코르까지 함께 노래했다."
  }
}
```

- `ticketSubtype`을 포함하지 않습니다.
- AI 회상 API를 호출하지 않고 제목과 추억을 사용자가 직접 입력합니다.

### 7.5 티켓 AI 질문 기록 Request

```json
{
  "draftId": "9bb06555-85de-46e2-b44e-8f67eb8e08d2",
  "back": {
    "companions": [
      "현수"
    ],
    "weather": "맑음",
    "mood": "벅참",
    "writingMode": "AI_RECALL",
    "title": "함께 부른 마지막 앙코르",
    "answers": [
      {
        "questionId": "CONCERT_PERFORMANCE_1",
        "answer": "마지막 곡을 모두 함께 부르던 순간이 가장 벅찼어요."
      },
      {
        "questionId": "CONCERT_PERFORMANCE_2",
        "answer": "앙코르 때 다 같이 떼창했어요."
      },
      {
        "questionId": "CONCERT_PERFORMANCE_3",
        "answer": "마지막 앙코르곡이 가장 기억에 남아요."
      }
    ]
  }
}
```

- `title`은 Solar 후보를 사용자가 수정·확정한 최종 제목입니다.
- `ticketSubtype`은 6.2에서 확정해 `draftId`에 저장한 값을 사용하므로 최종 저장 요청에서 다시 보내지 않습니다.
- 별도의 `memoryText`나 `oneLineMemory`를 전송하지 않습니다.
- 요청에는 `questionId`와 `answer`만 포함합니다. 백엔드는 서버 질문 은행의 질문 문구를 함께 저장해 카드 뒷면에서 다시 보여줍니다.
- 세 질문의 서로 다른 `questionId`를 모두 포함해야 하며, 답하지 않은 질문은 `answer: null`로 보냅니다. 단, 하나 이상의 답변은 비어 있지 않아야 합니다.

### 7.6 공통 뒷면 필드

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `draftId` | UUID | O | 앞면이 확정된 임시 기록 ID |
| `back.companions` | String[] | O | 사용자가 직접 입력한 동행인. 혼자라면 `[]` |
| `back.weather` | String | O | 사용자가 직접 선택한 날씨 |
| `back.mood` | String | O | 사용자가 직접 선택한 기분 |

날씨와 기분의 최종 선택지 코드는 아직 정하지 않았습니다. MVP 구현 전에는 프론트엔드와 백엔드가 같은 문자열 목록을 사용하도록 확정해야 합니다.

### 7.7 Response

`201 Created`

```json
{
  "success": true,
  "message": "추억 카드가 저장되었습니다.",
  "data": {
    "cardId": "e89ed42d-1a89-4eea-8ddc-dca90a5c78c4",
    "documentType": "TICKET",
    "printLayout": "LANDSCAPE_TICKET",
    "memoryDate": "2026-07-25",
    "year": 2026,
    "draftStatus": "SAVED"
  }
}
```

`printLayout`은 저장 성공 후 프론트엔드가 실제 종이 출력 연출의 비율을 선택하는 값입니다. DB에 별도 저장하지 않고 확정된 `documentType`에서 계산합니다.

| 문서 유형 | `printLayout` | 출력 모양 |
|---|---|---|
| `RECEIPT` | `NARROW_RECEIPT` | 폭이 좁고 긴 영수증 |
| `TICKET` | `LANDSCAPE_TICKET` | 가로형 티켓과 절취선 |
| `LETTER` | `LETTER_SHEET` | 일반 편지지 비율 |

#### 백엔드 저장 원칙

- 로그인 사용자를 카드 소유자로 저장합니다.
- `draftId`의 사용자 확정 앞면만 사용합니다.
- 실제 추억 날짜 `memoryDate`의 연도 서랍에 저장합니다.
- AI 최초 추출값이 아니라 사용자가 수정·확정한 값을 저장합니다.
- 영수증은 사용자가 최종 선택한 `front.purchaseItems`만 기존 확정 앞면 데이터에 함께 저장하며, 별도 공개 API나 `purchaseItems` 전용 DB 컬럼을 추가하지 않습니다.
- 선택 항목이 `null`이면 카드 조회 응답에서도 `null`로 유지합니다.
- 같은 `draftId`로 두 개의 카드를 생성하지 않습니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `VALIDATION_001` | 유형별 필수 뒷면 값 누락 |
| 403 | `DRAFT_004` | 다른 사용자의 임시 기록 접근 |
| 404 | `DRAFT_001` | 임시 기록을 찾을 수 없음 |
| 409 | `DRAFT_003` | 앞면 미확정 또는 이미 저장된 임시 기록 |
| 413 | `IMAGE_002` | 추가 사진 용량 제한 초과 |
| 415 | `IMAGE_003` | 지원하지 않는 추가 사진 형식 |
| 500 | `CARD_003` | 카드 저장 실패 |

---

## 8. 연도별 서랍 및 카드 조회 API

### 8.1 연도별 서랍 목록 조회

`GET /drawers`

로그인 사용자의 카드가 존재하는 연도를 최근 연도부터 반환합니다.

#### Response

`200 OK`

```json
{
  "success": true,
  "message": "연도별 서랍을 조회했습니다.",
  "data": {
    "drawers": [
      {
        "year": 2026,
        "cardCount": 5
      },
      {
        "year": 2025,
        "cardCount": 3
      }
    ]
  }
}
```

카드가 없으면 다음과 같이 반환합니다.

```json
{
  "success": true,
  "message": "연도별 서랍을 조회했습니다.",
  "data": {
    "drawers": []
  }
}
```

### 8.2 특정 연도의 카드 목록 조회

`GET /drawers/{year}/cards`

선택한 연도의 카드를 실제 추억 날짜가 오래된 것부터 최신인 것 순서로 반환합니다. 프론트엔드는 이 응답 순서를 이용해 같은 연도의 다른 기록을 넘겨봅니다.

#### Path Parameter

| 변수 | 타입 | 설명 |
|---|---|---|
| `year` | Integer | 조회할 실제 추억 연도 |

#### Response

`200 OK`

```json
{
  "success": true,
  "message": "연도별 카드를 조회했습니다.",
  "data": {
    "year": 2026,
    "cards": [
      {
        "cardId": "e10e31cb-9ea1-4aaa-9822-e13358defb03",
        "documentType": "RECEIPT",
        "memoryDate": "2026-04-02",
        "layoutSeed": 1420639217,
        "front": {
          "storeName": "서면카페",
          "purchaseItems": [
            {
              "name": "아이스 아메리카노",
              "quantity": 2
            },
            {
              "name": "치즈케이크",
              "quantity": 1
            }
          ]
        }
      },
      {
        "cardId": "e89ed42d-1a89-4eea-8ddc-dca90a5c78c4",
        "documentType": "TICKET",
        "memoryDate": "2026-07-25",
        "layoutSeed": 958216044,
        "front": {
          "eventName": "흠뻑쇼",
          "venue": "부산아시아드주경기장",
          "seat": null
        }
      }
    ]
  }
}
```

- 해당 연도에 카드가 없으면 `cards: []`을 반환합니다.
- `layoutSeed`는 카드 UUID에서 만든 0 이상의 안정적인 정수입니다. 프론트엔드는 이를 열린 서랍 내부의 위치와 기울기 선택에만 사용하며, 카드 내용·정렬 순서·DB 값에는 영향을 주지 않습니다.
- `seat`가 `null`이면 프론트엔드는 좌석 항목명까지 숨깁니다.
- 같은 연도의 카드는 `memoryDate` 오름차순, 즉 오래된 날짜부터 최신 날짜 순서로 반환합니다.
- 프론트엔드는 업로드 원본 이미지를 사용하지 않고 `documentType`, `memoryDate`, `front`로 문서별 앞면을 재구성합니다. 열린 서랍 이미지의 나무 프레임과 앞판을 침범하지 않도록 카드가 놓이는 영역을 서랍 내부로 제한합니다.
- 서랍에는 최근 카드 최대 10장을 펼치고 전체 `cards` 길이로 총 장수를 표시합니다. 종이를 한 번 누르면 중앙으로 확대하고 한 번 더 누르면 API 12 상세 화면으로 이동합니다.
- 프론트엔드는 각 카드 프레임 아래에 `memoryDate` 기준 경과일 문구를 표시합니다. 날짜 차이는 일광 절약 시간의 영향을 피하도록 날짜 부분을 UTC 자정 값으로 변환해 계산하고, 사용자 기기 날짜를 1분마다 다시 확인합니다. 365일 이상은 365일 단위의 연수와 남은 일수로 나누어 표시합니다.

### 8.3 카드 상세 조회

`GET /cards/{cardId}`

선택한 카드의 앞면과 뒷면 전체 정보를 반환합니다.

#### Response — AI 질문으로 기록한 티켓

`200 OK`

```json
{
  "success": true,
  "message": "카드 상세 정보를 조회했습니다.",
  "data": {
    "cardId": "e89ed42d-1a89-4eea-8ddc-dca90a5c78c4",
    "documentType": "TICKET",
    "memoryDate": "2026-07-25",
    "front": {
      "eventName": "흠뻑쇼",
      "venue": "부산아시아드주경기장",
      "seat": null
    },
    "back": {
      "companions": [
        "현수"
      ],
      "weather": "맑음",
      "mood": "벅참",
      "writingMode": "AI_RECALL",
      "ticketSubtype": "CONCERT_PERFORMANCE",
      "title": "함께 부른 마지막 앙코르",
      "answers": [
        {
          "questionId": "CONCERT_PERFORMANCE_1",
          "question": "가장 벅찼던 순간은 언제였나요?",
          "answer": "마지막 곡을 모두 함께 부르던 순간이 가장 벅찼어요."
        },
        {
          "questionId": "CONCERT_PERFORMANCE_2",
          "question": "그날 떼창하거나 함성을 질렀던 순간이 있나요?",
          "answer": "앙코르 때 다 같이 떼창했어요."
        },
        {
          "questionId": "CONCERT_PERFORMANCE_3",
          "question": "어떤 곡에서 마음이 가장 크게 움직였나요?",
          "answer": "마지막 앙코르곡이 가장 기억에 남아요."
        }
      ]
    }
  }
}
```

#### 기록 유형별 상세 필드

| 기록 유형 | 앞면 | 뒷면 |
|---|---|---|
| 영수증 | `storeName`, `purchaseItems` | `companions`, `weather`, `mood`, `diaryText`, `backPhotoUrls` |
| 티켓 직접 기록 | `eventName`, `venue`, `seat` | 공통 정보, `writingMode`, `title`, `memoryText` |
| 티켓 AI 질문 | 티켓 직접 기록과 동일 | 공통 정보, `writingMode`, `ticketSubtype`, `title`, `answers` |
| 손편지 | `ocrText` | `companions`, `weather`, `mood`, `diaryText`, `backPhotoUrls` |

API 11 목록과 API 12 상세의 영수증 `front.purchaseItems`는 모두 사용자가 API 5에서 최종 확정한 `{ "name", "quantity" }` 배열입니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 403 | `CARD_001` | 다른 사용자의 카드 접근 |
| 404 | `CARD_002` | 카드를 찾을 수 없음 |

### 8.4 저장된 카드 수정

`PUT /cards/{cardId}`

로그인 사용자가 소유한 카드의 날짜와 앞·뒷면 텍스트 정보를 전체 교체합니다. 문서 유형과 티켓 작성 방식은 저장 당시 값으로 유지하며 변경할 수 없습니다.

#### Content-Type

`application/json`

#### 영수증 Request 예시

```json
{
  "memoryDate": "2026-07-25",
  "front": {
    "storeName": "서면카페",
    "purchaseItems": [
      {
        "name": "아이스 아메리카노",
        "quantity": 2
      },
      {
        "name": "치즈케이크",
        "quantity": 1
      }
    ]
  },
  "back": {
    "companions": [
      "민지"
    ],
    "weather": "맑음",
    "mood": "행복",
    "diaryText": "오랜만에 만나서 오래 이야기한 날."
  }
}
```

#### 티켓 직접 기록 Request 예시

```json
{
  "memoryDate": "2026-07-25",
  "front": {
    "eventName": "흠뻑쇼",
    "venue": "부산아시아드주경기장",
    "seat": null
  },
  "back": {
    "companions": [
      "현수"
    ],
    "weather": "맑음",
    "mood": "벅참",
    "writingMode": "DIRECT",
    "title": "여름밤의 흠뻑쇼",
    "memoryText": "마지막 앙코르까지 함께 노래했다."
  }
}
```

#### 수정 규칙

- `memoryDate`, `front`, `back`은 모두 보내야 하는 전체 수정 요청입니다.
- 문서 유형은 기존 카드의 유형을 사용하며 요청으로 변경하지 않습니다.
- 영수증 `front.purchaseItems`는 `{ "name", "quantity" }` 배열이며 빈 배열로 모든 품목을 지울 수 있습니다. 각 이름은 비어 있지 않고 수량은 1 이상의 정수여야 합니다.
- 신규 프론트엔드는 영수증 수정 시 `purchaseItems`를 항상 전송합니다. 기존 클라이언트가 이 필드를 생략한 경우에는 저장된 품목을 유지합니다.
- 손편지는 `front.ocrText`만 수정할 수 있습니다. 업로드 원본은 내부 보관하더라도 카드 목록·상세 화면에는 표시하지 않습니다.
- 티켓은 저장 당시의 `writingMode`를 유지해야 합니다. `AI_RECALL` 카드의 질문 ID는 저장된 `ticketSubtype`의 고정 질문과 다시 검증합니다.
- 영수증·손편지의 기존 뒷면 사진은 유지합니다. 이 API에서는 사진을 추가·교체·삭제하지 않습니다.
- 날짜의 연도가 바뀌면 수정된 연도 서랍에서 조회됩니다.

#### Response

`200 OK`

```json
{
  "success": true,
  "message": "추억 카드가 수정되었습니다.",
  "data": {
    "cardId": "e89ed42d-1a89-4eea-8ddc-dca90a5c78c4",
    "documentType": "RECEIPT",
    "memoryDate": "2026-07-25",
    "year": 2026
  }
}
```

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 400 | `VALIDATION_001` | 유형별 필수값 누락, 허용되지 않은 앞면 필드, 티켓 작성 방식 변경 |
| 403 | `CARD_001` | 다른 사용자의 카드 수정 시도 |
| 404 | `CARD_002` | 카드를 찾을 수 없음 |
| 500 | `CARD_003` | 저장된 카드 데이터 손상 또는 수정 실패 |

### 8.5 저장된 카드 삭제

`DELETE /cards/{cardId}`

로그인 사용자가 소유한 카드를 삭제합니다. DB 삭제가 확정된 뒤 원본 문서 이미지와 뒷면 추가 사진도 함께 정리합니다.

#### Response

`200 OK`

```json
{
  "success": true,
  "message": "추억 카드가 삭제되었습니다.",
  "data": {
    "cardId": "e89ed42d-1a89-4eea-8ddc-dca90a5c78c4"
  }
}
```

삭제 이후 연도별 서랍의 카드 개수와 카드 목록은 별도 캐시 없이 현재 DB 상태를 다시 조회하므로 즉시 반영됩니다.

#### 주요 오류

| HTTP | 코드 | 상황 |
|---:|---|---|
| 403 | `CARD_001` | 다른 사용자의 카드 삭제 시도 |
| 404 | `CARD_002` | 카드를 찾을 수 없음 |
| 500 | `CARD_003` | 저장된 카드 데이터 손상 또는 삭제 실패 |

---

## 9. Upstage API 내부 연동 명세

이 절은 프론트엔드가 호출하는 공개 API를 추가로 정의하는 것이 아니라, 5~7절의 API를 Spring Boot 백엔드에서 구현할 때 지켜야 할 내부 연동 규칙입니다.

### 9.1 단계별 호출 순서

| 앱 단계 | Upstage 호출 | 백엔드 처리 |
|---|---|---|
| 이미지 분석 | Document Parse → Solar | 전체 텍스트·구조를 한 번 인식해 임시 기록에 보관하고, 그 결과로 문서 유형 후보만 생성 |
| 영수증 유형 확정 | Information Extract | 원본 이미지에서 `memoryDate`, `storeName`, `purchaseItems`만 구조화해 추출 |
| 티켓 유형 확정 | Information Extract | `memoryDate`, `eventName`, `venue`, `seat` 스키마로 필요한 필드만 추출 |
| 손편지 유형 확정 | 추가 호출 없음 | 저장해 둔 Document Parse 전체 본문을 `ocrText`로 사용하고 `memoryDate`는 `null` 반환 |
| 카드 앞면 확정 | Upstage 호출 없음 | 사용자가 수정한 값을 확정하고 이미지 처리 결과를 연결 |
| 티켓 직접 기록 | Upstage 호출 없음 | 제목과 추억을 사용자가 직접 작성 |
| 티켓 AI 질문 시작 | Solar | 저장해 둔 Document Parse 결과로 티켓 세부 유형 후보만 생성 |
| 티켓 질문 조회 | Upstage 호출 없음 | 사용자가 확정한 유형의 고정 질문을 서버 질문 은행에서 반환 |
| AI 제목 생성 | Solar | 고정 질문에 대한 비어 있지 않은 사용자 답변만 근거로 제목 한 줄 생성 |
| 카드 저장·조회 | Upstage 호출 없음 | 사용자 확정값과 이미지를 저장하고 `memoryDate`의 연도로 그룹화 |

```mermaid
flowchart TD
    A["이미지 업로드"] --> B["Document Parse"]
    B --> C["Solar 문서 유형 후보"]
    C --> D["사용자 유형 확인"]
    D -->|영수증·티켓| E["Information Extract"]
    D -->|손편지| F["저장된 전체 본문 재사용"]
    E --> G["사용자 추출값 확인"]
    F --> G
```

### 9.2 Document Parse 처리 규칙

- `/memory-drafts/analyze`에서 업로드 이미지를 Document Parse에 한 번만 전송합니다.
- 전체 텍스트와 문서 구조를 백엔드 내부의 `parsedContent`로 정규화해 `draftId`에 연결합니다.
- `parsedContent`는 문서 유형 판단, 손편지 본문, 티켓 세부 유형 판단에 재사용합니다.
- 원본 Upstage 응답과 `parsedContent`는 프론트엔드에 그대로 노출하지 않습니다.
- 흐림·잘림 등으로 유효한 본문을 얻지 못하면 `422 DOCUMENT_001`을 반환합니다.

### 9.3 Information Extract 처리 규칙

Information Extract는 사용자가 문서 유형을 확인한 뒤 영수증 또는 티켓에만 호출합니다.

- 입력은 `parsedContent`가 아니라 `draftId`에 연결해 보관한 원본 문서 이미지입니다.
- Document Parse와 Information Extract는 서로 다른 목적의 독립 API이며, Information Extract가 Document Parse 결과를 입력받는 구조로 구현하지 않습니다.
- 현재 Upstage의 `information-extract` 모델을 `/v1/information-extraction/chat/completions`로 호출하고, 메시지에는 data URL 형식의 원본 이미지 `image_url` 항목 하나만 전달합니다.
- 영수증의 `response_format` JSON Schema는 배열과 중첩 객체를 사용하며, 티켓은 문자열 필드만 사용합니다. 불확실한 문자열은 빈 문자열로 받은 뒤 백엔드가 `null`로 정규화하고, 영수증 품목 배열은 항목별 형식을 다시 검증합니다.

#### 영수증 추출 스키마

```json
{
  "memoryDate": "YYYY-MM-DD 또는 null",
  "storeName": "문자열 또는 null",
  "purchaseItems": [
    {
      "name": "실제 구매하거나 주문한 메뉴·상품명",
      "quantity": 1
    }
  ]
}
```

- 실제 구매·주문 품목만 추출하고 소계, 합계, 부가세, 할인, 쿠폰, 포인트, 결제수단, 카드번호, 승인번호, 주문번호, 사업자정보와 광고 문구는 제외합니다.
- 카페·음식점의 `ICE`, 샷 추가, 포장, 사이즈업 같은 옵션은 기본적으로 제외합니다. 마트 영수증은 실제 상품명을 유지하고 할인 행만 제외합니다.
- 품목의 `name`은 비어 있지 않은 문자열, `quantity`는 1 이상의 정수여야 합니다. 유효한 품목이 없으면 `purchaseItems: []`로 정규화합니다.
- 대표 메뉴를 AI가 자동 확정하지 않으며 품목 추천을 위한 Solar 호출도 추가하지 않습니다. 사용자가 후보를 수정·삭제·추가하고 최종 선택한 항목만 카드 앞면에 저장합니다.

#### 티켓 추출 스키마

```json
{
  "memoryDate": "YYYY-MM-DD 또는 null",
  "eventName": "문자열 또는 null",
  "venue": "문자열 또는 null",
  "seat": "문자열 또는 null"
}
```

- 정의한 필드 외의 값은 저장하거나 프론트엔드에 반환하지 않습니다.
- Upstage 결과를 바로 신뢰하지 않고 백엔드 DTO와 날짜 형식으로 다시 검증합니다.
- 불확실하거나 형식이 맞지 않는 값은 추측하거나 오늘 날짜로 바꾸지 않고 `null`로 정규화합니다.
- 손편지는 전체 본문 보존이 목적이므로 Information Extract를 호출하지 않습니다.

### 9.4 Solar 처리 규칙

Solar 호출 결과는 백엔드가 다음 내부 형식으로 정규화하고 허용된 enum 및 문자열 규칙을 다시 검증합니다.

가능하면 Solar의 Structured Outputs를 이용해 아래 JSON 형식을 강제하고, 백엔드에서도 허용된 enum인지 다시 검증합니다.

#### 문서 유형 판단

```json
{
  "documentType": "RECEIPT | TICKET | LETTER | UNKNOWN"
}
```

- 입력 근거는 Document Parse의 `parsedContent`입니다.
- 허용된 값 외의 결과나 판단이 어려운 경우 `UNKNOWN`으로 처리합니다.
- `UNKNOWN`은 오류가 아니며 `suggestedDocumentType: null`, `requiresManualSelection: true`로 변환합니다.

#### 티켓 세부 유형 판단

```json
{
  "ticketSubtype": "CONCERT_PERFORMANCE | MOVIE | EXHIBITION | UNKNOWN"
}
```

- 사용자가 `AI 질문으로 떠올리기`를 선택해 6.1 API를 호출한 경우에만 실행합니다.
- 입력 근거는 해당 임시 기록의 `parsedContent`입니다.
- `UNKNOWN`은 `suggestedTicketSubtype: null`, `requiresManualSelection: true`로 변환합니다.
- Solar는 질문을 새로 만들지 않으며 질문은 서버 질문 은행에서만 가져옵니다.

#### 제목 한 줄 생성

```json
{
  "titleCandidate": "사용자 답변에 근거한 제목 한 줄"
}
```

- 서버 질문 은행과 일치하는 `questionId`만 허용합니다.
- Solar에는 비어 있지 않은 사용자 답변만 사실 근거로 전달합니다.
- 비어 있지 않은 모든 답변을 반영하되 답변별 핵심어를 구분자로 나열하지 않고, 답변 사이의 관계를 하나의 장면이나 의미로 종합합니다.
- 쉼표·가운뎃점·세미콜론·슬래시·막대 기호가 포함된 나열형 후보는 원본 답변 전체로 한 번 재생성하고, 재생성 결과도 나열형이면 `AI_002`로 처리합니다.
- 공백 포함 8~24자의 명사구 또는 짧은 문장을 권장하되, 이는 생성 프롬프트 지침이며 공개 API의 강제 길이 제한은 아닙니다.
- 답변에 없는 인물·사건·감정·장소를 추가하거나 의미를 과장하지 않습니다.
- 빈 제목, 여러 줄 결과 또는 재생성 후에도 나열형인 결과는 유효하지 않은 응답으로 처리합니다.
- 문서 유형·티켓 세부 유형 분류는 결정적인 결과를 위해 `temperature: 0`, 제목 생성은 제한된 표현 다양성을 위해 `temperature: 0.3`을 사용합니다.
- 생성 결과는 최종값이 아니라 수정 가능한 후보이며, 카드에는 사용자가 확정한 `title`을 저장합니다.

### 9.5 환경 변수와 보안

- Upstage API는 Spring Boot 백엔드에서만 호출합니다.
- API 키는 `UPSTAGE_API_KEY` 환경 변수에서 읽습니다.
- JWT 서명 키는 32자 이상의 `JWT_SECRET` 환경 변수에서 읽습니다.
- 액세스 토큰 만료 초는 `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS` 환경 변수에서 읽습니다.
- 실제 키를 소스 코드, `application.yml`, 예시 JSON, 로그, Git 커밋 또는 GitHub 저장소에 포함하지 않습니다.
- 로컬 개발용 비밀 설정 파일을 사용한다면 반드시 `.gitignore`에 포함합니다.
- 프론트엔드 번들, 브라우저 네트워크 응답, 에러 메시지에 API 키나 Upstage 원본 응답을 노출하지 않습니다.
- 손편지 본문과 사용자의 회상 답변은 일반 애플리케이션 로그에 남기지 않습니다. 로그에는 내부 요청 ID, 성공 여부, 상태 코드, 처리 시간만 기록합니다.

Spring Boot 설정은 실제 키 대신 환경 변수 참조만 둡니다.

```yaml
upstage:
  api-key: ${UPSTAGE_API_KEY}

memory-drawer:
  auth:
    jwt-secret: ${JWT_SECRET}
    access-token-expiration-seconds: ${JWT_ACCESS_TOKEN_EXPIRATION_SECONDS}
```

### 9.6 실패 및 상태 처리

- Document Parse 또는 Information Extract 호출 실패는 `503 AI_001`로 변환합니다.
- Solar의 문서 유형·티켓 세부 유형·제목 생성 호출 실패는 `503 AI_002`로 변환합니다.
- 외부 API 호출과 응답 검증이 모두 성공한 뒤에만 임시 기록 상태를 다음 단계로 변경합니다.
- 실패 시 이미 저장한 사용자 입력과 `draftId`를 유지해 사용자가 같은 단계를 다시 시도할 수 있게 합니다.
- Solar 제목 생성에 실패해도 카드 작성을 중단하지 않고, 사용자가 제목을 직접 입력해 저장할 수 있게 합니다.
- API 3 최초 분석 실패는 아직 성공 응답으로 전달한 `draftId`가 없으므로 이미지와 임시 기록을 저장하지 않고 재업로드 방식으로 재시도합니다.
- Solar 모델은 `solar-pro3`를 사용합니다.
- 외부 요청 연결 제한 시간은 5초, 전체 요청 제한 시간은 30초로 설정합니다.
- Document Parse 1회 호출 규칙을 지키기 위해 서버 자동 재시도 횟수는 0회로 설정합니다.
- Upstage 모델명과 외부 요청 설정은 백엔드 설정 한 곳에서 관리하고 프론트엔드 계약에 포함하지 않습니다.

### 9.7 AI 데이터 사용 원칙

- AI가 추출하거나 생성한 값은 항상 사용자가 확인·수정할 수 있습니다.
- AI 원본 결과와 사용자 확정값을 구분하고 최종 카드에는 사용자 확정값만 저장합니다.
- 영수증 구매 품목은 Information Extract 후보를 그대로 확정하지 않고 사용자의 최종 선택을 거칩니다.
- 동행인·날씨·기분은 AI가 추측하지 않습니다.
- Solar가 질문이나 별도의 한 줄 추억을 새로 생성하지 않습니다.

### 9.8 공식 구현 참고 문서

- [Upstage Document Parse](https://console.upstage.ai/docs/capabilities/parse/document-parsing)
- [Upstage Information Extract](https://console.upstage.ai/docs/capabilities/extract/universal-extraction)
- [Document Parse와 Information Extract 비교](https://www.upstage.ai/blog/en/difference-of-ie-and-dp)
- [Upstage Solar Chat](https://console.upstage.ai/docs/capabilities/generate/chat)
- [Upstage Structured Outputs](https://console.upstage.ai/docs/capabilities/generate/structured-outputs)
- [Upstage Error Codes](https://console.upstage.ai/docs/resources/error-codes)

---

## 10. 주요 오류 코드

| HTTP | 코드 | 의미 |
|---:|---|---|
| 400 | `VALIDATION_001` | 요청값 누락 또는 형식 오류 |
| 401 | `AUTH_001` | 인증 토큰 없음·만료·유효하지 않음 |
| 401 | `AUTH_002` | 로그인 정보 불일치 |
| 409 | `AUTH_003` | 이메일 중복 |
| 400 | `IMAGE_001` | 이미지 파일 누락 |
| 413 | `IMAGE_002` | 이미지 용량 제한 초과 |
| 415 | `IMAGE_003` | 지원하지 않는 이미지 형식 |
| 422 | `DOCUMENT_001` | 문서를 분석할 수 없음 |
| 400 | `DOCUMENT_002` | 지원하지 않는 문서 유형 |
| 404 | `DRAFT_001` | 임시 기록 없음 |
| 409 | `DRAFT_002` | 임시 기록 처리 순서 오류 |
| 409 | `DRAFT_003` | 앞면 미확정 또는 이미 저장된 임시 기록 |
| 403 | `DRAFT_004` | 다른 사용자의 임시 기록 접근 |
| 400 | `TICKET_001` | 지원하지 않는 티켓 세부 유형 |
| 409 | `TICKET_002` | 티켓 AI API 사용 조건 불충족 |
| 400 | `TICKET_003` | 질문 ID 불일치·중복 또는 유효한 답변 없음 |
| 503 | `AI_001` | Document Parse·Information Extract 호출 오류 |
| 503 | `AI_002` | Solar 문서 유형·티켓 세부 유형·제목 생성 오류 |
| 403 | `CARD_001` | 다른 사용자의 카드 접근 |
| 404 | `CARD_002` | 카드 없음 |
| 500 | `CARD_003` | 카드 저장 오류 |

---

## 11. 프론트엔드·백엔드 역할 구분

| 기능 | 프론트엔드 | 백엔드 |
|---|---|---|
| 카메라·앨범 | 촬영·선택, 미리보기, 방향·크기 보정 | 보정된 파일 수신 |
| 문서 유형 카드 | enum에 맞는 프레임 표시, 맞음·아님 입력 | 유형 후보 생성 |
| 추출 결과 확인 | 원본 없이 결과 입력칸만 표시, 수정값 전송 | 확신하는 값만 반환, 불확실하면 `null` |
| 영수증 구매 품목 | 후보의 이름·수량 수정, 추가·삭제·선택 후 최종 항목 전송 | 원본 이미지의 Information Extract 결과를 정제해 `purchaseItems` 후보 반환 |
| 티켓 세부 유형 | AI 질문 선택 후에만 후보 표시·수정 | 해당 시점에만 Solar로 후보 계산하고 최종 선택값 검증 |
| 회상 질문 | 유형별 질문 전체와 입력칸 표시 | 고정 질문 은행 반환 |
| AI 제목 | 후보를 수정 가능한 형태로 표시 | `questionId`를 서버 질문 은행과 검증하고 사용자 답변만 근거로 제목 한 줄 생성 |
| 카드 미리보기 | 세 유형 모두 흰색 배경으로 앞·뒷면 렌더링, 빈 선택 항목 숨김 | 확정 데이터와 이미지 처리 결과 제공, 배경색 값은 저장·반환하지 않음 |
| 최종 저장 | 최종 뒷면 데이터와 사진 전송 | 카드 저장 및 실제 날짜의 연도 서랍 배치 |
| 서랍 화면 | 열린 서랍 내부에 최근 카드 최대 10장의 앞면을 안정적으로 펼치고 선택 카드를 확대·상세 이동 | 사용자별 연도, 확정 앞면 데이터와 카드별 `layoutSeed` 반환 |
| 경과일 표시 | 기기 로컬 날짜로 `memoryDate`와의 날짜 차이를 계산하고 1분마다 갱신 | 별도 시간·경과일 필드를 제공하지 않음 |

---

## 12. 구현 전 확정할 TODO

기능을 새로 추가하는 항목이 아니라, 현재 기능을 구현하기 위해 팀이 정해야 하는 기술 세부사항입니다.

### 12.1 이번에 확정된 항목

- 비밀번호는 1자 이상 10자 이하로 제한합니다.
- 공백만 입력한 비밀번호는 허용하지 않습니다.
- 대문자·소문자·숫자·특수문자 조합 규칙은 두지 않습니다.

### 12.2 남은 TODO

- 액세스 토큰 만료 시간과 재로그인 정책
- 추가 사진의 최대 개수와 파일당 용량
- 동행인 이름 길이와 최대 인원
- 날씨·기분 선택지와 실제 enum 코드
- 제목, 추억, 일기, 질문 답변의 최대 글자 수
- 이미지 URL의 만료·갱신 방식
- 카드 앞면을 합성 이미지로 저장할지 데이터와 원본으로 매번 렌더링할지

API 3 관련 이미지 형식·용량·HEIC 처리·임시 보관·이미지 접근·Solar 모델·외부 요청 설정은 5.1절과 9.5~9.6절의 확정값을 사용합니다.

---

## 13. 명시적 제외 범위

다음 기능은 최신 README에 없으므로 API를 만들지 않습니다.

- 로그아웃·내 정보·프로필
- 카드 검색·필터·공유
- 서랍 생성·이름 변경·삭제
- 음식·풍경·영상·음성 기록
- 영수증·손편지 AI 회상 질문
- Solar를 이용한 영수증 대표 품목 자동 확정
- 영수증 가격·지도·위치·스티커
- 카드 배경색 추출·선택·변경 및 관련 API 필드
- 티켓 직접 기록 시 세부 유형 분류
- 질문 두 개만 선택하는 로직
- AI가 만드는 별도의 한 줄 추억
