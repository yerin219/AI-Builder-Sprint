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
- 관련 브랜치: `feature/fe-api3-5-integration`. 커밋·push·PR은 진행하지 않음.
