# 기억서랍 (Memory Drawer)

> 영수증, 티켓, 손편지에 남은 정보를 AI로 읽고 사용자의 확인과 감정을 더해 추억 카드와 연도별 서랍으로 보관하는 모바일 웹 서비스

AI Builder Sprint 2026 예선 제출용 저장소입니다. 이 프로젝트는 별도 배포 주소 없이 **로컬 기동 방식**으로 제출합니다. 심사 시에는 아래 안내에 따라 프론트엔드와 백엔드를 실행해 주세요.

## 문서 바로가기

| 문서 | 내용 |
|---|---|
| [프로젝트 상세 설명](./memory-drawer/README.md) | 문제 정의, 사용자 흐름, 주요 기능, 예외 처리, 기술 구조 |
| [AI 활용 증빙](./memory-drawer/docs/AI_USAGE.md) | 사용 모델·도구, 프롬프트 요약, 팀원 검토, 테스트와 작업 기록 |
| [API 명세](./memory-drawer/docs/API_SPEC.md) | 엔드포인트, 요청·응답, 상태 전이, 오류 코드, Upstage 연동 |
| [에이전트 작업 지침](./memory-drawer/AGENTS.md) | 개발 원칙, 보안·Git·검증 규칙 |

## 1. 문제와 해결 방법

종이 영수증, 티켓, 손편지는 시간이 지나면 훼손되거나 잃어버리기 쉽고, 날짜와 가격만 저장하면 그날의 사람과 감정은 남기기 어렵습니다. 기억서랍은 종이 문서를 AI로 인식하되 AI가 기억을 대신 만들지 않도록 설계했습니다.

1. 사용자가 영수증, 티켓 또는 손편지 이미지를 선택합니다.
2. AI가 문서 전체를 인식하고 문서 유형과 필요한 정보를 후보로 제안합니다.
3. 사용자가 유형과 추출값을 확인하고 직접 수정합니다.
4. 동행인, 날씨, 기분과 자유 기록 또는 사진을 사용자가 작성합니다.
5. 사용자 확정값만 추억 카드로 저장하고 실제 추억 날짜의 연도별 서랍에서 다시 봅니다.

## 2. 핵심 기능

- 회원가입과 로그인, JWT 기반 사용자별 데이터 분리
- JPEG, PNG, WebP 이미지 업로드 및 최대 10MB 검증
- 영수증, 티켓, 손편지 문서 유형 후보 제안과 수동 변경
- 영수증의 날짜, 가게명, 구매 품목 후보 추출·수정·선택
- 티켓의 날짜, 행사명, 장소, 좌석 후보 추출·수정
- 손편지 OCR 본문 확인·수정과 조건부 배경 제거
- 티켓의 직접 기록 또는 선택적 AI 회상 질문
- 사용자 답변만 근거로 한 수정 가능한 제목 후보 생성
- 카드 저장, 연도별 서랍 조회, 상세 조회, 수정과 삭제
- AI 실패, 잘못된 파일, 권한 오류와 잘못된 처리 순서에 대한 예외 처리

## 3. AI 활용

### 3.1 서비스 기능

| 모델·API | 사용 위치 | 역할 | 사용자 통제 |
|---|---|---|---|
| Upstage Document Parse | 이미지 최초 분석 | 문서 전체 텍스트와 구조 인식 | 인식 결과를 바로 저장하지 않음 |
| Upstage Solar `solar-pro3` | 문서 유형, 티켓 세부 유형, 티켓 제목 후보 | 허용된 유형 후보와 사용자 답변 기반 제목 생성 | 유형과 제목을 사용자가 확인·수정 |
| Upstage Information Extract | 영수증·티켓 유형 확정 후 | 확정 유형에 필요한 필드만 구조화해 추출 | 사용자가 추출값과 구매 품목을 최종 확정 |

AI는 동행인, 날씨, 기분이나 사용자 답변에 없는 기억을 추측하지 않습니다. 최종 카드에는 AI의 최초 결과가 아니라 **사용자가 확인·수정한 값**만 저장합니다.

### 3.2 개발 과정

Codex를 요구사항 분석, 코드·테스트 제안, 오류 원인 분석, UI 개선과 문서화 보조에 활용했습니다. 팀원은 API 계약, 상태 코드, 화면 정책, 데이터·보안 정책을 직접 결정하고 AI 제안을 검토한 뒤 반영했습니다.

팀원별 활용 과정, 프롬프트 요약, AI 제안과 팀원 결정, 검증 결과 및 관련 PR은 [AI 활용 증빙](./memory-drawer/docs/AI_USAGE.md)에 기록했습니다.

## 4. 실행 및 배포 환경

이 프로젝트는 AWS, Supabase 등 외부 클라우드 인프라를 사용하지 않으며 배포 주소가 없습니다.

| 구분 | 환경 |
|---|---|
| 애플리케이션 형태 | 모바일 우선 웹 서비스 |
| Frontend | React, JavaScript, Vite, Node.js 24 LTS, npm |
| Backend | Java 21, Spring Boot 3.5.16, Gradle Wrapper |
| Database | 로컬 MySQL 8.4 |
| 파일 저장 | 백엔드 실행 환경의 로컬 파일 시스템 |
| 외부 서비스 | Upstage Document Parse, Information Extract, Solar |
| Frontend 주소 | `http://localhost:5173` |
| Backend Base URL | `http://localhost:8080/api` |

Upstage API 키는 대회 운영진이 보유하고 있으므로 저장소나 별도 메일에 포함하지 않았습니다. 실행 시 운영진이 보유한 키를 `UPSTAGE_API_KEY` 환경변수로 설정해야 합니다.

