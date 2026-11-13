# 백엔드 아키텍처

이 문서는 이 저장소의 현재 코드 상태를 기록한다. 기능을 바꾸면 같은 작업 안에서 함께 갱신한다.
S5 일정, S6 내 코스(`itinerary`, `course` 패키지)의 세부 동작은 [PROGRESS.md](../PROGRESS.md)에 있고, 이 문서는 구조와 공통 부분, 신형 담당 API(행사, 검색, 장소, 관심 행사)를 다룬다.

> 마지막 갱신: 2026-11-14, E4에서 요일이 있는 날짜(다음 주 토요일 등)를 그 요일로 계산하도록 수정

## 한눈에 보기

| 항목 | 내용 |
| --- | --- |
| 언어 | Java 21 (toolchain) |
| 프레임워크 | Spring Boot 4.1.0 (Web, Data JPA, Security, Validation, Data Redis) |
| 빌드 | Gradle (래퍼 포함), Lombok |
| DB | MariaDB (`mariadb-java-client`, Hibernate `MariaDBDialect`). 테스트는 H2 메모리 DB |
| 캐시 | Redis (리프레시 토큰 저장, 토큰 블랙리스트) |
| 인증 | JWT (jjwt 0.12.6, HS256), Stateless |
| API 문서 | springdoc-openapi 3.0.3, Swagger UI `/swagger-ui.html`. `@ApiErrorCodes`로 오류 응답 예시를 문서에 추가 |
| 행사 데이터 | MariaDB `events`, `event_sources`. 팀 행사 DB(GCP PostgreSQL) 또는 엑셀에서 `scripts/seed_events.py`로 옮겨 넣는다 |
| AI | OpenAI Chat Completions (JSON 모드), JDK HttpClient로 직접 호출 (E4만 사용) |
| 장소 검색 | 카카오 로컬 키워드 검색, JDK HttpClient로 직접 호출 |
| 베이스 패키지 | `com.yongyongthon.be` (앱 클래스 `BeApplication`) |
| 시간대 | `BeApplication.main` 첫 줄에서 `Asia/Seoul`로 고정 |

## 구조

