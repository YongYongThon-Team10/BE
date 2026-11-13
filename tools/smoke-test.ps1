# S5, S6 일정 API 전체 흐름 확인 (지시서 10-2 스모크 테스트를 실제 서버에 돌린다)
# 사용법 (서버를 켠 뒤 BE 폴더에서):
#   powershell -ExecutionPolicy Bypass -File .\tools\smoke-test.ps1
# 다른 주소: -Base http://localhost:8080
param(
    [string]$Base = "http://localhost:8080",
    [string]$BaseEventId = "YI-1714",
    [string]$Date = "2026-11-14"
)

$ErrorActionPreference = "Stop"
$script:pass = 0
$script:fail = 0

function Call([string]$Method, [string]$Path, [string]$Body) {
    $params = @{ Method = $Method; Uri = "$Base$Path"; UseBasicParsing = $true }
    if ($Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = [Text.Encoding]::UTF8.GetBytes($Body)
    }
    try {
        $r = Invoke-WebRequest @params
        $status = [int]$r.StatusCode
        $bytes = $r.RawContentStream.ToArray()
    } catch {
        $err = $_
        $resp = $err.Exception.Response
        if (-not $resp) { throw }
        $status = [int]$resp.StatusCode
        $bytes = @()
        try {
            $stream = $resp.GetResponseStream()
            if ($stream.CanSeek) { $stream.Position = 0 }
            $ms = New-Object IO.MemoryStream
            $stream.CopyTo($ms)
            $bytes = $ms.ToArray()
        } catch { $bytes = @() }
        if ($bytes.Length -eq 0 -and $err.ErrorDetails -and $err.ErrorDetails.Message) {
            $bytes = [Text.Encoding]::UTF8.GetBytes($err.ErrorDetails.Message)
        }
    }
    $text = [Text.Encoding]::UTF8.GetString([byte[]]$bytes)
    $json = $null
    if ($text) { $json = $text | ConvertFrom-Json }
    return [pscustomobject]@{ Status = $status; Json = $json; Text = $text }
}

function Check([string]$Name, [bool]$Ok, [string]$Detail = "") {
    if ($Ok) {
        $script:pass++
        Write-Host ("[통과] " + $Name) -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host ("[실패] " + $Name + "  " + $Detail) -ForegroundColor Red
    }
}

function Types($it) { ($it.slots | ForEach-Object { $_.type }) -join "," }

Write-Host "== 서버: $Base, 기준 행사: $BaseEventId, 날짜: $Date ==" -ForegroundColor Cyan

# 1. 일정 생성
$createBody = @"
{"baseEventId":"$BaseEventId","date":"$Date","origin":{"type":"manual","lat":37.3220,"lng":127.0951,"address":"수지구청역"},"transport":"car","timeWindow":{"start":"10:00","end":"18:00"},"companion":"kids","kidsAge":6,"budget":"any","wants":["food"]}
"@
$r = Call POST "/api/v1/itineraries" $createBody
Check "#1 일정 생성 200" ($r.Status -eq 200) $r.Text
if ($r.Status -ne 200) { Write-Host "일정 생성이 실패해서 멈춥니다."; exit 1 }
$it = $r.Json.data
$id = $it.itineraryId
$last = $it.slots[$it.slots.Count - 1]
Check "#1 귀가 슬롯이 마지막" ($last.type -eq "return") (Types $it)
Check "#1 첫 슬롯은 기준 행사" ($it.slots[0].type -eq "event") (Types $it)
Write-Host ("    itineraryId = " + $id + ", 슬롯: " + (Types $it))

# 2. 일정 조회
$r = Call GET "/api/v1/itineraries/$id"
Check "#2 일정 조회" ($r.Status -eq 200 -and $r.Json.data.itineraryId -eq $id) $r.Text
$r = Call GET "/api/v1/itineraries/itn_nope"
Check "#2 없는 일정은 404 C4043" ($r.Status -eq 404 -and $r.Json.error.code -eq "C4043") $r.Text

# 3. 빈 식사 슬롯 채우기
$r = Call POST "/api/v1/itineraries/$id/slots" '{"type":"meal","title":"점심 국밥","durationMin":40}'
$meal = $r.Json.data.slots | Where-Object { $_.type -eq "meal" } | Select-Object -First 1
Check "#3 빈 식사 슬롯 채움 (slt_2 유지)" ($r.Status -eq 200 -and $meal.slotId -eq "slt_2" -and $meal.title -eq "점심 국밥") $r.Text

