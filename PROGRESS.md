# PROGRESS (S5 하루 일정, S6 내 코스)

기준 문서: S5, S6 개발 지시서 (2026-11-13). 브랜치: `dev`

## 환경

| 항목 | 값 |
| --- | --- |
| 스택 | 팀 저장소 그대로 (Java 21, Spring Boot 4.1, MariaDB, Redis, Lombok, Jackson 3) |
| 패키지 | `itinerary`(S5 일정), `course`(S6 내 코스), `event`(행사 데이터 읽기 전용) |
| API 경로 | 팀 규칙대로 `/api/v1/...` (`/api/v1/itineraries`, `/api/v1/shared`, `/api/v1/me`) |
| 인증 | 없음. 위 세 경로를 `SecurityConfiguration`의 `permitAll()`에 추가 |
| 오류 코드 | 팀 `C` 코드 체계 (아래 표) |
| 행사 데이터 | 용인시 축제 행사 엑셀(1,825건)을 `tools/convert_events.py`로 바꾼 `src/main/resources/data/events_all.json` |
| 행사 DB | PostgreSQL(`yongin_events`, GCP IAP 터널). 테이블 구조 확인 후 `EventRepository` DB 구현 추가 예정 |
| 테스트 | H2 메모리 DB + 테스트용 행사 JSON (`src/test/resources/application-test.yml`) |

## 오류 코드 (지시서 코드에서 팀 코드로)

| 지시서 | 팀 코드 | HTTP | ErrorCode | 메시지 |
| --- | --- | --- | --- | --- |
| E4001 | C001 | 400 | INVALID_REQUEST | 잘못된 요청입니다. |
| E4003 | C4001 | 400 | SLOT_NOT_DELETABLE | 삭제할 수 없는 항목입니다. |
| E4041 | C4042 | 404 | EVENT_NOT_FOUND | 행사를 찾을 수 없습니다. |
| E4042 | C4043 | 404 | ITINERARY_NOT_FOUND | 일정을 찾을 수 없습니다. |
| E4043 | C4044 | 404 | SLOT_NOT_FOUND | 일정 항목을 찾을 수 없습니다. |
| E4044 | C4045 | 404 | SHARED_ITINERARY_NOT_FOUND | 공유된 일정을 찾을 수 없습니다. |
| E5021 | C5021 | 502 | AI_SERVER_ERROR | AI 서버 호출에 실패했습니다. |
| E5041 | C5041 | 504 | AI_TIMEOUT | AI 응답 시간이 초과되었습니다. |
| C500 | C500 | 500 | UNKNOWN_ERROR | 오류가 발생하였습니다. |

C4041은 팀의 USER_DATA_NOT_FOUND가 쓰고 있어서 404 일련번호를 C4042부터 붙였다. 프론트에 이 표를 공유해야 한다.

## 단계별 상태 (지시서 9장)

| 단계 | 내용 | 상태 |
| --- | --- | --- |
| 1 | 오류 코드, 공통 예외 처리, 보안 경로 | 완료 (테스트 통과, 2026-11-13) |
| 2 | 도메인 모델, 테이블, EventRepository + JSON 구현 | 완료 (테스트 통과, 2026-11-13) |
| 3 | 재계산(Recalculator), 비용, 이동시간 + 단위 테스트 | 완료 (테스트 통과, 2026-11-13) |
| 4 | #1 생성, #2 조회 + 통합 테스트 | 완료 (테스트 통과, 2026-11-13) |
| 5 | #3 추가, #4 삭제, #5 교체, #8 순서 변경 | 완료 (테스트 통과, 2026-11-13) |
| 6 | #6 alternatives, #7 meal-candidates | 완료 (테스트 통과, 2026-11-13) |
| 7 | #9 재추천 | 완료 (테스트 통과, 2026-11-13) |
| 8 | #10, #11 공유 | 완료 (테스트 통과, 2026-11-13) |
| 9 | #12, #13, #14 내 코스 | 완료 (테스트 통과, 2026-11-13) |
| 10 | #15 캘린더 | 완료 (테스트 통과, 2026-11-13) |
| 11 | (선택) LLM | 대기 |
| 12 | 전체 점검 | 대기 |

테스트 실행 (MariaDB, Redis 없이 도는 테스트만, PowerShell):

```
.\gradlew.bat test --tests "com.yongyongthon.be.itinerary.*" --tests "com.yongyongthon.be.course.*"
```

2026-11-13 결과: 7개 테스트 클래스, 61개 테스트 모두 통과 (지시서 10-2 스모크 흐름 `SmokeFlowTest` 포함).

