"""팀 행사 DB(GCP PostgreSQL yongin_events의 events) 또는 행사 엑셀을 events, event_sources INSERT SQL로 바꾼다.

기본 출처는 팀 행사 DB다. 앱은 MariaDB의 events 테이블만 읽으므로, 이 스크립트로 한 번 옮겨 넣는다.

1. 팀 행사 DB에서 (기본)
   IAP 터널을 먼저 켠다:
     gcloud compute start-iap-tunnel yongin-postgres 5432 --zone=us-central1-a --local-host-port=127.0.0.1:15432
   접속 정보 파일(팀원에게 받은 PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD, PGSSLMODE)을 저장소 밖에 두고:
     pip install "psycopg[binary]"
     python3 scripts/seed_events.py --db --env-file <접속 정보 파일> > build/seed_events.sql
   컬럼 이름이 맞지 않으면 --show-columns로 행사 DB의 컬럼을 보고 DB_COLUMNS를 고친다.
     python3 scripts/seed_events.py --db --env-file <접속 정보 파일> --show-columns
   파일 없이 환경변수(PG*)만으로도 접속한다. 비밀번호는 저장소에 올리지 않는다.

2. 엑셀에서 (예비)
   엑셀은 저장소에 올리지 않는다. 각자 로컬에 두고 실행한다.
     pip install openpyxl
     python3 scripts/seed_events.py <엑셀 경로> > build/seed_events.sql

적재:
    docker exec -i be-mariadb mariadb -uroot -proot be < build/seed_events.sql

테이블이 먼저 있어야 한다 (bootRun 한 번 또는 db/schema.sql). 실행할 때마다 두 테이블을 비우고 다시 넣는다.
관심 행사(favorites)가 행사를 참조하므로, 다시 적재하면 지워진 행사ID를 가리키는 관심 행사는 FK가 꺼진 동안 남는다.
행사ID가 바뀌지 않는 한 문제없지만, 행사를 빼고 다시 적재했다면 favorites도 정리해야 한다.
변환 규칙은 docs의 sinhyeong-api-plan.md "엑셀 적재 규칙"과 같다. 바꾸면 그 문서도 고친다.
python3 scripts/seed_events.py --self-test 로 변환 함수를 검사한다.
"""
import os
import re
import sys
from datetime import date, datetime
from decimal import Decimal

DISTRICTS = ("처인구", "기흥구", "수지구")

# 엑셀 일정상태를 명세의 dateStatus로
DATE_STATUS = {
    "공개 일정": "확정",
    "개최 확인": "확정",
    "기간상 진행 중": "확정",
    "공개된 예정 일정": "예정",
    "연간 계획": "연간 계획",
    "안전관리 계획": "연간 계획",
    "날짜 미확인": "미확인",
}

# 취소된 행사는 적재하지 않는다 (취소 공지를 담을 notice 컬럼이 아직 없다)
CANCELLED = "취소 공지"

# 엑셀 좌표 검토 값. NO_COORDINATE는 좌표 없음, 그 외 처음 보는 값은 오류로 멈춘다
COORD_PRECISION = {
    "시설 대표점": "place",
    "지번 좌표": "place",
    "주소 좌표": "place",
    "확인된 행사 장소": "place",
    "확인된 회차 장소": "place",
    "최종 공지 행사장": "place",
    "복수 장소": "area",
    "안내 기준점": "area",
    "집결·교육 장소": "area",
    "행사 구간 기준점": "area",
}
NO_COORDINATE = {"온라인", "장소별 상이"}

# 적재에서 빼는 중복 행
DUPLICATE_MARKS = {"중복 확인", "중복 일정"}


def text(v):
    """빈 문자열과 자리표시자('.', ''-', '해당 없음')는 None."""
    if v is None:
        return None
    s = str(v).strip()
    return None if s in ("", ".", "'-", "-", "해당 없음") else s


def district_of(gu, *addresses):
    if text(gu):
        return gu
    for a in addresses:
        m = re.search("|".join(DISTRICTS), a or "")
        if m:
            return m.group(0)
    return None


def is_free(fee):
    if fee is None:
        return None
    if "무료" in fee:
        return True
    if "유료" in fee or "원" in fee:
        return False
    return None  # 예: 프로그램에 따라 상이


