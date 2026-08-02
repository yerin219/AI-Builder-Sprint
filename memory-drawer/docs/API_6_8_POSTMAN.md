# API 6·7·8 Postman 테스트 안내

## 현재 상태

API 4·5 병합본을 기준으로 API 6·7·8 Controller·DB 연동을 완료했습니다. 2026-07-31에 로컬 MySQL과 실제 Upstage API를 사용해 티켓 AI 회상, DIRECT·AI_RECALL 카드 저장, 서랍·목록·상세 조회까지 성공했습니다.

## Import

Postman에서 `Import`를 누르고 `postman/Memory Drawer API 6-8.postman_collection.json` 파일을 선택합니다.

컬렉션 변수에는 비밀값이 들어 있지 않습니다. 다음 값은 각자 로컬에서 채웁니다.

| 변수 | 넣을 값 |
|---|---|
| `accessToken` | API 2 로그인 응답의 `data.accessToken` |
| `ticketAiDraftId` | API 3 → 4 TICKET → 5를 거친 AI 회상용 `FRONT_CONFIRMED` draftId |
| `ticketDirectDraftId` | API 3 → 4 TICKET → 5를 거친 직접 기록용 별도 draftId |
| `receiptDraftId` | API 3 → 4 RECEIPT → 5를 거친 draftId |
| `letterDraftId` | API 3 → 4 LETTER → 5를 거친 draftId |

`baseUrl`은 기본값 `http://localhost:8080/api`를 사용합니다.

## 실행 전 준비

1. MySQL과 백엔드 서버를 실행합니다.
2. API 1 회원가입과 API 2 로그인을 실행합니다.
3. 로그인 응답의 `data.accessToken`을 컬렉션 변수 `accessToken`에 넣습니다. `Bearer`라는 단어는 직접 넣지 않습니다.
4. API 3·4·5를 이용해 용도별 `FRONT_CONFIRMED` draftId를 만듭니다.

API 7에서 저장에 성공하면 해당 draft는 `SAVED`가 됩니다. 따라서 티켓 DIRECT와 AI_RECALL에는 서로 다른 draftId가 필요합니다.

## 권장 실행 순서

### 티켓 AI 회상

```text
6.1 티켓 세부 유형 후보 생성
→ 6.2 티켓 세부 유형 확정 및 질문 조회
→ 6.3 답변으로 제목 생성
→ 7.4 티켓 AI 회상 저장
→ 8.1 서랍 목록 조회
→ 8.2 특정 연도 카드 목록 조회
→ 8.3 카드 상세 조회
```

6.1에서 Solar가 유형을 확신하지 못하면 `ticketSubtype`을 `CONCERT_PERFORMANCE`, `MOVIE`, `EXHIBITION` 중 하나로 직접 선택한 뒤 6.2를 실행합니다.

### 나머지 저장 방식

- 영수증: 7.1
- 손편지: 7.2
- 티켓 직접 기록: 7.3

영수증·손편지는 `diaryText`만 보내는 예시입니다. `backPhotos` 파일 업로드는 지원하지만 사진 개수 제한과 파일 조회 방식은 공통 TODO 확정 뒤 컬렉션 예시에 추가합니다.

## 주의

- `UPSTAGE_API_KEY`, DB 비밀번호, JWT 비밀키는 컬렉션 파일에 넣지 않습니다.
- API 7은 `multipart/form-data`입니다. `card` 파트는 `application/json` JSON이며, 티켓에는 `backPhotos`를 보내지 않습니다.
- AI_RECALL 저장 요청에는 `ticketSubtype`, 질문 문구, `memoryText`를 넣지 않습니다.
- API 8의 같은 연도 카드는 `memoryDate` 오래된 순으로 반환됩니다.
