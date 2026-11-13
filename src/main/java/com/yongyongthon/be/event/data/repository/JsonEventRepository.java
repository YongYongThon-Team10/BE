package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.event.data.dto.EventInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 행사 JSON 파일(tools/convert_events.py로 엑셀에서 변환)을 읽는 구현. 기본값.
 * app.event-data.source=json (또는 설정 없음)일 때 쓴다.
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "app.event-data.source", havingValue = "json", matchIfMissing = true)
public class JsonEventRepository extends InMemoryEventRepository {

    public JsonEventRepository(AppProperties props, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        super(load(props.getEventsPath(), resourceLoader, objectMapper));
        log.info("[JsonEventRepository] 행사 데이터 로드: {}건 ({})", size(), props.getEventsPath());
    }

    private static List<EventInfo> load(String path, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        try (InputStream in = resourceLoader.getResource(path).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<EventInfo>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("행사 데이터를 읽을 수 없습니다: " + path, e);
        }
    }
}