def coordinate(lat, lng, review):
    """좌표가 숫자가 아니거나 위치가 확정되지 않은 행은 (None, None, None)."""
    if review in NO_COORDINATE:
        return None, None, None
    if review not in COORD_PRECISION:
        raise ValueError(f"처음 보는 좌표 검토 값: {review}")
    precision = COORD_PRECISION[review]
    if isinstance(lat, Decimal):
        lat = float(lat)
    if isinstance(lng, Decimal):
        lng = float(lng)
    if not isinstance(lat, (int, float)) or not isinstance(lng, (int, float)):
        return None, None, None
    return float(lat), float(lng), precision


def receipt_time(v):
    """'2025.12.3 10:00' 또는 '2026.3.29' 형식. 행사 DB가 날짜, 시각 타입으로 주면 그대로 쓴다."""
    if isinstance(v, datetime):
        return v.replace(tzinfo=None)
    if isinstance(v, date):
        return datetime(v.year, v.month, v.day)
    s = text(v)
    if s is None:
        return None
    m = re.fullmatch(r"(\d{4})\.(\d{1,2})\.(\d{1,2})(?: (\d{1,2}):(\d{2}))?", s)
    if not m:
        raise ValueError(f"접수 일시 형식 오류: {s}")
    y, mo, d, hh, mm = m.groups()
    return datetime(int(y), int(mo), int(d), int(hh or 0), int(mm or 0))


def source_type(name):
    if "블로그" in name:
        return "blog"
    if "뉴스룸" in name:
        return "etc"
    return "official"


def sources(names, main_url, extra_urls):
    """URL 한 개당 출처 한 줄. 출처명은 URL과 같은 순서로 적혀 있고, URL이 더 많으면 마지막 출처명을 쓴다."""
    names = [n.strip() for n in names.split(" | ")]
    urls = [main_url.strip()] + [u.strip() for u in (extra_urls or "").split(" | ") if u.strip()]
    result = []
    for i, url in enumerate(urls):
        name = names[i] if i < len(names) else names[-1]
        result.append((name, source_type(name), url, i))
    return result


def to_day(v):
    return v.date() if isinstance(v, datetime) else v


def sql(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, Decimal):
        return str(v)
    if isinstance(v, (int, float)):
        return repr(v)
    if isinstance(v, datetime):
        return f"'{v:%Y-%m-%d %H:%M:%S}'"
    if isinstance(v, date):
        return f"'{v:%Y-%m-%d}'"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def date_status(v):
    if v not in DATE_STATUS:
        raise ValueError(f"처음 보는 일정상태 값: {v}")
    return DATE_STATUS[v]


def excel_rows(path):
    """엑셀 첫 시트의 행을 {헤더: 값} 딕셔너리로."""
    import openpyxl

    sheet = openpyxl.load_workbook(path, read_only=True, data_only=True).worksheets[0]
    rows = sheet.iter_rows(values_only=True)
    header = list(next(rows))
    for r in rows:
        yield {name: r[i] for i, name in enumerate(header) if name is not None}


# 엑셀 헤더별 행사 DB 컬럼 후보. 앞에서부터 처음 있는 컬럼을 쓴다.
# 필수 항목의 컬럼이 하나도 없으면 멈춘다. 선택 항목은 없으면 빈 값으로 둔다.
DB_COLUMNS = {
    "행사ID": ["event_id"],
    "행사명": ["title", "name"],
    "분류": ["source_category", "category"],
    "시작일": ["start_date"],
    "종료일": ["end_date"],
    "일정상태": ["schedule_status", "date_status", "status"],
    "행사시간": ["event_hours", "event_time", "time"],
    "장소": ["venue", "place_name", "place"],
    "주소": ["address"],
    "확인된 위치 주소": ["verified_address"],
    "구": ["district", "gu"],
    "위도": ["latitude", "lat"],
    "경도": ["longitude", "lng"],
    "좌표 검토": ["coordinate_review", "coord_review", "coordinate_status", "location_review"],
    "요금": ["fee"],
    "대상": ["target", "target_audience", "audience"],
    "신청링크": ["apply_url", "application_url", "registration_url"],
    "접수시작": ["receipt_start", "application_start", "registration_start"],
    "접수마감": ["receipt_end", "application_end", "registration_end"],
    "접수상태": ["receipt_status", "application_status", "registration_status"],
    "출처명": ["source_names", "source_name"],
    "대표출처URL": ["source_url", "primary_source_url", "representative_source_url"],
    "추가출처URL": ["extra_source_urls", "additional_source_urls", "source_urls_extra"],
    "중복 검토": ["duplicate_status"],
}
DB_REQUIRED = {"행사ID", "행사명", "분류", "시작일", "종료일", "일정상태", "위도", "경도", "좌표 검토",
               "출처명", "대표출처URL", "중복 검토"}