# 7. 식사 후보
$r = Call GET "/api/v1/itineraries/$id/slots/slt_2/meal-candidates"
Check "#7 식사 후보 3개" ($r.Status -eq 200 -and $r.Json.data.candidates.Count -eq 3) $r.Text

# 5. 식사 후보로 교체
$r = Call PUT "/api/v1/itineraries/$id/slots/slt_2" '{"replaceWithCandidateId":"cnd_m1"}'
$meal = $r.Json.data.slots | Where-Object { $_.slotId -eq "slt_2" }
Check "#5 식사 교체 (물빛 국수집, 9000원)" ($r.Status -eq 200 -and $meal.title -eq "물빛 국수집" -and $meal.costAmount -eq 9000) $r.Text

# 6. 행사 교체 후보, 받은 후보로 교체
$nearby = $r.Json.data.slots | Where-Object { $_.type -eq "nearbyEvent" } | Select-Object -First 1
if ($nearby) {
    $r = Call GET "/api/v1/itineraries/$id/slots/$($nearby.slotId)/alternatives?limit=3"
    Check "#6 행사 교체 후보 조회" ($r.Status -eq 200) $r.Text
    $cands = @($r.Json.data.candidates)
    Write-Host ("    후보 " + $cands.Count + "개: " + (($cands | ForEach-Object { $_.name }) -join " / "))
    if ($cands.Count -gt 0) {
        $r = Call PUT "/api/v1/itineraries/$id/slots/$($nearby.slotId)" '{"replaceWithCandidateId":"cnd_1"}'
        $changed = $r.Json.data.slots | Where-Object { $_.slotId -eq $nearby.slotId }
        Check "#5 후보 cnd_1로 교체" ($r.Status -eq 200 -and $changed.eventId -eq $cands[0].eventId) $r.Text
    }
} else {
    Write-Host "    근처 행사 슬롯이 없어 #6을 건너뜁니다." -ForegroundColor Yellow
}

# 3, 4. 직접 입력 추가 후 삭제
$r = Call POST "/api/v1/itineraries/$id/slots" '{"type":"custom","title":"카페에서 쉬기","costText":"8,000원"}'
$slots = $r.Json.data.slots
$custom = $slots | Where-Object { $_.title -eq "카페에서 쉬기" }
Check "#3 직접 입력 추가 (귀가 직전, 8000원)" ($r.Status -eq 200 -and $custom.costAmount -eq 8000 -and $slots[$slots.Count - 2].title -eq "카페에서 쉬기") $r.Text
$r = Call DELETE "/api/v1/itineraries/$id/slots/$($custom.slotId)"
Check "#4 슬롯 삭제" ($r.Status -eq 200 -and -not ($r.Json.data.slots | Where-Object { $_.slotId -eq $custom.slotId })) $r.Text
$r = Call DELETE "/api/v1/itineraries/$id/slots/slt_1"
Check "#4 기준 행사 삭제는 400 C4001" ($r.Status -eq 400 -and $r.Json.error.code -eq "C4001") $r.Text

# 8. 순서 변경 (앞의 두 슬롯 바꾸기)
$r = Call GET "/api/v1/itineraries/$id"
$ids = @($r.Json.data.slots | Where-Object { $_.type -ne "return" } | ForEach-Object { $_.slotId })
$swapped = @($ids[1], $ids[0]) + $ids[2..($ids.Count - 1)]
$orderBody = '{"slotIds":[' + (($swapped | ForEach-Object { '"' + $_ + '"' }) -join ",") + ']}'
$r = Call PATCH "/api/v1/itineraries/$id/slots/order" $orderBody
$s = $r.Json.data.slots
Check "#8 순서 변경, 귀가는 마지막" ($r.Status -eq 200 -and $s[0].slotId -eq $ids[1] -and $s[$s.Count - 1].type -eq "return") $r.Text

# 9. 재추천 (이동수단만 바꾸기)
$r = Call POST "/api/v1/itineraries/$id/regenerate" '{"transport":"walk"}'
$d = $r.Json.data
Check "#9 재추천 (id 유지, 조건 일부만 변경, slotId 새로)" ($r.Status -eq 200 -and $d.itineraryId -eq $id -and $d.transport -eq "walk" -and $d.timeWindow.start -eq "10:00" -and $d.slots[0].slotId -ne "slt_1") $r.Text
Write-Host ("    재추천 슬롯: " + (Types $d) + ", 첫 slotId = " + $d.slots[0].slotId)

