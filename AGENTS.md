# 용인 행사 축제 서비스 백엔드

용인시의 행사와 축제를 문장이나 상황 칩으로 검색하고, 행사 상세를 확인한 뒤 하루 일정으로 짜 주는 서비스의 백엔드다. 기획 원본은 명세서01(Notion)과 기능명세서이고, 저장소 밖에 있다.

## 문서

작업 전에 관련 문서를 먼저 읽을 것.

1. [docs/architecture.md](docs/architecture.md): 현재 구조 (한눈에 보기, 구조, API, 응답 형식, 권한, 인증 흐름, Redis 키, 데이터 모델, 에러 코드, 설정, 로컬 실행, 알려진 이슈)
2. [PROGRESS.md](PROGRESS.md): S5 하루 일정, S6 내 코스(`itinerary`, `course`)의 세부 동작과 결정
3. [docs/development-guide.md](docs/development-guide.md): 개발 규칙, 새 기능 추가 체크리스트, 문서 갱신 규칙
4. [docs/commit-convention.md](docs/commit-convention.md): 커밋 메시지 컨벤션 (태그, 형식, 작성 규칙)
5. [docs/github-convention.md](docs/github-convention.md): 브랜치 전략, Issue 및 PR 작성 가이드 (템플릿은 `.github/`)
6. [docs/sinhyeong-api-plan.md](docs/sinhyeong-api-plan.md): 신형 담당 API(E1 to E5, E14, E19, E20) 구현 기록, 결정, 엑셀 적재 규칙
7. [README.md](README.md): 로컬 실행, 행사 데이터 넣기, 인증 API

기능을 구현할 때는 명세서01의 기능 ID를 먼저 확인한다.

## 문서 갱신 (필수)

