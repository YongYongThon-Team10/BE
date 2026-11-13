package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.EventProgram;
import com.yongyongthon.be.event.data.dto.NearbyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 행사 JSON 파일(tools/convert_events.py로 엑셀에서 변환)을 시작할 때 읽어 메모리에 둔다.
 */
@Slf4j
@Repository
public class JsonEventRepository implements EventRepository {

    private final Map<String, EventInfo> events;

    public JsonEventRepository(AppProperties props, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        String path = props.getEventsPath();
        try (InputStream in = resourceLoader.getResource(path).getInputStream()) {
            List<EventInfo> list = objectMapper.readValue(in, new TypeReference<List<EventInfo>>() {
            });
            Map<String, EventInfo> map = new LinkedHashMap<>();
            for (EventInfo e : list) {
                if (e.eventId() != null) {
                    map.put(e.eventId(), e);
                }
            }
            this.events = Collections.unmodifiableMap(map);
            log.info("[JsonEventRepository] 행사 데이터 로드: {}건 ({})", events.size(), path);
        } catch (IOException e) {
            throw new IllegalStateException("행사 데이터를 읽을 수 없습니다: " + path, e);
        }
    }

    @Override
    public Optional<EventInfo> findById(String eventId) {
        return Optional.ofNullable(eventId == null ? null : events.get(eventId));
    }

    @Override
    public List<NearbyEvent> findNearby(LocalDate date, double lat, double lng, double radiusKm,
                                        boolean freeOnly, Set<String> excludeIds) {
        return events.values().stream()
                .filter(e -> e.runsOn(date))
                .filter(EventInfo::hasCoords)
                .filter(e -> !excludeIds.contains(e.eventId()))
                .filter(e -> e.duplicateOf() == null)
                .filter(e -> !freeOnly || e.free())
                .map(e -> new NearbyEvent(e, Geo.haversineKm(lat, lng, e.lat(), e.lng())))
                .filter(n -> n.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyEvent::distanceKm)
                        .thenComparing(n -> n.event().eventId()))
                .toList();
    }

    @Override
    public List<EventProgram> findPrograms(String eventId) {
        return findById(eventId).map(EventInfo::programsOrEmpty).orElse(List.of());
    }

    @Override
    public List<EventInfo> findByIdsOverlapping(Set<String> ids, LocalDate from, LocalDate to) {
        return ids.stream()
                .map(events::get)
                .filter(e -> e != null && e.overlaps(from, to))
                .sorted(Comparator.comparing(EventInfo::startDate).thenComparing(EventInfo::eventId))
                .toList();
    }
}
