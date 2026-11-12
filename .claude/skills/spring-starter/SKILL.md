---
name: spring-starter
description: "Spring Boot 프로젝트에 JWT 인증 보일러플레이트(회원가입/로그인/로그아웃/토큰 재발급, Spring Security, Redis 리프레시 토큰 + 블랙리스트, GlobalResponse 응답 래퍼, ErrorCode, 예외 핸들러, Swagger)를 통째로 깔거나, 기존 프로젝트에 같은 레이어링을 따르는 새 도메인 패키지를 추가할 때 사용한다. \"스프링 부트 새 프로젝트 만들어줘\", \"초기 세팅 해줘\", \"JWT 로그인 붙여줘\", \"회원가입/로그인 구현해줘\", \"order 도메인 추가해줘\", \"컨트롤러/서비스/레포지토리 만들어줘\" 처럼 요청하면 반드시 이 스킬을 쓸 것. Also triggers in English: scaffold or bootstrap a Spring Boot project, set up JWT auth, add signup/signin/logout/token reissue, generate a new domain package (controller/service/entity/repository/DTO). Java 21 + Spring Boot 4.1 + Gradle + MariaDB + Redis 스택 전용.\n"
---

# Spring Boot JWT 보일러플레이트 스캐폴딩

이 스킬은 검증된 레퍼런스 프로젝트의 **실제 소스**를 `assets/template/`에 토큰화해서 들고 있다.
Java를 새로 써내는 게 아니라 복사 + 치환하므로, 결과물은 컴파일이 확인된 코드 그대로다.
직접 파일을 하나씩 타이핑하지 말고 **항상 스크립트를 먼저 돌려라.**

```
~/.claude/skills/spring-starter/
  SKILL.md
  scripts/scaffold.py          <- 진입점
  assets/template/core/        <- common + security + user + gradle + yml
  assets/template/domain/      <- owned / standalone 도메인 변형
  references/conventions.md    <- 손으로 코드를 더할 때 지켜야 할 규칙
```

## 무엇이 깔리는가

- `common/response` — `GlobalResponse<T>{data, error}` 래퍼, `ErrorCode` enum, `GlobalException` 추상 클래스, `@RestControllerAdvice`
- `common/security` — `JwtTokenProvider`(AT/RT 발급, Redis에 RT 저장, 로그아웃 시 블랙리스트), `OncePerRequestFilter`, EntryPoint/AccessDeniedHandler, `SecurityUtil.getUsername()`
- `common/redis`, `common/swagger` — RedisTemplate 설정 + bearerAuth OpenAPI 설정
- `user` — 회원가입·로그인·로그아웃·토큰 재발급 (`/api/v1/auth/**`), `User implements UserDetails`, `UserRole`
  - `POST /api/v1/auth/reissue` `{refreshToken}` — RT 서명·만료·블랙리스트 검사 후 Redis에 저장된 RT와 대조, 통과하면 AT+RT를 새로 발급(RT 로테이션, 이전 RT는 즉시 무효). `permitAll` 경로.
  - 로그인·회원가입·재발급 응답(`TokenResponseDto`)은 `accessToken`, `refreshToken`, `email`, `name`을 함께 준다. 응답 조립은 `AuthService.createTokenResponse(user, token)` 한 곳에서만 한다 — 필드를 늘릴 땐 여기만 고쳐라.
  - `UserRole` 상수, 가입 시 권한 결정 방식, `RoleHierarchy`는 `init`의 권한 옵션으로 정한다 (아래 "권한 옵션").
- `application.yml` + `-local` + `-prod` 프로필, Gradle 래퍼, build.gradle
- `.env.example`: 모든 환경변수와 로컬 기본값. `application.yml`의 `spring.config.import: optional:file:.env[.properties]`가 로컬 `.env`를 읽고, `.env`는 `.gitignore`에 들어간다. OS 환경변수가 항상 우선한다.
- 앱 클래스 `main()` 첫 줄에서 `TimeZone.setDefault(...)`로 JVM 기본 시간대를 고정한다 (기본 `Asia/Seoul`). graft 모드에서도 기존 앱 클래스에 이 줄만 끼워 넣고, 이미 `TimeZone.setDefault`가 있으면 건드리지 않는다.

## 1. 프로젝트 초기 구축

```bash
python3 ~/.claude/skills/spring-starter/scripts/scaffold.py init \
  --target <프로젝트_디렉토리> --package <베이스_패키지> --name <프로젝트명>
```

모드는 **자동으로 판별**된다.

| 대상 디렉토리 | 모드 | 동작 |
|---|---|---|
| 비어 있음 | `full` | gradlew·build.gradle·settings.gradle 포함해 전부 생성 |
| `build.gradle` 있음 (Spring Initializr 결과물 등) | `graft` | 자바 소스와 yml만 얹고, 빠진 의존성만 `build.gradle`에 추가 |