1. 기능을 추가하거나 동작을 바꾸면 같은 작업 안에서 `docs/architecture.md`를 갱신한다. S5, S6 쪽이면 `PROGRESS.md`도 갱신한다. 무엇을 어디에 고칠지는 [development-guide.md의 문서 갱신 규칙](docs/development-guide.md#문서-갱신-규칙)을 따른다.
2. 작업 방식이나 규칙이 바뀌면 이 파일(`AGENTS.md`)이나 `docs/development-guide.md`를 갱신한다.
3. 새 문서를 `docs/`에 추가하면 위 문서 목록에도 추가한다.
4. `CLAUDE.md`는 `@AGENTS.md`만 담는다. Claude와 Codex가 같은 지시를 읽도록 에이전트 지시는 항상 이 파일에 쓴다.

## 스택

Java 21, Spring Boot 4.1, Gradle, MariaDB, Redis, JWT 인증 (패키지 `com.yongyongthon.be`). 테스트는 H2.

1. `./gradlew compileJava`: 컴파일 확인. 변경 후 기본 검증 수단이다
2. `./gradlew test`: 전체 테스트. `BeApplicationTests.contextLoads`는 실제 MariaDB와 Redis가 있어야 통과하고, 나머지는 H2로 돈다
3. `./gradlew bootRun`: 로컬 실행. 먼저 `.env.example`을 `.env`로 복사하고 MariaDB와 Redis를 띄운 뒤 행사 데이터를 넣는다 (README)

1. 앱 DB는 MariaDB다. 행사 데이터의 원본은 팀 행사 DB(GCP PostgreSQL)이고, `scripts/seed_events.py`로 MariaDB `events`에 옮겨 넣는다. 앱이 PostgreSQL에 직접 붙지 않는다
2. 행사 DB 접속 정보(비밀번호 포함)는 저장소 밖에 둔다. 내용을 읽어 출력하거나 문서, 커밋에 옮기지 않는다
3. 경로는 명세서01과 같은 `/api/...`이고 인증 API만 `/api/v1/auth`다. 오류 코드는 명세서01의 E 코드, 공통 오류는 C 코드다

## 권한

1. 역할은 `ADMIN`, `USER` 두 가지이고 authority 문자열은 `ROLE_ADMIN`, `ROLE_USER`다
2. 회원가입 시 항상 `USER`가 부여된다. 요청으로 역할을 고를 수 없다
3. `RoleHierarchy`는 없다. ADMIN이 USER 권한을 자동으로 포함하지 않는다
4. 일정, 공유, 내 코스, 캘린더는 로그인 없이 쓰는 데모 API다. 관심 행사(`/api/me/favorites`)는 로그인이 필요하다

## 코드 규칙

자세한 내용은 [docs/development-guide.md](docs/development-guide.md)를 본다.

1. 컨트롤러는 `ResponseEntity`가 아니라 `GlobalResponse<T>`를 직접 반환하고, 서비스가 `GlobalResponse.ok(...)`로 감싼다
2. 모든 비즈니스 예외는 `GlobalException`을 상속한다. `RuntimeException`을 직접 상속하면 C500으로 떨어진다
3. 컨트롤러 메서드에는 `@Operation`과, 오류를 낼 수 있으면 `@ApiErrorCodes`를 붙인다 (Swagger 오류 응답 예시)
4. 새 도메인은 `add-domain` 스크립트(`.claude/skills/spring-starter`)로 만들고, 자리표시자 필드 `name`, `description`, `createdAt`을 실제 필드로 바꾼다
5. 새 보안 매처는 `SecurityConfiguration`에서 `.anyRequest().authenticated()` 앞에 넣는다. 공개 목록에 `/api/me/**` 전체를 넣으면 관심 행사까지 열리니 경로를 하나씩 적는다
6. 새 환경변수는 `application*.yml`과 `.env.example`에 함께 추가한다. prod 프로필에는 기본값을 두지 않는다
7. 엔티티를 바꾸면 `db/schema.sql`도 같이 고친다 (prod는 `ddl-auto: validate`)
8. 로그는 `[메서드명] 한국어 설명: {}` 형식이고 토큰, 비밀번호 원문은 남기지 않는다

## Git

1. 작업 브랜치는 `dev`다 (팀 기준). 작업은 `dev`에 계속 병합하고, 마지막에 `dev`에서 `main`으로 PR을 올려 반영한다. 기능 PR의 base는 항상 `dev`다. GitHub 기본 브랜치가 `main`이라 PR을 만들 때 base를 직접 바꾼다 (자세한 내용은 [docs/github-convention.md](docs/github-convention.md))
2. 커밋과 푸시는 사용자가 직접 한다. 에이전트는 git add, commit, push를 하지 않는다
3. 커밋 메시지를 쓰거나 제안할 때는 반드시 [docs/commit-convention.md](docs/commit-convention.md)를 따른다
4. Issue나 PR을 작성하거나 초안을 제안할 때는 [docs/github-convention.md](docs/github-convention.md)를 따른다
5. 커밋 메시지와 PR 본문에 `Co-Authored-By`나 "Generated with ..." 같은 AI 표기를 절대 넣지 않는다. 에이전트 기본 템플릿에 있어도 제거한다

## 작성 규칙 (모든 출력)

채팅 답변, 설명, 요약, 문서, 표, 제목, 목록, 주석 등 직접 작성하는 모든 텍스트에 항상 적용한다.

1. 가운데점(U+00B7, U+318D)을 쓰지 않는다. 쉼표, '및', '와', '과'로 바꾼다.
2. 긴 대시(U+2014, U+2013)를 쓰지 않는다. 마침표, 쉼표, 괄호로 바꾸거나 문장을 나눈다.
3. 화살표 기호(U+2192, U+2794) 등의 특수 문자를 쓰지 않는다.
4. 목록은 번호 목록을 우선 사용한다.
5. 문단은 내용이 바뀌는 지점에서 명확하게 나누고 문단 사이에 빈 줄을 넣는다.
6. 출력 전에 금지한 기호가 있는지 확인하고, 있으면 고친다. 이 규칙을 지켰다는 설명은 덧붙이지 않는다.
