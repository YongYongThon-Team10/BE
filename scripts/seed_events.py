"""행사 엑셀(용인시 축제 행사 데이터 공유용.xlsx)을 events, event_sources INSERT SQL로 바꾼다.

엑셀은 저장소에 올리지 않는다. 각자 로컬에 두고 실행한다.

    pip install openpyxl
    python3 scripts/seed_events.py <엑셀 경로> > build/seed_events.sql
    docker exec -i be-mariadb mariadb -uroot -proot be < build/seed_events.sql

테이블이 먼저 있어야 한다 (bootRun 한 번 또는 db/schema.sql). 실행할 때마다 두 테이블을 비우고 다시 넣는다.
관심 행사(favorites)가 행사를 참조하므로, 다시 적재하면 지워진 행사ID를 가리키는 관심 행사는 FK가 꺼진 동안 남는다.
행사ID가 바뀌지 않는 한 문제없지만, 행사를 빼고 다시 적재했다면 favorites도 정리해야 한다.
변환 규칙은 docs의 sinhyeong-api-plan.md "엑셀 적재 규칙"과 같다. 바꾸면 그 문서도 고친다.
python3 scripts/seed_events.py --self-test 로 변환 함수를 검사한다.
"""
import re
import sys
from datetime import date, datetime

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
    precision = COORD_PRECISION[review]
    if not isinstance(lat, (int, float)) or not isinstance(lng, (int, float)):
        return None, None, None
    return float(lat), float(lng), precision


def receipt_time(v):
    """'2025.12.3 10:00' 또는 '2026.3.29' 형식."""
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
    if isinstance(v, (int, float)):
        return repr(v)
    if isinstance(v, datetime):
        return f"'{v:%Y-%m-%d %H:%M:%S}'"
    if isinstance(v, date):
        return f"'{v:%Y-%m-%d}'"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def convert(path):
    import openpyxl

    sheet = openpyxl.load_workbook(path, read_only=True, data_only=True).worksheets[0]
    rows = sheet.iter_rows(values_only=True)
    header = list(next(rows))
    col = {name: i for i, name in enumerate(header)}

    events, event_sources, skipped = [], [], 0
    for r in rows:
        g = lambda name: r[col[name]]
        if not g("행사ID"):
            continue
        if g("중복 검토") in DUPLICATE_MARKS or g("일정상태") == CANCELLED:
            skipped += 1
            continue

        lat, lng, precision = coordinate(g("위도"), g("경도"), g("좌표 검토"))
        address = text(g("주소")) or text(g("확인된 위치 주소"))
        fee = text(g("요금"))
        apply_url = text(g("신청링크"))
        events.append((
            g("행사ID"), text(g("행사명")),
            "공연" if g("분류") == "공연" else "행사", text(g("분류")),
            to_day(g("시작일")), to_day(g("종료일")), DATE_STATUS[g("일정상태")],
            text(g("행사시간")), text(g("장소")), address,
            district_of(g("구"), g("주소"), g("확인된 위치 주소")),
            lat, lng, precision,
            fee, is_free(fee), text(g("대상")),
            "apply" if apply_url else None, apply_url,
            receipt_time(g("접수시작")), receipt_time(g("접수마감")), text(g("접수상태")),
            g("대표출처URL").strip(),
        ))
        for name, kind, url, order in sources(g("출처명"), g("대표출처URL"), g("추가출처URL")):
            event_sources.append((g("행사ID"), name, kind, url, order))

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
    print("ok")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    if sys.argv[1] == "--self-test":
        self_test()
    else:
        sys.stdout.write(convert(sys.argv[1]))