2026-11-13 실제 서버(MariaDB, 실데이터) 확인: Swagger로 15개 API를 생성부터 코스 삭제까지 순서대로 호출해서 모두 기대대로 동작함 (기준 행사 YI-1714, 2026-11-14).

실제 데이터로 돌려 보고 발견한 것 5개는 2026-11-13에 모두 고쳤다 (아래 "지시서와 다르게 정한 것" 참고).

기존 `BeApplicationTests.contextLoads`는 실제 MariaDB와 Redis가 필요하다 (팀 README 참고).

## 엔드포인트 상태

| # | Method | Endpoint | 상태 |
| --- | --- | --- | --- |
| 1 | POST | /api/v1/itineraries | 완료 |
| 2 | GET | /api/v1/itineraries/{itineraryId} | 완료 |
| 3 | POST | /api/v1/itineraries/{itineraryId}/slots | 완료 |
| 4 | DELETE | /api/v1/itineraries/{itineraryId}/slots/{slotId} | 완료 |
| 5 | PUT | /api/v1/itineraries/{itineraryId}/slots/{slotId} | 완료 |
| 6 | GET | /api/v1/itineraries/{itineraryId}/slots/{slotId}/alternatives | 완료 |
| 7 | GET | /api/v1/itineraries/{itineraryId}/slots/{slotId}/meal-candidates | 완료 |
| 8 | PATCH | /api/v1/itineraries/{itineraryId}/slots/order | 완료 |
| 9 | POST | /api/v1/itineraries/{itineraryId}/regenerate | 완료 |
| 10 | POST | /api/v1/itineraries/{itineraryId}/share | 완료 |
| 11 | GET | /api/v1/shared/{shareId} | 완료 |
| 12 | POST | /api/v1/me/courses | 완료 |
| 13 | GET | /api/v1/me/courses | 완료 |
| 14 | DELETE | /api/v1/me/courses/{courseId} | 완료 |
| 15 | GET | /api/v1/me/calendar | 완료 |

## 행사 데이터 변환 규칙 (엑셀에서 JSON으로)

