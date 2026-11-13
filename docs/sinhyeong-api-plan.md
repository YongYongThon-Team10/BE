# 신형 담당 API 구현 계획

기준 문서는 신형 담당 API 세부 명세(`yongin_sinhyeong_api_spec.md`)와 행사 엑셀(`용인시 축제 행사 데이터 공유용.xlsx`, 1,825행)이다. 둘 다 저장소 밖에 있다. 대상은 E1, E2, E3, E4, E5, E14, E19, E20 여덟 개 엔드포인트다.

> 마지막 갱신: 2026-11-14, PR 8 머지 기준으로 정리. Swagger 오류 코드 문서 추가

## 확정한 결정 (2026-11-13)

1. 경로 접두사는 명세 그대로 `/api/...`를 쓴다 (`/api/v1/...` 아님)
2. 연간 계획 행사는 날짜 필터에 포함하고, `sort=date`에서는 뒤로 보낸다. 엑셀에서는 시작일과 종료일이 전 행에 채워져 있다
3. `indoorOutdoor`, `companion` 필터는 파라미터만 받고 무시한다. 엑셀에 근거 컬럼이 없어 걸면 결과가 0건이 되기 때문이다. 근거 컬럼이 생기면 적용한다
4. `budget=free`는 엑셀 `요금`에 무료가 확인된 행사로 처리한다
5. `category`(공연, 행사)는 엑셀 `분류`가 `공연`이면 공연, 그 외는 행사로 만든다. `subCategory`는 `분류` 원문(15종)을 그대로 쓴다
6. 중복 검토가 `중복 확인`, `중복 일정`인 73건과 일정상태가 `취소 공지`인 1건은 적재에서 제외한다 (적재 결과 1,751건)
7. 엑셀은 저장소에 올리지 않고 로컬에서만 적재한다. 출처 URL과 비고가 들어 있어 팀 확인 전에는 올리지 않는다
8. `view`는 선택이고 기본값은 `list`다
9. 검증 실패 코드는 신형 API만 E4001, 인증 API는 기존 C002를 유지한다
10. 목록 필터와 정렬은 행사 전체를 읽어 메모리에서 처리한다 (4단계에서 Specification 대신 변경. 데이터가 약 1,800건이라 충분하고 DB 없이 테스트할 수 있다. 수만 건이 되면 DB 쿼리로 옮긴다)
11. 신형이 쓰는 E 코드는 E4001, E4002, E4041, E5021, E5041이다

12. AI 호출(E4)은 BE가 OpenAI API를 직접 호출한다. 키는 환경변수 `OPENAI_API_KEY`, 모델은 `OPENAI_MODEL`로 바꿀 수 있게 한다. 키가 없거나 호출이 실패하면 502 E5021
13. AI 타임아웃은 E4 15초(`AI_SEARCH_PARSE_TIMEOUT_SECONDS`). E5 60초는 E5가 AI를 호출하게 되면(S5) 적용한다. 넘으면 504 E5041
14. E14 데이터원은 카카오 로컬 API(키워드 검색). REST API 키는 환경변수 `KAKAO_REST_API_KEY`
15. E5는 신형이 요청 DTO와 검증(400 E4001, 404 E4041)까지 만들고, 일정 생성 본체와 응답은 석훈 신(S5)이 채운다

16. (2026-11-14, PR 8 머지) 팀원이 충돌을 풀며 신형 구조로 통일했다. 경로는 명세의 `/api/...`(인증만 `/api/v1/auth`), 오류 코드는 명세의 E 코드. 팀원 일정, 코스 API도 여기에 맞췄다
17. (2026-11-14, PR 8 머지) 행사 데이터는 MariaDB `events`, `event_sources`만 읽는다. 일정, 내 코스는 `EventCatalog`로 같은 테이블을 읽는다. 데이터는 `seed_events.py`로 넣는데, 기본 출처는 팀 행사 DB(`--db`, GCP IAP 터널 필요)이고 엑셀은 예비다
18. (2026-11-14, PR 8 머지) E5 생성 요청은 신형의 `ItineraryCreateRequestDto`와 `ItineraryRequestValidator`를 쓰고, 생성과 응답은 팀원 코드다. 재추천 검증은 팀원의 `ItineraryConditionValidator`
19. 관심 행사(E19, E20)는 로그인 필수로 유지한다. 팀원 캘린더의 관심 행사 목록은 `favorites`를 읽는다

## 엑셀 적재 규칙

