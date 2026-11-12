# 코드 컨벤션

스크립트로 못 만드는 코드를 손으로 더할 때 지킬 규칙. 모두 레퍼런스 프로젝트에서 뽑은 실제 패턴이다.

## 패키지 레이아웃

```
<base>/
  common/
    redis/       RedisConfiguration, RedisService
    response/    GlobalResponse, GlobalErrorResponse, ErrorCode, GlobalException, GlobalExceptionHandler
    security/    JwtTokenProvider, JwtAuthenticationFilter, SecurityConfiguration, SecurityUtil,
                 AuthenticationToken, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler
                 exception/  AuthenticationInfoException, TokenInvalidException
    swagger/     SwaggerConfiguration
  <domain>/
    controller/  <Domain>Controller
    service/     <Domain>Service
    data/
      dto/       <Domain>RequestDto, <Domain>ResponseDto   (user 도메인만 request/·response/ 로 한 겹 더 나뉨)
      entity/    <Domain>
      enums/     필요할 때만
      exception/ <Domain>NotFoundException 등
      repository/<Domain>Repository
```

도메인 패키지는 `common`에 의존해도 되지만, `common`이 도메인에 의존해선 안 된다.
예외: `JwtTokenProvider`가 롤 복구를 위해 `UserRepository`를 참조한다.

## 응답

컨트롤러는 `ResponseEntity`가 아니라 `GlobalResponse<T>`를 직접 반환한다. 상태 코드는 예외 핸들러가 정한다.

```java
public GlobalResponse<OrderResponseDto> getOrder(@RequestParam Long id) {
    return orderService.getOrder(id);
}
```

서비스가 `GlobalResponse.ok(dto)` 또는 반환값이 없으면 `GlobalResponse.ok()`로 감싼다.
성공 응답은 `{"data": ..., "error": {"code": null, "message": null}}`,
실패는 `{"data": null, "error": {"code": "C404", "message": "..."}}` 형태가 된다.

## 예외

1. `ErrorCode`에 `이름("코드", "한국어 메시지", HTTP상태)` 추가
2. `GlobalException`을 상속한 예외 클래스를 도메인의 `data/exception/`에 추가 (인자 없는 생성자)
3. 서비스에서 던지기만 하면 `GlobalExceptionHandler`가 처리한다

코드 체계: `C` + HTTP 상태. 같은 상태에 여러 개면 뒤에 일련번호 (`C4011`, `C4012`).

```java
public class OrderNotFoundException extends GlobalException {
    public OrderNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
```

## 서비스

- 클래스에 `@Service @RequiredArgsConstructor`, 로그가 필요하면 `@Slf4j`
- 의존성은 `private final` 필드 주입 (생성자 주입을 Lombok이 생성)
- 쓰기 메서드에 `@Transactional`, 읽기 전용에 `@Transactional(readOnly = true)` — `org.springframework.transaction.annotation.Transactional`
- 엔티티 조회는 `private <Domain> get<Domain>Entity(Long id)` 헬퍼로 뽑고 `orElseThrow(XxxNotFoundException::new)`
- 엔티티는 Lombok `@Builder`로 만든다

## 로그

`[메서드명] 한국어 설명: {}` 형식으로 통일한다.

```java
log.info("[signUp] 새로운 사용자 등록: {}", user.getEmail());
log.error("[logout] 유효하지 않은 Access Token으로 로그아웃 시도");
```

토큰 값, 비밀번호, 개인정보 원문은 남기지 않는다 — 식별자(이메일) 정도까지.

## 엔티티

`@Data @Builder @NoArgsConstructor @AllArgsConstructor @Entity @Table(name = "...")`,
PK는 `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`.
기본값이 필요한 필드에는 `@Builder.Default`.
연관관계는 `@ManyToOne @JoinColumn(name = "user_id", nullable = false)`.

## DTO

`@Data @Builder @NoArgsConstructor @AllArgsConstructor`.
요청 DTO에만 `jakarta.validation` 애너테이션(`@NotBlank`, `@Email`, `@Size`)을 붙이고,
컨트롤러 파라미터에 `@Valid`를 단다. 검증 실패는 `GlobalExceptionHandler`가 `C002`로 변환한다.

## 컨트롤러

`@RestController @RequestMapping("/api/v1/<domain>") @RequiredArgsConstructor`.
인증된 사용자의 이메일이 필요하면 `SecurityUtil.getUsername()`을 컨트롤러에서 읽어 서비스에 넘긴다 —
서비스는 `SecurityContextHolder`를 직접 보지 않는다.
Swagger 설명이 필요하면 `@Operation(summary = "...")`.

## 보안 경로

새 공개 엔드포인트는 `SecurityConfiguration`의 `permitAll()` 목록에,
권한 제한이 필요하면 `.requestMatchers("/api/v1/<domain>/**").hasRole("USER")`를
`.anyRequest().authenticated()` **앞에** 넣는다. 순서가 뒤바뀌면 매처가 무시된다.

1. `hasRole("USER")`는 `ROLE_USER` 권한을 요구한다. `UserRole` enum의 값이 이미 `ROLE_` 접두사를 갖고 있으므로 `hasRole`에는 접두사 없이 쓴다.
2. 여러 권한을 허용하려면 `.hasAnyRole("OWNER", "MANAGER")`.
3. `RoleHierarchy` 빈(`init --role-hierarchy`)이 있으면 상위 권한은 하위 권한의 매처를 자동으로 통과한다. 상위 권한 이름을 매처에 중복으로 적지 않는다.
4. 실제 권한 이름은 프로젝트의 `UserRole` enum을 확인하고 쓴다. 템플릿 기본값은 `ADMIN`, `USER`지만 프로젝트마다 다를 수 있다.

## 예외 상속

모든 비즈니스 예외는 `GlobalException`을 상속한다. `RuntimeException`을 직접 상속하면 `GlobalExceptionHandler`의
`Exception` 핸들러로 떨어져 C500이 된다. 인증 정보 부재(`AuthenticationInfoException`)는 `VERIFICATION_INVALID`(C4011, 401)를 쓴다.

## 설정

프로필별 값은 `application-local.yml` / `application-prod.yml`에 두고,
공통은 `application.yml`에 둔다. 모든 값은 `${ENV_VAR:기본값}` 형태로 환경변수를 우선한다.
prod 프로필에는 기본값을 두지 않는다 — 누락되면 부팅이 실패하는 편이 낫다.

새 환경변수를 추가하면 `.env.example`에도 같은 이름으로 추가한다. 로컬 기본값이 있으면 그 값을, 비밀 값이면 주석 처리한 줄과 생성 방법을 적는다.
날짜와 시간은 앱 클래스에서 고정한 JVM 기본 시간대를 따른다. 코드에서 `ZoneId`를 따로 하드코딩하지 않는다.
