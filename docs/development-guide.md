# 개발 가이드

새 기능을 추가하거나 코드를 바꿀 때 따르는 규칙. 현재 구조는 [architecture.md](architecture.md)를 본다. 일정과 내 코스(S5, S6)는 [PROGRESS.md](../PROGRESS.md)도 본다.

## 문서 갱신 규칙

기능을 추가하거나 동작을 바꾸면 **같은 작업 안에서** 문서도 갱신한다. 코드만 바꾸고 끝내지 않는다.

| 이런 변경을 했으면 | 이 문서를 갱신 |
| --- | --- |
| 엔드포인트 추가, 변경, 삭제 | `architecture.md`의 API, DTO |
| 패키지(도메인) 추가, 삭제 | `architecture.md`의 구조 |
| 엔티티 변경 | `architecture.md`의 데이터 모델 |
| 에러 코드 추가 | `architecture.md`의 에러 코드 |
| 인증, 권한 변경 | `architecture.md`의 권한, 인증 흐름 |
| 환경변수, 설정 추가 | `architecture.md`의 설정, 그리고 `.env.example` |
| 의존성 추가 | `architecture.md`의 한눈에 보기 |
| 알려진 이슈 해결 또는 새로 발견 | `architecture.md`의 알려진 이슈 / 결정 대기 |
| 코드 규칙, 작업 방식 변경 | 이 문서 또는 `AGENTS.md` |

1. 갱신할 때 `architecture.md` 상단의 **마지막 갱신** 줄도 날짜와 한 줄 요약으로 바꾼다.
2. 새 문서를 `docs/`에 추가하면 `AGENTS.md`의 문서 목록에도 추가한다.
3. `CLAUDE.md`는 `@AGENTS.md`만 담고 있다. 에이전트 지시는 항상 `AGENTS.md`에 쓴다.

## 새 기능 추가 체크리스트

1. 기능명세서(명세서01, Notion)에서 해당 기능 ID와 요구사항을 확인한다
2. 새 도메인이면 스캐폴딩 스크립트로 생성한다: `python3 .claude/skills/spring-starter/scripts/scaffold.py add-domain <이름> --target .` (User 연관이 없으면 `--standalone`)
3. 자리표시자 필드 `name`, `description`, `createdAt`을 실제 필드로 바꾼다. 엔티티, RequestDto 검증, ResponseDto, 서비스의 빌더 호출 네 곳을 함께 고친다
4. 도메인 이름이 SQL 예약어(`order`, `user`, `group` 등)면 `@Table(name = "...")`을 백틱으로 감싸거나 복수형으로 바꾼다
5. 공개가 필요한 경로는 `SecurityConfiguration`의 `permitAll()` 목록에, 권한 제한은 `.requestMatchers(...)`를 `.anyRequest().authenticated()` 앞에 넣는다
6. `./gradlew compileJava`로 확인한다
7. 문서 갱신 ([위 표](#문서-갱신-규칙))

## 응답과 예외

1. 컨트롤러는 `GlobalResponse<T>`를 직접 반환한다. 상태 코드는 예외 핸들러가 정한다.

   ```java
   @PostMapping("/signin")
   public GlobalResponse<TokenResponseDto> signIn(@Valid @RequestBody SignInRequestDto dto) {
       return authService.signIn(dto);
   }
   ```

2. 서비스가 `GlobalResponse.ok(dto)` 또는 `GlobalResponse.ok()`로 감싼다.
3. 새 예외는 `ErrorCode`에 `이름("코드", "한국어 메시지", HTTP상태)`를 추가하고 `GlobalException`을 상속한 클래스를 도메인의 `data/exception/`에 만든다. 코드 체계는 `C` + HTTP 상태이고, 같은 상태가 여럿이면 뒤에 일련번호를 붙인다(`C4011`, `C4012`).
4. `RuntimeException`을 직접 상속하면 C500으로 떨어진다. 반드시 `GlobalException`을 상속한다.

## 서비스와 컨트롤러

1. 서비스는 `@Service @RequiredArgsConstructor`, 로그가 필요하면 `@Slf4j`. 의존성은 `private final` 필드로 받는다.
2. 쓰기 메서드에 `@Transactional`, 읽기 전용에 `@Transactional(readOnly = true)` (spring의 `transaction.annotation`).
3. 엔티티 조회는 `get<Domain>Entity` 헬퍼로 뽑고 `orElseThrow(XxxNotFoundException::new)`. 예: `AuthService.getUserEntity`.
4. 컨트롤러는 `@RestController @RequestMapping("/api/v1/<domain>") @RequiredArgsConstructor`. 인증된 사용자의 이메일이 필요하면 컨트롤러에서 `SecurityUtil.getUsername()`을 읽어 서비스에 넘긴다. 서비스는 `SecurityContextHolder`를 직접 보지 않는다.

## 엔티티와 DTO

1. 엔티티는 `@Data @Builder @NoArgsConstructor @AllArgsConstructor @Entity @Table(name = "...")`, PK는 `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`. 기본값은 `@Builder.Default`. 예: `User.role`.
2. 연관관계는 `@ManyToOne @JoinColumn(name = "user_id", nullable = false)`.
3. DTO는 `@Data @Builder @NoArgsConstructor @AllArgsConstructor`. 요청 DTO에만 검증 애너테이션(`@NotBlank`, `@Email`, `@Size`)을 붙이고 컨트롤러 파라미터에 `@Valid`를 단다.
4. `@CreatedDate`는 동작하지 않는다(auditing 미설정). 생성 시각은 `AuthService.signUp`처럼 `LocalDateTime.now()`로 직접 채운다. 시간대는 앱 클래스에서 고정했으므로 코드에서 `ZoneId`를 하드코딩하지 않는다.

## 로그

`[메서드명] 한국어 설명: {}` 형식으로 통일한다. 토큰 값, 비밀번호, 개인정보 원문은 남기지 않는다. 식별자(이메일)까지만 허용한다.

```java
log.info("[signUp] 새로운 사용자 등록: {}", user.getEmail());
```

## 설정과 환경변수

1. 공통은 `application.yml`, 프로필별 값은 `application-local.yml`과 `application-prod.yml`에 둔다.
2. 모든 값은 `${ENV_VAR:기본값}` 형태로 환경변수를 우선한다. prod 프로필에는 기본값을 두지 않는다. 누락되면 부팅이 실패하는 편이 낫다.
3. 새 환경변수는 `.env.example`에도 같은 이름으로 추가한다. 비밀 값이면 주석 처리한 줄과 생성 방법을 적는다.
4. `.env`에 `KEY=`처럼 빈 값을 두면 기본값이 무시된다. 쓰지 않을 값은 줄째 지우거나 주석으로 둔다.

## 주의할 점

1. `./gradlew build`는 DB와 Redis 없이 실패한다. 컴파일 확인은 `compileJava`, 빌드는 `build -x test`.
2. jjwt는 0.12.x API다. `Jwts.builder().subject()`, `signWith(key, Jwts.SIG.HS256)`, 파싱은 `Jwts.parser().verifyWith(key).build().parseSignedClaims()`. 구버전 예제(`setSubject`, `parseClaimsJws`)는 컴파일되지 않는다.
3. `TokenResponseDto`에 필드를 늘릴 때는 `AuthService.createTokenResponse` 한 곳만 고친다.