```
src/main/java/com/yongyongthon/be/
  BeApplication.java              # 진입점, JVM 기본 시간대 고정
  common/
    ai/
      OpenAiClient.java           # OpenAI 호출. 실패는 E5021, 시간 초과는 E5041로 변환
      exception/                  # AiServerException(E5021), AiTimeoutException(E5041)
    config/AppProperties.java     # app.* 설정 (공유 링크 주소 등)
    redis/                        # RedisTemplate 설정, RedisService
    response/
      GlobalResponse.java         # {data, error} 응답 래퍼
      GlobalErrorResponse.java    # {code, message}
      ErrorCode.java              # 에러 코드 enum (공통 C 코드, 명세서01 E 코드)
      GlobalException.java        # 비즈니스 예외의 추상 부모
      GlobalExceptionHandler.java # @RestControllerAdvice. 형식 오류 C001, 검증 실패 C002, 나머지 C500
      PublicApiExceptionHandler.java  # event, search, itinerary, place, course 패키지의 요청 값 오류를 E4001로 응답 (우선순위 최상)
      RequestValueInvalidException.java  # 서비스에서 던지는 E4001
    security/
      SecurityConfiguration.java  # 필터 체인, 공개 경로, CORS, BCrypt
      JwtTokenProvider.java       # 토큰 발급, 검증, 블랙리스트, RT 저장
      JwtAuthenticationFilter.java
      JwtAuthenticationEntryPoint.java  # 401 (C4011)
      JwtAccessDeniedHandler.java       # 403 (C403)
      SecurityUtil.java           # getUsername(), getUsernameOrNull()(비로그인이면 null)
      exception/                  # AuthenticationInfoException, TokenInvalidException
    swagger/
      SwaggerConfiguration.java   # bearerAuth 스키마
      ApiErrorCodes.java          # API가 낼 수 있는 ErrorCode 목록 애너테이션
      ApiErrorCodesCustomizer.java  # 위 애너테이션을 Swagger 오류 응답 예시로 바꿈 (C500은 항상 추가)
    util/                         # Geo(직선거리), Times(HH:mm), Ids
  event/
    controller/EventController.java    # /api/events (E1, E2, E3)
    controller/FavoriteController.java # /api/me/favorites (E19, E20, 로그인 필요)
    service/EventService.java     # 목록 검색(필터, 정렬, 페이지), 상세, 안내 상이, 태그와 거리, isLiked
    service/FavoriteService.java  # 관심 행사 담기와 해제 (멱등)
    service/EventCatalog.java     # 일정, 내 코스가 행사를 읽는 곳 (events 테이블을 EventInfo로)
    data/
      dto/                        # E1, E2, E3, E19 요청과 응답, EventInfo, EventProgram, NearbyEvent
      entity/                     # Event, EventSource, Favorite
      exception/                  # EventNotFound(E4041), DistanceCoordinateRequired(E4002)
      repository/                 # EventRepository, EventSourceRepository, FavoriteRepository
  itinerary/                      # S5 하루 일정 (팀원 담당, PROGRESS.md)
  course/                         # S6 내 코스, 캘린더 (팀원 담당, PROGRESS.md)
  place/
    controller/PlaceController.java  # /api/places (E14, 공개)
    service/PlaceService.java     # 카카오 로컬 호출과 응답 변환
    data/                         # PlaceSearchResponseDto, PlaceSearchFailedException(E5022)
  search/
    controller/SearchController.java  # /api/search (E4)
    service/SearchService.java    # 프롬프트 생성, 모델 출력 정리, unresolved 계산
    service/SearchConditionRules.java  # 상대 날짜 계산, 용인시 동, 읍, 면과 소속 구 표
    data/dto/                     # SearchParseRequestDto, SearchParseResponseDto
  user/                           # 회원가입, 로그인, 로그아웃, 재발급 (/api/v1/auth)
scripts/seed_events.py            # 팀 행사 DB 또는 엑셀을 events, event_sources INSERT SQL로 변환
db/schema.sql                     # prod용 전체 테이블 스키마
```

의존 방향은 도메인이 `common`에 의존하고, `common`은 도메인에 의존하지 않는다. 예외는 다음과 같다.

1. `JwtTokenProvider`가 권한 복구를 위해 `UserRepository`를 참조한다
2. `event`가 관심 행사 때문에 `user`(`User`, `AuthService`)에 의존한다
3. `itinerary`, `course`가 행사를 `EventCatalog`로 읽고, `course`의 `InterestEventProvider`가 `FavoriteRepository`를 읽는다

## API

기본 정책은 공개 경로를 뺀 모든 경로가 인증 필요(`anyRequest().authenticated()`)다.

1. 공개: `/api/v1/auth/signin`, `signup`, `reissue`, Swagger 경로(`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` 등)
2. 공개, 토큰 선택: `GET /api/events`, `GET /api/events/**`, `GET /api/places/search`, `POST /api/search/parse`, `POST /api/itineraries`. 토큰이 있으면 필터가 인증 정보를 채우고, 없거나 잘못돼도 비로그인으로 처리한다
3. 공개 (팀원 데모 API): `/api/itineraries/**`, `/api/shared/**`, `/api/me/courses`, `/api/me/courses/**`, `/api/me/calendar`
4. 로그인 필요: `/api/me/favorites/**`(위 목록에 없어 `anyRequest`에 걸린다), `/api/v1/auth/logout`

신형 담당 API:

| Method | Path | 권한 | 요청 | 응답 | 설명 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/events` | 공개 (토큰 선택) | 쿼리 `EventSearchRequestDto` | `EventListResponseDto` | 행사 목록과 지도 (E1). 빈 결과도 200 |
| GET | `/api/events/{eventId}` | 공개 (토큰 선택) | 경로 `eventId` | `EventDetailResponseDto` | 행사 상세 (E2). 없으면 404 E4041 |
| GET | `/api/events/{eventId}/discrepancies` | 공개 (토큰 선택) | 경로 `eventId` | `EventDiscrepancyResponseDto` | 안내 상이 (E3). 항상 `hasDiscrepancy=false`, `items=[]`. 없으면 404 E4041 |
| POST | `/api/search/parse` | 공개 | `{query, today}` | `SearchParseResponseDto` | 자연어를 검색 조건으로 변환 (E4). 못 뽑아도 200 |
| GET | `/api/places/search` | 공개 | 쿼리 `query`(공백 불가, 100자 이하) | `PlaceSearchResponseDto` | 출발지 장소 검색 (E14). `places[]`: name, address, lat, lng. 최대 15건 |
| PUT | `/api/me/favorites/{eventId}` | 로그인 필요 | 경로 `eventId` | `FavoriteResponseDto` | 관심 행사 담기 (E19). 멱등, `favorited=true`. 없는 행사는 404 E4041 |
| DELETE | `/api/me/favorites/{eventId}` | 로그인 필요 | 경로 `eventId` | `FavoriteResponseDto` | 관심 행사 해제 (E20). 멱등, `favorited=false`. 없는 행사는 404 E4041 |

인증 API(`/api/v1/auth/signup`, `signin`, `reissue`, `logout`)는 README의 표를 본다. S5, S6 API(`/api/itineraries`, `/api/shared`, `/api/me/courses`, `/api/me/calendar`)는 PROGRESS.md를 본다.

모든 컨트롤러 메서드에 `@Operation`이 있고, 오류를 낼 수 있는 API에는 `@ApiErrorCodes`가 있다. 새 API도 둘 다 붙인다.

E1 동작:

1. 필터: 행사 기간이 `dateFrom`부터 `dateTo`(생략 시 `dateFrom`)까지와 겹치면 포함. `district`(콤마 복수), `dong`(주소에 동 이름 포함, 주소 없으면 제외), `budget=free`(`isFree`가 true)
2. `companion`, `indoorOutdoor`는 값 검증만 하고 필터에 쓰지 않는다 (근거 데이터 없음)
3. `sort=date`(기본): 진행 중(시작일이 `dateFrom` 이전이거나 같음), 시작 예정, 연간 계획, 날짜 없음 순. 같은 묶음 안에서는 시작일, 행사ID 순
4. `sort=distance`: `lat`, `lng` 필수(없으면 400 E4002). 직선거리 가까운 순, 좌표 없는 행사는 맨 뒤. 거리가 같으면 날짜순
5. `lat`, `lng`가 있으면 정렬과 관계없이 `distanceKm`(소수 둘째 자리)를 채운다
6. `view=map`: 페이지를 무시하고 전체를 준다. `page`는 0, `size`는 `totalCount`
7. 오류: 400 E4001(`dateFrom` 누락, 날짜 형식, 기간 역전, 허용되지 않는 값, `page` 음수, `size`가 1 이상 100 이하가 아님), 400 E4002
8. 구현: 행사 전체를 읽어 메모리에서 거르고 정렬한다. 데이터가 수만 건이 되면 DB 쿼리로 옮긴다 (코드에 `ponytail:` 주석)
9. 항목의 `summary`, `themeLabel`, `indoorOutdoor`, `thumbnailUrl`은 null, `hasDiscrepancy`는 false
10. `isLiked`: 로그인 사용자가 담은 행사면 true. 비로그인이면 false이고 관심 행사를 조회하지 않는다

E2 응답에서 데이터가 없어 고정값으로 내보내는 필드:

1. `summary`, `indoorOutdoor`, `imageUrl`, `notice`, `keyInfo.runningTimeMin`, `keyInfo.indoorOutdoor`, `apply.channelType`, `apply.label`: 항상 null
2. `programs`: 항상 빈 배열
3. `hasDiscrepancy`: 항상 false (출처별 값 데이터가 생기면 E1, E2, E3를 함께 바꾼다)
4. `tags`: `isFree`가 true면 무료, `applyType`이 apply면 신청 필요 (`EventService.tagsOf`)
5. `apply.text`: `applyType`이 apply면 신청 필요, 아니면 null
6. `sources`: 공식 출처가 먼저, 같은 유형 안에서는 원본 순서
7. 날짜는 `yyyy-MM-dd`, 접수 일시(`apply.receiptStart`, `receiptEnd`)는 `yyyy-MM-dd HH:mm`

E4 동작:

1. `query`(공백 불가, 200자 이하)와 `today`(선택, 생략 시 서버 날짜)를 받아 OpenAI에 보낸다. 프롬프트에 기준일과 요일을 넣는다
2. 모델 출력은 믿지 않고 허용된 값만 남긴다. 날짜는 `yyyy-MM-dd`만, 기간이 역전되면 두 날짜 모두 버린다. 나머지는 명세의 값 목록만
3. 상대 날짜가 문장에 있으면(띄어쓰기 무시) 서버가 계산한 기간으로 덮어쓴다. 같은 표를 프롬프트에도 넣는다. 찾는 순서는 요일("다음 주 토요일"은 다음 주의 토요일 하루, 주 표현이 없으면 오늘을 포함해 가장 가까운 그 요일, 요일이 여럿이면 처음부터 끝까지), 주말(이번 주말은 토요일이면 오늘과 내일, 일요일이면 오늘만, 그 외에는 다가오는 토요일과 일요일), 주(이번 주, 금주, 다음 주), 모레, 내일, 오늘
4. 동은 용인시 동 목록(`SearchConditionRules.AREA_TO_DISTRICT`, 위키백과 2026-10-15 판 기준)에 있고 문장에 실제로 나온 것만 남긴다. "동백"처럼 "동"을 빼고 말해도 인정한다
5. 구는 세 가지를 합친다: 문장에 구 이름이 나온 모델의 구("수지"만 말해도 인정), 남은 동의 소속 구(표 기준), 문장에 나온 읍과 면의 소속 구(모두 처인구). 모델이 동의 소속 구를 틀리게 내는 경우가 있어서(마북동을 처인구로) 모델 값만으로는 정하지 않는다
6. "이동", "구성", "중앙", "동부", "남사"처럼 흔한 낱말과 겹치는 지명은 전체 이름(이동읍 등)으로만 찾는다
7. 예산은 무료나 "돈은 상관없다"를 직접 말했을 때만 채우도록 프롬프트에 적었다 (말하지 않으면 null)
8. `unresolved`: `dateFrom`, `district`, `companion`, `budget`, `indoorOutdoor` 중 null인 것
9. `parsed`: 조건이 하나라도 있으면 true. false면 conditions는 전부 null이고 `message`에 안내 문구
10. 오류: 400 E4001(AI 호출 전), 502 E5021(키 없음, 연결 실패, 오류 응답, JSON이 아닌 출력), 504 E5041(타임아웃 15초)
11. 실제 호출 확인(2026-11-14, gpt-4o-mini): 한 번에 2초 안팎

E14 동작:

1. 카카오 로컬 키워드 검색을 호출해 `place_name`, 도로명 주소(없으면 지번 주소), `y`(위도), `x`(경도)로 바꾼다. 결과가 없으면 빈 배열
2. 검색어 앞뒤 공백은 지운다. 위치 범위를 용인으로 좁히지 않는다 (출발지는 용인 밖일 수 있다)
3. 오류: 400 E4001, 502 E5022(키 없음, 연결 실패, 오류 응답, 5초 초과, 응답 형식 오류)
4. 요청과 응답 본문, 공개 여부, E5022는 명세서01에 없어 신형이 정했다 (명세 8장의 제안 형태 그대로)

관심 행사는 팀원 캘린더(`/api/me/calendar`)에도 나온다. `InterestEventProvider`가 토큰이 있을 때 그 사용자의 `favorites`를 읽는다.

## 응답 형식

성공:

```json
{ "data": { "eventId": "YI-0003", "name": "..." }, "error": { "code": null, "message": null } }
```

실패:

```json
{ "data": null, "error": { "code": "E4041", "message": "행사를 찾을 수 없습니다." } }
```

HTTP 상태 코드는 `ErrorCode`의 값을 핸들러가 적용한다. 요청 값 오류는 패키지에 따라 다르다.

1. `event`, `search`, `itinerary`, `place`, `course`: `PublicApiExceptionHandler`가 먼저 받아 E4001(본문 검증 실패, 파라미터 누락, 형식 오류, 깨진 JSON)
2. 그 외(인증 API): 애너테이션 검증 실패는 C002, 형식 오류(깨진 JSON, 파라미터 누락, 타입 오류)는 C001

어떤 필드가 틀렸는지는 응답이 아니라 로그에만 남는다.

## 권한

1. 역할: `ADMIN`(`ROLE_ADMIN`), `USER`(`ROLE_USER`). `UserRole` enum에 정의되어 있다
2. 가입 시 `User.role`의 `@Builder.Default`로 항상 `USER`
3. `RoleHierarchy` 빈이 없다. 역할끼리 서로 포함하지 않는다
4. `hasRole` 매처를 쓰는 경로는 없다. 인증 여부만 검사한다

## 인증 흐름

1. 가입 또는 로그인: 비밀번호를 BCrypt로 비교하거나 저장하고, `JwtTokenProvider.generateToken`이 AT(권한 클레임 포함)와 RT를 발급한다. RT는 Redis에 저장된다.
2. 인증된 요청: `JwtAuthenticationFilter`가 `Authorization: Bearer` 헤더의 AT를 검증한다(서명, 만료, 블랙리스트). 통과하면 권한 클레임으로 `Authentication`을 만든다.
3. 토큰 재발급: RT의 서명, 만료, 블랙리스트를 검사하고 Redis에 저장된 RT와 대조한다. 통과하면 AT와 RT를 새로 발급한다. 이전 RT는 즉시 무효가 된다.
4. 로그아웃: 저장된 RT를 블랙리스트에 올리고 Redis에서 지운다. AT도 남은 만료 시간만큼 블랙리스트에 올린다.

## 저장소 키 (Redis)

| 키 패턴 | 값 | TTL |
| --- | --- | --- |
| `refresh:<email>` | 리프레시 토큰 | RT 만료 시간 (기본 7일) |
| `blacklist:<token>` | `logout` | 해당 토큰의 남은 만료 시간 |

## 데이터 모델

테이블 전체(일정, 코스 포함)는 `db/schema.sql`에 있다. 여기서는 공통과 신형 테이블만 적는다.

### User (테이블 `user`)

| 필드 | 타입 | 제약 |
| --- | --- | --- |
| id | Long | PK, IDENTITY |
| email | String | NOT NULL, UNIQUE |
| name | String | NOT NULL |
| password | String | NOT NULL (BCrypt 해시) |
| role | UserRole (STRING) | NOT NULL, 기본 USER |
| registeredAt | LocalDateTime | 수정 불가. `AuthService.signUp`이 직접 채운다 |
| modifiedAt | LocalDateTime | `@UpdateTimestamp` |

### Event (테이블 `events`)

`scripts/seed_events.py`로 채운다. 앱은 읽기만 한다. 엑셀 기준 1,751건(중복 73건과 취소 1건 제외).

| 필드 | 타입 | 제약 |
| --- | --- | --- |
| id (`event_id`) | String(20) | PK, 행사ID (예: YI-0001) |
| name | String(300) | NOT NULL |
| category | String(10) | NOT NULL, 공연 또는 행사 |
| subCategory | String(50) | 원본 분류 (도서관 프로그램, 전시, 축제 등) |
| startDate, endDate | LocalDate | |
| dateStatus | String(10) | NOT NULL, 확정, 예정, 연간 계획, 미확인 |
| time | String(100) | 시간 원문 |
| placeName, address | String(200) | 주소가 없으면 확인된 위치 주소 |
| district | String(10) | 처인구, 기흥구, 수지구. 없으면 주소에서 추출 |
| lat, lng | Double | 온라인 등은 null |
| coordPrecision | String(10) | place 또는 area |
| fee | String(200) | 요금 원문 |
| isFree | Boolean | 무료 확인 true, 유료 확인 false, 모르면 null |
| target | String(300) | 대상 |
| applyType | String(10) | apply 또는 null (신청 링크가 있으면 apply) |
| applyUrl | String(1000) | |
| receiptStart, receiptEnd | LocalDateTime | 접수 기간 |
| receiptStatus | String(50) | 접수 상태 원문 |
| sourceUrl | String(1000) | NOT NULL, 대표 출처 |

명세의 `summary`, `themeLabel`, `imageUrl`, `indoorOutdoor`, `tags`, `notice`, 프로그램은 데이터가 없어 컬럼을 두지 않았다. 응답에서 null 또는 빈 배열로 내보낸다. 일정과 내 코스는 `EventCatalog`가 이 엔티티를 `EventInfo`로 바꿔 쓴다.

### EventSource (테이블 `event_sources`)

| 필드 | 타입 | 제약 |
| --- | --- | --- |
| id | Long | PK, IDENTITY |
| event | Event | `@ManyToOne` LAZY, `event_id` NOT NULL |
| name | String(100) | NOT NULL |
| type | String(10) | NOT NULL, official, blog, etc |
| url | String(1000) | NOT NULL |
| sortOrder | Integer | NOT NULL, 0이 대표 출처 |

`Event`에는 출처 컬렉션이 없다. `EventSourceRepository.findByEventIdOrderBySortOrder`로 조회한다.

### Favorite (테이블 `favorites`)

| 필드 | 타입 | 제약 |
| --- | --- | --- |
| id | Long | PK, IDENTITY |
| user | User | `@ManyToOne` LAZY, `user_id` NOT NULL |
| event | Event | `@ManyToOne` LAZY, `event_id` NOT NULL |
| createdAt | LocalDateTime | NOT NULL, 서비스가 직접 채움 |

`(user_id, event_id)` 유니크. 담기는 트랜잭션 없이 저장해서, 동시 요청으로 유니크 제약에 걸려도 이미 담긴 것으로 보고 성공 응답한다.

## 에러 코드

| 코드 | HTTP | 메시지 | 사용처 |
| --- | --- | --- | --- |
| C001 | 400 | 잘못된 요청입니다. | `GlobalExceptionHandler`의 형식 오류 (위 5개 패키지 밖) |
| C002 | 400 | 유효하지 않은 파라미터입니다. | `GlobalExceptionHandler`의 애너테이션 검증 실패 (위 5개 패키지 밖) |
| C4011 | 401 | 인증 정보가 유효하지 않습니다. | `JwtAuthenticationEntryPoint`, `AuthenticationInfoException` |
| C4012 | 401 | 비밀번호가 일치하지 않습니다. | `PasswordInvalidException` (로그인) |
| C4013 | 401 | 유효하지 않은 토큰입니다. | `TokenInvalidException` (재발급, 로그아웃) |
| C403 | 403 | 승인되지 않은 사용자입니다. | `JwtAccessDeniedHandler` |
| C4041 | 404 | 사용자를 찾을 수 없습니다. | `UserNotFoundException` |
| C404 | 404 | 정보를 불러올 수 없습니다. | 미사용 |
| C4091 | 409 | 이미 존재하는 이메일입니다. | `EmailDuplicationException` (가입) |
| C500 | 500 | 오류가 발생하였습니다. | 처리되지 않은 예외 |
| E4001 | 400 | 요청 값이 올바르지 않습니다. | `PublicApiExceptionHandler`, `RequestValueInvalidException` |
| E4002 | 400 | 거리순 정렬에는 좌표가 필요합니다. | `DistanceCoordinateRequiredException` (E1) |
| E4003 | 400 | 삭제할 수 없는 항목입니다. | 일정 슬롯 (PROGRESS.md) |
| E4041 | 404 | 행사를 찾을 수 없습니다. | `EventNotFoundException` (E2, E3, E19, E20, 일정 생성) |
| E4042 | 404 | 일정을 찾을 수 없습니다. | 일정 (PROGRESS.md) |
| E4043 | 404 | 일정 항목을 찾을 수 없습니다. | 일정 슬롯 (PROGRESS.md) |
| E4044 | 404 | 공유된 일정을 찾을 수 없습니다. | 공유 (PROGRESS.md) |
| E5021 | 502 | AI 서버 호출에 실패했습니다. | `AiServerException` (E4) |
| E5041 | 504 | AI 응답 시간이 초과되었습니다. | `AiTimeoutException` (E4) |
| E5022 | 502 | 장소 검색에 실패했습니다. | `PlaceSearchFailedException` (E14). 명세서01에 없는 코드 |

## 설정

`application.yml`(공통), `application-local.yml`(기본값 포함), `application-prod.yml`(기본값 없음, 환경변수 필수)로 나뉜다. 활성 프로필은 `SPRING_PROFILES_ACTIVE`(기본 `local`)이고, 로컬 `.env`는 `spring.config.import`로 읽는다. 테스트는 `local`, `test` 프로필(`src/test/resources/application-test.yml`, H2)이다.

| 환경변수 | 로컬 기본값 | 설명 |
| --- | --- | --- |
| SPRING_PROFILES_ACTIVE | local | local 또는 prod |
| DB_HOST, DB_PORT, DB_NAME | localhost, 3306, be | MariaDB 접속 |
| DB_USERNAME, DB_PASSWORD | root, root | MariaDB 계정 |
| REDIS_HOST, REDIS_PORT, REDIS_PASSWORD | localhost, 6379, 빈 값 | Redis 접속 |
| **JWT_SECRET_KEY** | 개발용 더미 키 | Base64 인코딩 256비트 이상. **배포 전 반드시 교체**. 빈 값이면 부팅 실패 |
| JWT_ACCESS_TOKEN_EXPIRE_TIME | 3600000 | AT 만료(ms), 1시간 |
| JWT_REFRESH_TOKEN_EXPIRE_TIME | 604800000 | RT 만료(ms), 7일 |
| CORS_ALLOWED_ORIGINS | `*` | 콤마로 구분한 허용 Origin 패턴 |
| **OPENAI_API_KEY** | 없음 | E4용. 없으면 E4만 502 E5021. **prod에서는 필수** |
| OPENAI_MODEL | gpt-4o-mini | E4가 쓰는 모델 |
| AI_SEARCH_PARSE_TIMEOUT_SECONDS | 15 | E4 타임아웃(초) |
| **KAKAO_REST_API_KEY** | 없음 | E14용. 없으면 E14만 502 E5022. **prod에서는 필수** |
| `app.share-base-url` | `https://example.com/shared` | 일정 공유 링크 주소. **임시값, 배포 주소로 바꿔야 함** |

