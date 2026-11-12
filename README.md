# BE

YongYongThon Team10 백엔드. Java 21 · Spring Boot 4.1 · Gradle · MariaDB · Redis · JWT 인증.

## 로컬 실행

```bash
cp .env.example .env
docker run -d --name be-mariadb -p 3306:3306 -e MARIADB_ROOT_PASSWORD=root -e MARIADB_DATABASE=be mariadb:11
docker run -d --name be-redis -p 6379:6379 redis:7
./gradlew bootRun
```

- `.env`는 git에 올라가지 않는다(`.gitignore`에 포함). 값은 `.env.example`을 참고한다.
- Swagger UI: http://localhost:8080/swagger-ui.html
- 컴파일만 확인: `./gradlew compileJava`
- `./gradlew build`는 `contextLoads` 테스트가 실제 MariaDB/Redis를 요구하므로 DB 없이는 실패한다. DB 없이 빌드하려면 `./gradlew build -x test`.

## 인증 API

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 (권한 `USER`로 가입), 토큰 발급 | 불필요 |
| POST | `/api/v1/auth/signin` | 로그인, 토큰 발급 | 불필요 |
| POST | `/api/v1/auth/logout` | 로그아웃 (Access Token 블랙리스트 등록, Refresh Token 삭제) | `Authorization: Bearer <accessToken>` |
| POST | `/api/v1/auth/reissue` | `{refreshToken}`으로 AT/RT 재발급 (RT 로테이션, 이전 RT 즉시 무효) | 불필요 |

응답은 모두 `GlobalResponse<T>{data, error}` 형태다. 권한은 `ADMIN`, `USER`.

## 새 도메인 추가

[spring-starter](.claude/skills/spring-starter/SKILL.md) 프로젝트 스킬이 포함되어 있다. Claude Code에서 `/spring-starter`로 쓰거나, 스크립트를 직접 실행한다.

```bash
python3 .claude/skills/spring-starter/scripts/scaffold.py add-domain <이름> --target .
```

- `<이름>`은 lowerCamelCase (`order`, `orderItem`). `controller` / `service` / `data/{dto,entity,exception,repository}`가 생성된다.
- 기본은 **owned**: 엔티티가 `@ManyToOne User` 소유자를 갖고, `/api/v1/<이름>/**`에 `hasRole("USER")` 보안 매처가 추가된다.
- `--standalone`: User 연관이 없는 독립 도메인. 보안 매처는 추가하지 않고 `.anyRequest().authenticated()`를 따른다.
- `--role ADMIN` 또는 `--role A,B`: 요구 권한 변경.
- **생성 후 필수:** 엔티티/DTO의 자리표시자 필드 `name`, `description`, `createdAt`은 뼈대일 뿐이다. 실제 필드로 바꿔라(엔티티 컬럼, `RequestDto` 검증, `ResponseDto`, 서비스의 빌더 호출 네 곳).
- **SQL 예약어 주의:** `order`, `user`, `group` 등은 테이블명으로 쓰면 DDL이 깨진다. `@Table(name = "...")`을 백틱으로 감싸거나 `orders`처럼 바꿔라.

## 배포 전 필수

`JWT_SECRET_KEY`를 반드시 설정한다. **Base64 인코딩된 256비트(32바이트) 이상**이어야 한다.

```bash
openssl rand -base64 32
```

`application-local.yml`의 기본값은 개발용 더미이므로 배포 환경에서는 사용하면 안 된다. `prod` 프로필의 나머지 환경변수(`DB_*`, `REDIS_*` 등)도 모두 설정할 것.