1. 엑셀에 없는 `summary`, `thumbnailUrl`, `runningTimeMin`은 null, `programs`는 빈 배열이다. 그래서 슬롯 `description`, `coverImageUrl`은 항상 null이고 프로그램 슬롯 추가(#3 `type=program`)는 지금 데이터로는 쓸 수 없다.
2. `address`는 '주소', 없으면 '확인된 위치 주소'.
3. `fee`는 '요금' 원문. '.', '-'처럼 글자와 숫자가 없는 값은 null.
4. `isFree`는 요금에 '무료'가 있고 '유료'가 없을 때만 true. 요금이 비어 있으면 false(모름). 도서관 프로그램 대부분이 요금 빈칸이라 비용 미확인으로 나온다.
5. 위도, 경도가 '해당 없음'인 21건(온라인, 여러 장소)은 좌표 null.
6. '중복 검토'가 '중복 확인'인 72건은 근처 행사 후보에서 뺀다 (`duplicateOf`).

## 지시서와 다르게 정한 것 (실제 데이터 확인 후, 2026-11-13 결정)

1. **기준 행사 공식 시간 맞추기 (6-2 변경):** 기준 행사의 `time`이 `HH:mm ~ HH:mm` 한 구간이고 아직 시작 전이면 시작 시각까지 기다렸다가 배치한다. 예: 11:00 ~ 12:00 공연은 10:05가 아니라 11:00에 시작. 이미 시작 시각이 지났으면 기다리지 않는다 (`Recalculator`).
2. **근처 행사 후보 중복 제외 (6-5 추가):** 일정에 이미 있는 행사(또는 먼저 고른 후보)와 기간이 같고, 0.1km 이내이고, 이름(기호와 공백 제외) 앞 8글자 이상이 같으면 같은 행사로 보고 뺀다. 원본에서 중복 표시가 빠진 같은 공연(예: YI-1714, YI-1715)을 막기 위함 (`NearbyFinder`).
3. **근처 행사 후보 분류, 단어 제외 (6-5 추가):** 분류가 "도서관 프로그램"(수강 신청이 필요한 강좌)이거나 이름에 "모집"이 있으면 근처 행사 후보(생성, alternatives)에서 뺀다. 설정값 `app.nearby-excluded-categories`, `app.nearby-excluded-keywords`로 바꿀 수 있다. 기준 행사로 고르거나 `replaceWithEventId`로 직접 넣는 것은 막지 않는다.
4. **Swagger 캘린더 응답:** month 조회와 date 조회의 응답 모양 두 가지를 문서에 `oneOf`로 보여 준다 (`CalendarResponseDocs`, 문서 전용).
5. **Swagger 오류 응답:** 컨트롤러 메서드에 `@ApiErrorCodes`로 낼 수 있는 오류 코드를 적으면 `ApiErrorCodesCustomizer`가 상태 코드별 오류 응답 예시를 문서에 넣는다. C500은 항상 들어간다.

## 가정한 것 (지시서 11장 외에 이번 작업에서 정한 것)

1. 비용은 "1만원", "2만5천원"처럼 만, 천 단위 표기도 숫자로 읽는다 (원본에 이런 표기가 있음).
2. 장소가 아예 없는 슬롯(빈 식사, 빈 근처 행사)에는 COORD_MISSING을 붙이지 않는다. 장소는 있는데 좌표가 없는 슬롯에만 붙인다. **확인 필요**: 지시서 6-2를 글자 그대로 따르면 모든 일정의 식사 슬롯에 경고가 붙는다.
3. 빈 근처 행사 슬롯은 `durationMin`, `startTime`, `endTime`이 null이고 시간을 차지하지 않는다.
4. 생성 시 근처 행사는 가장 가까운 후보부터 넣다가 시간대를 넘는 후보가 나오면 멈춘다. 하나도 못 넣으면 빈 슬롯과 NO_NEARBY_EVENT.
5. `timeBasis=official`은 기준 행사(event) 슬롯에만 적용한다 (6-2 #7 글자 그대로).
6. 일정이 자정을 넘으면 자르지 않고 `24:10`처럼 표기한다.
7. 경고 메시지 문구는 TIME_EXCEEDED만 지시서 예시를 쓰고 나머지는 새로 썼다.
8. 귀가 슬롯 `place`의 name과 address는 출발지 `origin.address`(없으면 null). 카카오맵 링크 이름은 없으면 "출발지".
9. JSON 형식 오류, 쿼리 누락, 타입 불일치는 공통 예외 처리에서 C001로 응답하도록 `GlobalExceptionHandler`에 추가했다 (이전에는 C500).
10. 슬롯 추가(#3)로 넣은 근처 행사와 프로그램도 `isUserAdded=true`로 둔다 (재추천 때 버려짐).
11. 식사 후보 교체(#5)로 바뀐 식사 슬롯은 `isUserAdded=false`(지시서대로)지만, 제목과 비용이 채워져 있어 빈 식사 슬롯으로 보지 않는다. 그래서 다음 `type=meal` 추가는 새 슬롯을 만든다.
12. 내 코스 목록의 `summary`는 지시서 형식 그대로 가운데점(U+00B7)을 쓴다. 코드에는 `\u00B7`로 적었다.
13. 코스 목록 `size`는 1~100만 허용한다 (최댓값은 지시서에 없어서 정함).
14. 코스 저장 시 `title`이 빈 문자열이면 생략한 것으로 보고 일정 제목을 쓴다.
15. 캘린더 `courses[].title`은 코스 제목(사용자가 바꿨으면 바꾼 제목)이다.
16. 관심 행사(`event_likes`)가 아직 없어서 캘린더 `events`는 항상 빈 배열이다 (`InterestEventProvider`).
17. 공유 미리보기는 저장된 일정을 그때 다시 계산해서 보여준다. 일정이 바뀌면 공유 미리보기도 바뀐다.
18. 테이블 컬럼 중 `date`, `type`은 DB 예약어 충돌을 피하려고 `itinerary_date`, `slot_type`으로 바꿨다.
19. 응답 DTO는 `isSaved` 같은 필드 이름을 정확히 맞추려고 Lombok 클래스 대신 record를 쓴다. 일정 엔티티는 양방향 연관이라 `@Data` 대신 `@Getter`, `@Setter`를 쓴다.

## 팀 공통 코드에서 고친 것

1. `SecurityConfiguration`: `/swagger-ui.html`이 `permitAll()`에 없어서 README의 Swagger 주소로 들어가면 401 C4011이 났다. 목록에 추가했다 (`/swagger-ui/index.html`은 원래 열림).

## 확인 필요

1. 행사 DB(PostgreSQL) 테이블 이름과 컬럼 목록
2. 위 가정 2번 (COORD_MISSING 범위)
3. 원격 저장소에 `dev` 브랜치가 이미 있는지 (로컬에서 main 기준으로 새로 만듦)