`graft` 모드에서는 **기존 패키지가 항상 이긴다.** 앱 클래스(`@SpringBootApplication`)에서 패키지를 읽어 쓰므로
`--package`는 넘기지 않아도 되고, 넘겼는데 기존과 다르면 스크립트가 거부한다. 기존 앱 클래스와 테스트 클래스는 건드리지 않는다.

옵션:

- `--with-demo` — 예제 CRUD 도메인(`product`)까지 생성. 구조 참고용 샘플이 필요할 때만. 기본은 미포함.
- `--demo-name <이름>` — 예제 도메인 이름 변경
- `--timezone <ID>`: JVM 기본 시간대 (기본 `Asia/Seoul`, 예: `UTC`)
- `--group`, `--title` — Gradle group(기본: 패키지에서 마지막 세그먼트 제거), Swagger 제목(기본: 프로젝트명에서 유도)
- `--force` — 충돌 파일 덮어쓰기

### 권한 옵션

| 옵션 | 기본값 | 동작 |
|---|---|---|
| `--roles A,B,C` | `ADMIN,USER` | `UserRole` enum 상수. `ROLE_` 접두사 없이 대문자로 쓴다 |
| `--default-role X` | `--roles`에 `USER`가 있으면 `USER` | 회원가입 시 자동으로 부여할 권한 (`User.role`의 `@Builder.Default`) |
| `--role-from-request` | 꺼짐 | 기본 권한 대신 `SignUpRequestDto.role` (`@NotNull`)을 받아 그대로 저장 |
| `--role-hierarchy 'A>B,A>C'` | 없음 | `RoleHierarchy` 빈 생성. `A>B>C`처럼 체인도 된다. 적히지 않은 권한끼리는 서로 포함하지 않는다 |

1. `--roles`에 `USER`가 없으면 `--default-role`과 `--role-from-request` 중 하나를 반드시 줘야 한다. 둘 다 없거나 둘 다 주면 스크립트가 거부한다.
2. `--role-from-request`를 쓰면 요청이 보낸 권한을 그대로 저장하므로 ADMIN 가입도 가능하다. 스크립트가 경고를 남긴다. 막아야 하면 `AuthService.signUp`에서 거른다.
3. `--with-demo`의 예제 도메인 권한은 기본 권한, 없으면 ADMIN을 뺀 모든 권한(`hasAnyRole`)으로 잡힌다.

예시 (주인과 관리사가 가입 시 역할을 고르고, ADMIN이 둘 다 포함):

```bash
python3 ~/.claude/skills/spring-starter/scripts/scaffold.py init --target . \
  --roles ADMIN,OWNER,MANAGER --role-from-request \
  --role-hierarchy 'ADMIN>OWNER,ADMIN>MANAGER'
```

**사용자에게 확인할 것:** 패키지명(신규 프로젝트일 때만), `product` 예제 도메인 포함 여부, 권한 구성.
권한은 `ADMIN`, `USER` 두 개로 충분하면 기본값으로 진행하고, 역할이 더 필요하다고 하면 상수 이름, 가입 시 권한 결정 방식, 상하 관계를 물어서 위 옵션으로 넘긴다.
나머지는 기본값으로 진행해도 된다.

## 2. 새 도메인 추가

```bash
python3 ~/.claude/skills/spring-starter/scripts/scaffold.py add-domain <도메인명> --target <프로젝트_디렉토리>
```

`<도메인명>`은 lowerCamelCase 자바 식별자(`order`, `orderItem`). 패키지는 앱 클래스에서 자동 감지된다.
`controller` / `service` / `data/{dto,entity,exception,repository}` 7개 파일을 만든다.

- 기본(**owned**): 엔티티에 `@ManyToOne User` 소유자가 붙고, 서비스가 `AuthService.getUserEntity(email)`로 소유자를 찾고, 컨트롤러가 `SecurityUtil.getUsername()`을 넘긴다. `SecurityConfiguration`에 `.requestMatchers("/api/v1/<도메인>/**").hasRole("USER")`도 자동 삽입된다 (권한은 `--role`로 변경).
- `--standalone` — User 연관 없는 독립 도메인. 보안 매처는 삽입하지 않고 `.anyRequest().authenticated()`에 맡긴다.
- `--role ADMIN` 또는 `--role OWNER,MANAGER`: 요구 권한 변경. 여러 개면 `hasAnyRole`로 들어간다.
  1. 프로젝트의 `UserRole` enum을 읽어 없는 권한이면 거부한다.
  2. `--role`을 생략하면 `USER`를 쓰는데, `UserRole`에 `USER`가 없으면 기본값이 없으므로 `--role`을 반드시 줘야 한다.
  3. `RoleHierarchy`가 있으면 상위 권한은 따로 적지 않아도 접근된다. 예: `ADMIN>OWNER`면 `--role OWNER`만으로 ADMIN도 통과.