프로필별 차이는 `ddl-auto`(local은 `update`, prod는 `validate`)와 SQL 로그(local만 출력)다.

## 로컬 실행

1. `cp .env.example .env`
2. `docker run -d --name be-mariadb -p 3306:3306 -e MARIADB_ROOT_PASSWORD=root -e MARIADB_DATABASE=be mariadb:11`
3. `docker run -d --name be-redis -p 6379:6379 redis:7`
4. `./gradlew bootRun` 한 번 실행 (테이블 생성)
5. 행사 데이터 넣기 (README의 "행사 데이터 넣기"): 팀 행사 DB는 IAP 터널을 켜고 `python3 scripts/seed_events.py --db --env-file <접속 정보 파일> > build/seed_events.sql`, 엑셀은 `python3 scripts/seed_events.py <엑셀 경로> > build/seed_events.sql`. 그다음 `docker exec -i be-mariadb mariadb -uroot -proot be < build/seed_events.sql`
6. `http://localhost:8080/swagger-ui.html`

`./gradlew test`는 `BeApplicationTests.contextLoads`가 실제 MariaDB와 Redis를 요구한다. 나머지 테스트는 H2를 쓴다. 컴파일만 볼 때는 `./gradlew compileJava`.

## 알려진 이슈 / 결정 대기