1. 구(district)가 비어 있는 행은 `확인된 위치 주소`에서 처인구, 기흥구, 수지구를 추출해 채운다 (387건 중 370건 가능, 나머지는 null)
2. 위도, 경도가 `해당 없음` 문자열인 21건은 null로 적재한다 (지도 마커 제외)
3. `dateStatus`는 명세의 4종으로 변환한다. 공개 일정, 개최 확인, 기간상 진행 중은 확정, 공개된 예정 일정은 예정, 연간 계획과 안전관리 계획은 연간 계획, 날짜 미확인은 미확인
4. `address`는 엑셀 `주소`, 없으면 `확인된 위치 주소`를 쓴다. 두 값 모두 도로명 주소라 동 이름이 거의 없어서 `dong` 필터(4단계)는 결과가 매우 적다
5. `coordPrecision`은 `좌표 검토` 값으로 정한다. 시설 대표점, 지번 좌표, 주소 좌표, 확인된 행사 장소, 확인된 회차 장소, 최종 공지 행사장은 place, 복수 장소, 안내 기준점, 집결 및 교육 장소, 행사 구간 기준점은 area, 온라인과 장소별 상이는 좌표 없음. 처음 보는 값이 나오면 스크립트가 멈춘다
6. `isFree`는 요금에 무료가 있으면 true, 유료나 금액이 있으면 false, 그 외 null
7. `applyType`은 신청링크가 있으면 apply, 없으면 null. 엑셀에 현장 참여 표시가 없어 walkin은 쓰지 않는다
8. `category`는 `분류`가 공연이면 공연, 그 외 행사
9. 출처는 `대표출처URL`과 `추가출처URL`(` | `로 구분)을 URL 하나당 한 줄로 만든다. 출처명은 `출처명`을 같은 순서로 붙이고, URL이 더 많으면 마지막 출처명을 쓴다. 유형은 출처명에 블로그가 있으면 blog, 뉴스룸이면 etc, 그 외 official
10. 변환은 `scripts/seed_events.py`가 한다. 이 규칙을 바꾸면 스크립트도 같이 고친다
11. 엑셀에 없는 필드(`summary`, `themeLabel`, `thumbnailUrl`, `indoorOutdoor`, `tags`, `notice`, 프로그램, 출처별 상이 값)는 null 또는 빈 배열로 응답한다. 추측해서 채우지 않는다

## 단계

| 단계 | 내용 | 선행 |
| --- | --- | --- |
| 1 | **완료.** 공통 기반: E 코드 5개와 예외, 신형 공개 경로 `permitAll`, E4001 검증, 문서 갱신. AI 예외 클래스(E5021, E5041)는 7단계에서 추가 | 없음 |
| 2 | **완료 (2026-11-13 실제 MariaDB에서 적재 1,751건과 `schema.sql` validate 통과 확인).** `events`(신청 정보 포함), `event_sources` 엔티티와 저장소, `db/schema.sql`, 적재 스크립트 `scripts/seed_events.py`. 프로그램 데이터가 엑셀에 없어 `event_programs`는 만들지 않고 E2에서 빈 배열로 응답한다. `favorites`는 6단계에서 만든다 | 1 |
| 3 | **완료.** E2 상세 `GET /api/events/{eventId}`. 데이터 없는 필드는 null, 프로그램은 빈 배열, `isLiked`와 `hasDiscrepancy`는 false 고정 | 2 |
| 4 | **완료.** E1 목록과 지도 `GET /api/events`. 필터(기간 겹침, 구, 동, 무료), 날짜순과 거리순, 페이지, `view=map`, E4001과 E4002 | 2 |
| 5 | **완료.** E3 안내 상이 `GET /api/events/{eventId}/discrepancies`. 출처별 값이 없어 항상 `hasDiscrepancy=false`, `items=[]` | 2 |
| 6 | **완료.** E19, E20 `PUT`, `DELETE /api/me/favorites/{eventId}` (로그인 필요, 멱등, 명세의 제안 본문 그대로). E1, E2의 `isLiked` 반영. `favorites` 테이블 추가 | 2 |
| 7 | **완료 (실제 OpenAI 호출은 미검증).** E4 `POST /api/search/parse`. `OpenAiClient`(JDK HttpClient, JSON 모드), 모델 출력 정리, 타임아웃 15초. 기본 모델 `gpt-4o-mini`는 키를 넣고 동작을 확인해야 함 | 1 |
| 8 | **완료 (실제 카카오 호출은 미검증).** E14 `GET /api/places/search`. 공개 API, 최대 15건, 실패는 E5022(명세에 없는 코드) | 1 |
| 9 | **완료.** E5 요청 DTO `ItineraryCreateRequestDto`와 `ItineraryRequestValidator`. 컨트롤러, 생성, 응답은 팀원 코드(PR 8에서 연결) | 2 |
| 10 | 단계마다 `docs/architecture.md` 갱신 | 매 단계 |

각 단계는 `dev`에서 딴 브랜치 하나와 PR 하나로 진행한다. 커밋과 푸시는 사용자가 직접 한다.

## 남은 작업

1. 실제 OpenAI, 카카오 호출 확인 (키 필요)
2. 팀 행사 DB에서 적재 (gcloud 설치와 로그인, IAP 터널 필요). 첫 실행 때 컬럼 이름 확인
3. 명세서01에 없는 정의(E14 본문과 공개 여부, E5022, E19와 E20 경로와 본문)를 Notion에 추가하고 팀 확인
4. 데이터가 생기면: 안내 상이(E1, E2, E3), 프로그램 목록(E2), 동행과 실내외 필터(E1)