### 생성 후 반드시 할 일

스크립트가 만드는 엔티티/DTO는 `name`, `description`, `createdAt`이라는 **자리표시자 필드**를 갖는다.
이건 뼈대일 뿐이므로, 생성 직후 그 도메인의 실제 필드로 바꿔라 — 엔티티 컬럼, `RequestDto`의 검증 애너테이션,
`ResponseDto`, 그리고 서비스의 빌더 호출까지 네 곳을 함께 고쳐야 한다.
필드를 늘렸으면 조회/수정 메서드도 그에 맞게 더한다.

새 예외 상황이 필요하면 `ErrorCode`에 항목을 추가하고 `GlobalException`을 상속한 예외를 만든다.
기본 템플릿은 기존 `ErrorCode.DATA_NOT_FOUND`를 재사용하므로 단순 CRUD면 손댈 게 없다.

## 3. 실행 확인

```bash
cd <프로젝트_디렉토리> && ./gradlew compileJava
```

부팅까지 확인하려면 MariaDB와 Redis가 먼저 떠 있어야 한다.

```bash
docker run -d -p 3306:3306 -e MARIADB_ROOT_PASSWORD=root -e MARIADB_DATABASE=<프로젝트명> mariadb:11
docker run -d -p 6379:6379 redis:7
```

## Gotchas

이 스택에서 실제로 발목을 잡는 것들이다.

- **`./gradlew build`는 DB 없이 실패한다.** `contextLoads` 테스트가 `@SpringBootTest`라 실제 MariaDB 연결을 요구한다. 컴파일만 볼 거면 `./gradlew compileJava`, 빌드가 필요하면 `./gradlew build -x test`.
- **도메인 이름이 SQL 예약어면 DDL이 깨진다.** `order`, `user`, `group` 등. 엔티티의 `@Table(name = "...")`을 `` `order` `` 처럼 백틱으로 감싸거나 `orders`로 바꿔라. 스크립트는 이름을 그대로 쓴다.
- **`User.registeredAt`의 `@CreatedDate`는 실제로 동작하지 않는다.** `@EnableJpaAuditing`이 없어서 `AuthService.signUp`이 `LocalDateTime.now()`로 직접 넣는다. 새 엔티티에서 감사 필드를 쓰려면 auditing을 켜거나 같은 방식으로 직접 채워라.
- **JWT 시크릿은 Base64 인코딩된 256비트 이상이어야 한다.** `Decoders.BASE64.decode` → `Keys.hmacShaKeyFor` 경로라 짧거나 non-Base64 값이면 부팅 중 터진다. `application-local.yml`의 기본값은 개발용 더미이므로 배포 전에 `JWT_SECRET_KEY`를 반드시 교체할 것.
- **`.env`에 빈 값을 두면 기본값이 무시된다.** `KEY=`처럼 비워 두면 키가 존재하는 것으로 처리돼 `${KEY:기본값}`의 기본값이 쓰이지 않는다. 특히 `JWT_SECRET_KEY=`는 빈 문자열로 키를 만들다가 부팅이 실패하므로 `.env.example`에서도 주석으로 둔다. 쓰지 않을 값은 줄째 지우거나 주석으로 둘 것.
- **`build.gradle.kts`는 자동 패치되지 않는다.** Kotlin DSL 프로젝트에 graft하면 스크립트가 경고만 내므로 의존성을 직접 추가해야 한다.
- **`SecurityUtil.getUsername()` 실패는 401(C4011)이다.** `AuthenticationInfoException`이 `GlobalException`을 상속하므로 `GlobalExceptionHandler`가 처리한다. 예전 템플릿처럼 `RuntimeException`을 직접 상속하면 C500으로 떨어지니 새 예외도 반드시 `GlobalException`을 상속할 것.
- **토큰에 권한 클레임이 없으면 DB의 role을 쓰고, 사용자도 없으면 권한 없음으로 처리한다.** 특정 권한으로 fallback하지 않으므로 `USER`가 없는 권한 구성에서도 안전하다.
- **jjwt는 0.12.x API다.** `Jwts.builder().subject()/.signWith(key, Jwts.SIG.HS256)`, 파싱은 `Jwts.parser().verifyWith(key).build().parseSignedClaims()`. 구버전 `setSubject`/`parseClaimsJws` 예제를 그대로 가져오면 컴파일되지 않는다.

## 손으로 코드를 더할 때

스크립트가 못 덮는 부분(복잡한 도메인 로직, 새 공통 컴포넌트)을 작성할 때는
`references/conventions.md`를 읽고 기존 코드와 같은 결을 유지하라 —
응답은 반드시 `GlobalResponse`로 감싸고, 예외는 `GlobalException`을 상속하고,
로그는 `[메서드명] 한국어 설명: {}` 형식을 따른다.