| 항목 | 현재 상태 | 영향 |
| --- | --- | --- |
| 명세에 없는 정의 | E14 본문과 공개 여부, E5022, E19와 E20 경로와 본문은 명세서01에 없어 신형이 정함 | 명세서01(Notion)에 추가하고 팀 확인 필요 |
| 안내 상이, 프로그램 | 출처별 값과 프로그램 데이터가 없음 | E3는 항상 차이 없음, E2 `programs`는 빈 배열 |
| 동행, 실내외 필터 | 근거 데이터가 없어 E1이 값만 받고 무시 | 필터를 걸어도 결과가 같음 |
| 동 필터 | 주소가 도로명이라 동 이름이 거의 없음 | `dong` 결과가 매우 적음 |
| 행사 DB 컬럼 | `seed_events.py --db`의 컬럼 이름이 팀 행사 DB와 맞는지 첫 실행 때 확인 필요 (PROGRESS.md) | 맞지 않으면 `DB_COLUMNS` 수정 |
| 재적재와 관심 행사 | `seed_events.py`는 FK 검사를 끄고 행사를 지운 뒤 다시 넣는다 | 행사ID가 빠진 채 다시 적재하면 그 행사를 가리키는 `favorites`가 남는다 |
| 공유 링크 주소 | `app.share-base-url`이 임시값 | 배포 주소가 정해지면 바꿔야 함 |
| prod 스키마 | 마이그레이션 도구 없이 `db/schema.sql` 수동 실행 | 엔티티를 바꾸면 `schema.sql`도 같이 고쳐야 함 |
| `@CreatedDate` 미동작 | `@EnableJpaAuditing`이 없음 | 생성 시각은 서비스가 직접 채운다 |
| 없는 경로 응답 | 매핑되지 않은 경로가 `Exception` 핸들러로 떨어져 404가 아닌 C500 | 클라이언트가 잘못된 경로를 서버 오류로 봄 |
| 요청 검증 응답 | 실패 시 필드 정보 없이 코드만 응답 | 클라이언트가 어떤 필드가 틀렸는지 알 수 없음 |
| CORS 기본값 | local 기본값이 `*`이고 `allowCredentials`가 true | 운영에서는 허용 Origin을 명시해야 함 |
| 실제 외부 호출 | OpenAI는 실제 호출 확인(2026-11-14). 카카오는 키가 없어 아직 미확인 | 카카오 키를 넣고 E14를 한 번 실제로 확인해야 함 |
| 행정 구역 표 | 동, 읍, 면과 소속 구를 코드에 표로 둠 | 행정 구역이 바뀌면 `SearchConditionRules`를 고쳐야 함 |
| GitHub 기본 브랜치 | 기본 브랜치가 `main` | PR을 만들 때 base를 `dev`로 직접 바꿔야 함 |