# 12. 내 코스 저장 (두 번 저장해도 같은 courseId)
$r = Call POST "/api/v1/me/courses" ('{"itineraryId":"' + $id + '"}')
$courseId = $r.Json.data.courseId
Check "#12 코스 저장" ($r.Status -eq 200 -and $r.Json.data.isSaved -eq $true) $r.Text
$r = Call POST "/api/v1/me/courses" ('{"itineraryId":"' + $id + '","title":"아이랑 수지 나들이"}')
Check "#12 같은 일정 다시 저장하면 같은 courseId" ($r.Status -eq 200 -and $r.Json.data.courseId -eq $courseId -and $r.Json.data.title -eq "아이랑 수지 나들이") $r.Text
$r = Call GET "/api/v1/itineraries/$id"
Check "#2 저장 후 isSaved = true" ($r.Json.data.isSaved -eq $true) $r.Text

# 13. 코스 목록
$r = Call GET "/api/v1/me/courses"
$mine = $r.Json.data.courses | Where-Object { $_.courseId -eq $courseId }
Check "#13 목록에 방금 저장한 코스" ($r.Status -eq 200 -and $mine) $r.Text
if ($mine) { Write-Host ("    summary = " + $mine.summary + ", mealSummary = " + $mine.mealSummary) }

# 15. 캘린더
$month = $Date.Substring(0, 7)
$r = Call GET "/api/v1/me/calendar?month=$month"
Check "#15 캘린더 month에 코스" ($r.Status -eq 200 -and ($r.Json.data.courses | Where-Object { $_.itineraryId -eq $id })) $r.Text
$r = Call GET "/api/v1/me/calendar?date=$Date"
Check "#15 캘린더 date에 코스" ($r.Status -eq 200 -and $r.Json.data.courseCount -ge 1) $r.Text
$r = Call GET "/api/v1/me/calendar?month=$month&date=$Date"
Check "#15 month와 date 동시는 400 C001" ($r.Status -eq 400 -and $r.Json.error.code -eq "C001") $r.Text

# 10, 11. 공유
$r = Call POST "/api/v1/itineraries/$id/share"
$shareId = $r.Json.data.shareId
Check "#10 공유 링크" ($r.Status -eq 200 -and $shareId) $r.Text
Write-Host ("    shareUrl = " + $r.Json.data.shareUrl)
$r = Call POST "/api/v1/itineraries/$id/share"
Check "#10 같은 일정은 같은 shareId" ($r.Json.data.shareId -eq $shareId) $r.Text
$r = Call GET "/api/v1/shared/$shareId"
$sh = $r.Json.data
$ret = $sh.slots[$sh.slots.Count - 1]
Check "#11 공유 미리보기 (slotId 없음, 귀가 place null)" ($r.Status -eq 200 -and -not ($r.Text -match '"slotId"') -and $ret.type -eq "return" -and $null -eq $ret.place) $r.Text
Check "#11 출발지 주소 노출 안 됨" (-not ($r.Text -match "수지구청역")) $r.Text
$r = Call GET "/api/v1/shared/shr_nope"
Check "#11 없는 공유는 404 C4045" ($r.Status -eq 404 -and $r.Json.error.code -eq "C4045") $r.Text

# 14. 코스 삭제
$r = Call DELETE "/api/v1/me/courses/$courseId"
Check "#14 코스 삭제" ($r.Status -eq 200 -and $r.Json.data.deleted -eq $true) $r.Text
$r = Call GET "/api/v1/me/courses"
Check "#14 삭제 후 목록에 없음" (-not ($r.Json.data.courses | Where-Object { $_.courseId -eq $courseId })) $r.Text
$r = Call DELETE "/api/v1/me/courses/$courseId"
Check "#14 다시 삭제하면 404 C4043" ($r.Status -eq 404 -and $r.Json.error.code -eq "C4043") $r.Text

# 인증 헤더 무시
$r = Invoke-WebRequest -UseBasicParsing -Uri "$Base/api/v1/itineraries/$id" -Headers @{ Authorization = "Bearer not-a-real-token" }
Check "인증 헤더가 있어도 정상" ([int]$r.StatusCode -eq 200)

Write-Host ""
$color = "Green"
if ($script:fail -gt 0) { $color = "Red" }
Write-Host ("결과: 통과 " + $script:pass + "개, 실패 " + $script:fail + "개") -ForegroundColor $color
Write-Host ("확인한 일정: " + $Base + "/api/v1/itineraries/" + $id)
