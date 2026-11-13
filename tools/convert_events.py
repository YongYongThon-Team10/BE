"""
용인시 축제 행사 엑셀을 서버가 읽는 행사 JSON으로 바꾼다.

사용법:
  pip install openpyxl
  python tools/convert_events.py <엑셀 경로> src/main/resources/data/events_all.json

규칙 (원본에 없는 값은 만들지 않는다):
  1. 필드 이름은 지시서 8장 이름을 쓴다.
  2. summary, thumbnailUrl, runningTimeMin 열은 원본에 없어서 null, programs는 빈 배열.
  3. address는 '주소'가 있으면 그 값, 없으면 '확인된 위치 주소'.
  4. fee는 '요금' 원문. 글자나 숫자가 하나도 없는 값('.', '-' 등)은 null.
  5. isFree는 fee에 '무료'가 있고 '유료'가 없을 때만 true. fee가 null이면 false(모름).
  6. time은 '행사시간' 원문.
  7. duplicateOf는 '중복 검토'가 '중복 확인'인 행의 '중복 대표 행사ID'. 근처 후보에서 빼는 데만 쓴다.
"""
import json
import re
import sys
from datetime import datetime

import openpyxl


def text(v):
    if v is None:
        return None
    s = str(v).strip()
    return s or None


def to_date(v):
    if v is None:
        return None
    if isinstance(v, datetime):
        return v.strftime("%Y-%m-%d")
    s = str(v).strip()
    m = re.match(r"(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})", s)
    return f"{int(m.group(1)):04d}-{int(m.group(2)):02d}-{int(m.group(3)):02d}" if m else None


def to_float(v):
    try:
        return float(v) if v not in (None, "") else None
    except (TypeError, ValueError):
        return None


def clean_fee(v):
    s = text(v)
    if s is None or not re.search(r"[0-9A-Za-z가-힣]", s):
        return None
    return s


def main(src, dst):
    ws = openpyxl.load_workbook(src, read_only=True, data_only=True).worksheets[0]
    rows = ws.iter_rows(values_only=True)
    header = [text(h) for h in next(rows)]
    col = {name: i for i, name in enumerate(header) if name}

    def get(r, name):
        i = col.get(name)
        return r[i] if i is not None and i < len(r) else None

    events = []
    for r in rows:
        if not any(r):
            continue
        fee = clean_fee(get(r, "요금"))
        is_free = fee is not None and "무료" in fee and "유료" not in fee
        dup = text(get(r, "중복 대표 행사ID")) if text(get(r, "중복 검토")) == "중복 확인" else None
        events.append({
            "eventId": text(get(r, "행사ID")),
            "name": text(get(r, "행사명")),
            "category": text(get(r, "분류")),
            "startDate": to_date(get(r, "시작일")),
            "endDate": to_date(get(r, "종료일")),
            "time": text(get(r, "행사시간")),
            "placeName": text(get(r, "장소")),
            "address": text(get(r, "주소")) or text(get(r, "확인된 위치 주소")),
            "lat": to_float(get(r, "위도")),
            "lng": to_float(get(r, "경도")),
            "fee": fee,
            "isFree": is_free,
            "summary": None,
            "thumbnailUrl": None,
            "runningTimeMin": None,
            "programs": [],
            "duplicateOf": dup,
        })

    with open(dst, "w", encoding="utf-8") as f:
        json.dump(events, f, ensure_ascii=False, indent=1)
    print(f"{len(events)} events written to {dst}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