## 5. 로컬 기동 실행 가이드

### 5.1 사전 요구사항

- Git
- Java 21
- Node.js 24 LTS와 npm
- MySQL 8.4
- Upstage API Key
- npm·Gradle 의존성과 Upstage API 호출을 위한 인터넷 연결

Spring Boot와 Gradle을 전역으로 설치할 필요는 없습니다. 저장소에 포함된 Gradle Wrapper를 사용합니다.

### 5.2 저장소 복제

```bash
git clone https://github.com/yerin219/AI-Builder-Sprint.git
cd AI-Builder-Sprint/memory-drawer
```

### 5.3 MySQL 준비

MySQL 서버를 실행한 뒤 다음 데이터베이스를 생성합니다.

```sql
CREATE DATABASE memory_drawer
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

테이블은 백엔드 시작 시 `backend/src/main/resources/schema.sql`을 통해 초기화됩니다.

### 5.4 백엔드 환경변수 설정

예시 변수는 [`backend/.env.example`](./memory-drawer/backend/.env.example)에 있습니다. 이 파일은 참고용이며 Spring Boot가 자동으로 읽지 않으므로, 같은 터미널 세션이나 IDE 실행 설정에 환경변수를 직접 지정해야 합니다.

Windows PowerShell:

```powershell
cd backend
$env:DB_URL="jdbc:mysql://localhost:3306/memory_drawer"
$env:DB_USERNAME="your_mysql_username"
$env:DB_PASSWORD="your_mysql_password"
$env:UPSTAGE_API_KEY="your_upstage_api_key"
$env:JWT_SECRET="replace_with_at_least_32_random_characters"
$env:JWT_ACCESS_TOKEN_EXPIRATION_SECONDS="3600"
$env:MEMORY_DRAWER_STORAGE_ROOT="./data/memory-drawer"
```

macOS/Linux:

```bash
cd backend
export DB_URL='jdbc:mysql://localhost:3306/memory_drawer'
export DB_USERNAME='your_mysql_username'
export DB_PASSWORD='your_mysql_password'
export UPSTAGE_API_KEY='your_upstage_api_key'
export JWT_SECRET='replace_with_at_least_32_random_characters'
export JWT_ACCESS_TOKEN_EXPIRATION_SECONDS='3600'
export MEMORY_DRAWER_STORAGE_ROOT='./data/memory-drawer'
```

### 5.5 백엔드 실행

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

정상 실행 주소는 `http://localhost:8080/api`입니다.

### 5.6 프론트엔드 실행

새 터미널에서 저장소의 `memory-drawer/frontend`로 이동합니다.

```bash
cd frontend
npm ci
npm run dev
```

기본 `VITE_API_BASE_URL`은 `/api`이며 Vite 개발 서버가 이를 `http://localhost:8080`으로 프록시합니다. 브라우저에서 `http://localhost:5173`에 접속합니다.

## 6. 환경변수 정보

| 변수 | 필수 | 설명 | 예시·기본값 |
|---|:---:|---|---|
| `DB_URL` | O | MySQL JDBC URL | `jdbc:mysql://localhost:3306/memory_drawer` |
| `DB_USERNAME` | O | 로컬 MySQL 사용자명 | `root` 등 로컬 계정 |
| `DB_PASSWORD` | O | 로컬 MySQL 비밀번호 | 실제 값은 Git에 저장하지 않음 |
| `UPSTAGE_API_KEY` | O | Upstage API 인증 키 | 운영진 보유 키 사용 |
| `JWT_SECRET` | O | JWT HS256 서명 키 | 32자 이상의 임의 문자열 |
| `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS` | O | 액세스 토큰 만료 시간(초) | `3600` |
| `MEMORY_DRAWER_STORAGE_ROOT` | 선택 | 업로드·파생 이미지 저장 경로 | `./data/memory-drawer` |
| `VITE_API_BASE_URL` | 선택 | 프론트엔드 API 기본 경로 | `/api` |

실제 API 키, DB 비밀번호, JWT 비밀값, 토큰과 개인정보는 저장소·문서·로그에 포함하지 않습니다.

## 7. 재현 시나리오

1. `http://localhost:5173`에서 회원가입 후 로그인합니다.
2. 모바일에서는 카메라 촬영 또는 앨범 선택을 사용하고, PC에서는 이미지 파일을 선택합니다.
3. 영수증, 티켓 또는 손편지 이미지를 업로드합니다.
4. AI가 제안한 문서 유형과 추출값을 확인하고 필요한 내용을 수정합니다.
5. 동행인, 날씨, 기분과 자유 기록 또는 사진을 입력합니다.
6. 티켓은 직접 기록하거나 AI 질문으로 떠올리기를 선택합니다.
7. 최종 미리보기에서 내용을 확인하고 저장합니다.
8. 연도별 서랍에서 카드를 열고 상세 조회·수정·삭제를 확인합니다.

테스트 계정은 필요하지 않습니다. 로컬 실행 후 회원가입 화면에서 새 계정을 생성할 수 있습니다.

## 8. 테스트 및 검증

Backend - Windows:

```powershell
cd memory-drawer/backend
.\gradlew.bat clean test
.\gradlew.bat build
```

Backend - macOS/Linux:

```bash
cd memory-drawer/backend
./gradlew clean test
./gradlew build
```

Frontend:

```bash
cd memory-drawer/frontend
npm ci
npm test
npm run lint
npm run build
```

전체 사용자 흐름과 API 요청·응답은 [프로젝트 상세 설명](./memory-drawer/README.md)과 [API 명세](./memory-drawer/docs/API_SPEC.md)를 참고해 재현할 수 있습니다.