def read_env_file(path):
    """KEY=VALUE 줄만 읽는다. export 접두어와 따옴표는 지운다. 값은 출력하지 않는다."""
    values = {}
    with open(path, encoding="utf-8-sig") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            if key.startswith("export "):
                key = key[len("export "):].strip()
            values[key] = value.strip().strip('"').strip("'")
    return values


def db_connect(env_file):
    try:
        import psycopg
    except ImportError:
        sys.exit('psycopg가 없습니다: pip install "psycopg[binary]"')
    env = dict(os.environ)
    if env_file:
        env.update(read_env_file(env_file))
    params = {
        "host": env.get("PGHOST", "127.0.0.1"),
        "port": env.get("PGPORT", "15432"),
        "dbname": env.get("PGDATABASE", "yongin_events"),
        "user": env.get("PGUSER", "yongin_reader"),
        "password": env.get("PGPASSWORD"),
        "sslmode": env.get("PGSSLMODE", "require"),
    }
    if not params["password"]:
        sys.exit("PGPASSWORD가 없습니다. --env-file로 접속 정보 파일을 주거나 환경변수 PGPASSWORD를 설정하세요.")
    try:
        return psycopg.connect(**params)
    except psycopg.OperationalError as e:
        sys.exit(f"행사 DB 접속 실패 ({params['host']}:{params['port']}/{params['dbname']}). "
                 f"IAP 터널이 켜져 있는지 확인하세요. 원인: {e}")


