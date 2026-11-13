-- 행사 테이블 스키마. prod는 ddl-auto: validate라 이 파일로 직접 만든다 (local은 JPA가 자동 생성).
-- 엔티티(event/data/entity)를 바꾸면 이 파일도 같이 고친다.

CREATE TABLE IF NOT EXISTS events (
    event_id        VARCHAR(20)   NOT NULL,
    name            VARCHAR(300)  NOT NULL,
    category        VARCHAR(10)   NOT NULL,
    sub_category    VARCHAR(50),
    start_date      DATE,
    end_date        DATE,
    date_status     VARCHAR(10)   NOT NULL,
    time            VARCHAR(100),
    place_name      VARCHAR(200),
    address         VARCHAR(200),
    district        VARCHAR(10),
    lat             DOUBLE,
    lng             DOUBLE,
    coord_precision VARCHAR(10),
    fee             VARCHAR(200),
    is_free         BIT,
    target          VARCHAR(300),
    apply_type      VARCHAR(10),
    apply_url       VARCHAR(1000),
    receipt_start   DATETIME(6),
    receipt_end     DATETIME(6),
    receipt_status  VARCHAR(50),
    source_url      VARCHAR(1000) NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE TABLE IF NOT EXISTS event_sources (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    event_id   VARCHAR(20)   NOT NULL,
    name       VARCHAR(100)  NOT NULL,
    type       VARCHAR(10)   NOT NULL,
    url        VARCHAR(1000) NOT NULL,
    sort_order INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_event_sources_event FOREIGN KEY (event_id) REFERENCES events (event_id)
);

-- user 테이블은 이 파일에 없다 (인증 템플릿 테이블, 알려진 이슈). favorites보다 먼저 있어야 한다
CREATE TABLE IF NOT EXISTS favorites (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    event_id   VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_favorites_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_favorites_event FOREIGN KEY (event_id) REFERENCES events (event_id)
);
