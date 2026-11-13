package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.event.data.dto.EventInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 팀 행사 DB(PostgreSQL yongin_events의 events 테이블)를 읽는 구현.
 * app.event-data.source=postgres일 때 쓴다. 시작할 때 한 번 전체를 읽어 메모리에 둔다.
 * GCP IAP 터널이 켜져 있어야 하고, 접속에 실패하면 서버 시작을 멈춘다.
 *
 * 팀 주 DB(MariaDB)와 섞이지 않게 DataSource 빈을 만들지 않고 시작할 때만 직접 접속한다.
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "app.event-data.source", havingValue = "postgres")
public class PostgresEventRepository extends InMemoryEventRepository {

    static final String SELECT_EVENTS = """
            SELECT event_id, title, source_category, start_date, end_date, event_hours,
                   venue, address, verified_address, fee, latitude, longitude,
                   duplicate_status, duplicate_representative_id
            FROM events
            """;

    public PostgresEventRepository(AppProperties props) {
        super(load(props));
        log.info("[PostgresEventRepository] 행사 데이터 로드: {}건 ({})", size(), props.getEventDbUrl());
    }

    private static List<EventInfo> load(AppProperties props) {
        try (Connection conn = DriverManager.getConnection(
                props.getEventDbUrl(), props.getEventDbUsername(), props.eventDbPassword());
             PreparedStatement ps = conn.prepareStatement(SELECT_EVENTS);
             ResultSet rs = ps.executeQuery()) {
            List<EventInfo> events = new ArrayList<>();
            while (rs.next()) {
                events.add(toEvent(rs));
            }
            return events;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "행사 DB에 접속할 수 없습니다. IAP 터널과 EVENT_DB_PASSWORD를 확인하세요: " + props.getEventDbUrl(), e);
        }
    }

    private static EventInfo toEvent(ResultSet rs) throws SQLException {
        String fee = EventFieldRules.fee(rs.getString("fee"));
        return new EventInfo(
                EventFieldRules.text(rs.getString("event_id")),
                EventFieldRules.text(rs.getString("title")),
                EventFieldRules.text(rs.getString("source_category")),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                EventFieldRules.text(rs.getString("event_hours")),
                EventFieldRules.text(rs.getString("venue")),
                EventFieldRules.address(rs.getString("address"), rs.getString("verified_address")),
                toDouble(rs.getBigDecimal("latitude")),
                toDouble(rs.getBigDecimal("longitude")),
                fee,
                EventFieldRules.isFree(fee),
                null,
                null,
                null,
                List.of(),
                EventFieldRules.duplicateOf(rs.getString("duplicate_status"), rs.getString("duplicate_representative_id"))
        );
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