def db_columns(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT * FROM events LIMIT 0")
        return [d.name for d in cur.description]


def db_rows(conn):
    """행사 DB events의 행을 엑셀 헤더 이름의 딕셔너리로 바꾼다."""
    columns = db_columns(conn)
    chosen, missing = {}, []
    for header, candidates in DB_COLUMNS.items():
        found = next((c for c in candidates if c in columns), None)
        if found:
            chosen[header] = found
        elif header in DB_REQUIRED:
            missing.append(f"{header} (후보: {', '.join(candidates)})")
    if missing:
        sys.exit("행사 DB에서 필수 컬럼을 찾지 못했습니다. DB_COLUMNS를 고치세요.\n  "
                 + "\n  ".join(missing) + "\n행사 DB 컬럼: " + ", ".join(columns))
    skipped_optional = [h for h in DB_COLUMNS if h not in chosen]
    if skipped_optional:
        print("행사 DB에 없어 빈 값으로 두는 항목: " + ", ".join(skipped_optional), file=sys.stderr)

    with conn.cursor() as cur:
        cur.execute("SELECT * FROM events ORDER BY event_id")
        names = [d.name for d in cur.description]
        for r in cur:
            row = dict(zip(names, r))
            yield {header: row[col] for header, col in chosen.items()}


def convert_rows(rows):
    """{엑셀 헤더: 값} 행들을 INSERT SQL로. 엑셀과 행사 DB가 같은 규칙을 쓴다."""
    events, event_sources, skipped = [], [], 0
    for row in rows:
        g = row.get
        event_id = text(g("행사ID"))
        if not event_id:
            continue
        if text(g("중복 검토")) in DUPLICATE_MARKS or text(g("일정상태")) == CANCELLED:
            skipped += 1
            continue

        try:
            lat, lng, precision = coordinate(g("위도"), g("경도"), text(g("좌표 검토")))
            address = text(g("주소")) or text(g("확인된 위치 주소"))
            fee = text(g("요금"))
            apply_url = text(g("신청링크"))
            sub_category = text(g("분류"))
            events.append((
                event_id, text(g("행사명")),
                "공연" if sub_category == "공연" else "행사", sub_category,
                to_day(g("시작일")), to_day(g("종료일")), date_status(text(g("일정상태"))),
                text(g("행사시간")), text(g("장소")), address,
                district_of(text(g("구")), text(g("주소")), text(g("확인된 위치 주소"))),
                lat, lng, precision,
                fee, is_free(fee), text(g("대상")),
                "apply" if apply_url else None, apply_url,
                receipt_time(g("접수시작")), receipt_time(g("접수마감")), text(g("접수상태")),
                text(g("대표출처URL")),
            ))
            for name, kind, url, order in sources(text(g("출처명")), text(g("대표출처URL")), text(g("추가출처URL"))):
                event_sources.append((event_id, name, kind, url, order))
        except (ValueError, KeyError, AttributeError) as e:
            raise SystemExit(f"{event_id} 변환 실패: {e}")

    out = ["SET FOREIGN_KEY_CHECKS = 0;", "DELETE FROM event_sources;", "DELETE FROM events;",
           "SET FOREIGN_KEY_CHECKS = 1;"]
    event_cols = ("event_id, name, category, sub_category, start_date, end_date, date_status, time, place_name, "
                  "address, district, lat, lng, coord_precision, fee, is_free, target, apply_type, apply_url, "
                  "receipt_start, receipt_end, receipt_status, source_url")
    for i in range(0, len(events), 200):
        values = ",\n".join("(" + ", ".join(sql(v) for v in e) + ")" for e in events[i:i + 200])
        out.append(f"INSERT INTO events ({event_cols}) VALUES\n{values};")
    for i in range(0, len(event_sources), 200):
        values = ",\n".join("(" + ", ".join(sql(v) for v in s) + ")" for s in event_sources[i:i + 200])
        out.append(f"INSERT INTO event_sources (event_id, name, type, url, sort_order) VALUES\n{values};")

    print(f"events {len(events)}, event_sources {len(event_sources)}, 중복과 취소 제외 {skipped}", file=sys.stderr)
    return "\n".join(out) + "\n"


def convert(path):
    return convert_rows(excel_rows(path))


def self_test():
    assert text(".") is None and text(" 무료 ") == "무료" and text("해당 없음") is None
    assert district_of(None, None, "경기 용인시 수지구 포은대로 499") == "수지구"
    assert district_of("처인구", None, None) == "처인구"
    assert district_of(None, None, "경기 광주시") is None
    assert is_free("전석무료(네이버예약)") is True
    assert is_free("유료 / 전석 15000원") is False and is_free("10,000원") is False
    assert is_free("프로그램에 따라 상이") is None and is_free(None) is None
    assert coordinate("해당 없음", "해당 없음", "온라인") == (None, None, None)
    assert coordinate(37.2, 127.1, "복수 장소") == (37.2, 127.1, "area")
    assert coordinate(37.2, 127.1, "장소별 상이") == (None, None, None)
    assert receipt_time("2025.12.3 10:00") == datetime(2025, 12, 3, 10, 0)
    assert receipt_time("2026.3.29") == datetime(2026, 3, 29)
    s = sources("용인시청 문화관광포털 | 용인관광 공식 블로그", "https://a", "https://b | https://c")
    assert s == [("용인시청 문화관광포털", "official", "https://a", 0),
                 ("용인관광 공식 블로그", "blog", "https://b", 1),
                 ("용인관광 공식 블로그", "blog", "https://c", 2)]
    assert sql("it's") == "'it''s'" and sql(None) == "NULL" and sql(True) == "1"
    assert coordinate(Decimal("37.25"), Decimal("127.1"), "주소 좌표") == (37.25, 127.1, "place")
    assert receipt_time(datetime(2025, 12, 3, 10, 0)) == datetime(2025, 12, 3, 10, 0)
    assert receipt_time(date(2026, 3, 29)) == datetime(2026, 3, 29)
    base = {"행사ID": "YI-0001", "행사명": "테스트 공연", "분류": "공연", "시작일": date(2026, 11, 1),
            "종료일": date(2026, 11, 2), "일정상태": "공개 일정", "위도": Decimal("37.2"), "경도": Decimal("127.1"),
            "좌표 검토": "주소 좌표", "요금": "무료", "출처명": "용인시청", "대표출처URL": " https://a ",
            "중복 검토": None}
    dup = dict(base, **{"행사ID": "YI-0002", "중복 검토": "중복 확인"})
    cancelled = dict(base, **{"행사ID": "YI-0003", "일정상태": "취소 공지"})
    out = convert_rows([base, dup, cancelled])
    assert "'YI-0001', '테스트 공연', '공연', '공연'" in out and "'https://a'" in out
    assert "YI-0002" not in out and "YI-0003" not in out
    print("ok")


def main(argv):
    if argv == ["--self-test"]:
        self_test()
        return
    if argv and argv[0] == "--db":
        rest = argv[1:]
        env_file = None
        if "--env-file" in rest:
            i = rest.index("--env-file")
            if i + 1 >= len(rest):
                sys.exit(__doc__)
            env_file = rest[i + 1]
        conn = db_connect(env_file)
        try:
            if "--show-columns" in rest:
                print("\n".join(db_columns(conn)))
                return
            sys.stdout.write(convert_rows(db_rows(conn)))
        finally:
            conn.close()
        return
    if len(argv) != 1:
        sys.exit(__doc__)
    sys.stdout.write(convert(argv[0]))


if __name__ == "__main__":
    main(sys.argv[1:])
