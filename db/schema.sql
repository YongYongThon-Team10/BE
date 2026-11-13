-- prod 테이블 스키마. prod는 ddl-auto: validate라 이 파일로 직접 만든다 (local은 JPA가 자동 생성).
-- 엔티티(event, itinerary, course, user의 data/entity)를 바꾸면 이 파일도 같이 고친다. 실행 순서는 FK 순서다.

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

-- 사용자 (인증). favorites보다 먼저 있어야 한다. 이름이 예약어라 백틱으로 감싼다
CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT               NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255)         NOT NULL,
    name          VARCHAR(255)         NOT NULL,
    password      VARCHAR(255)         NOT NULL,
    role          ENUM('ADMIN','USER') NOT NULL,
    registered_at DATETIME(6),
    modified_at   DATETIME(6)          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email)
);

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

-- S5 하루 일정. 행사(events)와 FK를 걸지 않는다 (행사를 다시 적재해도 일정은 남는다)
CREATE TABLE IF NOT EXISTS itineraries (
    itinerary_id    VARCHAR(40)   NOT NULL,
    base_event_id   VARCHAR(255)  NOT NULL,
    title           VARCHAR(255),
    itinerary_date  DATE          NOT NULL,
    transport       VARCHAR(20),
    window_start    VARCHAR(5),
    window_end      VARCHAR(5),
    origin_type     VARCHAR(20),
    origin_lat      DOUBLE,
    origin_lng      DOUBLE,
    origin_address  VARCHAR(255),
    companion       VARCHAR(20),
    kids_age        INT,
    budget          VARCHAR(20),
    wants           VARCHAR(255),
    cover_image_url VARCHAR(1000),
    slot_seq        INT,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (itinerary_id)
);

CREATE TABLE IF NOT EXISTS itinerary_slots (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    itinerary_id       VARCHAR(40)   NOT NULL,
    slot_id            VARCHAR(20)   NOT NULL,
    slot_order         INT,
    slot_type          VARCHAR(20)   NOT NULL,
    title              VARCHAR(255),
    event_id           VARCHAR(255),
    program_id         VARCHAR(255),
    start_time         VARCHAR(5),
    end_time           VARCHAR(5),
    duration_min       INT,
    cost_amount        INT,
    cost_text          VARCHAR(500),
    description        VARCHAR(2000),
    time_basis         VARCHAR(20),
    is_user_added      BIT,
    place_name         VARCHAR(255),
    place_address      VARCHAR(255),
    place_lat          DOUBLE,
    place_lng          DOUBLE,
    move_mode          VARCHAR(20),
    move_distance_km   DOUBLE,
    move_estimated_min INT,
    move_map_link      VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT uk_itinerary_slots_itinerary_slot UNIQUE (itinerary_id, slot_id),
    CONSTRAINT fk_itinerary_slots_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (itinerary_id)
);

-- 공유 링크. 일정 하나에 한 줄
CREATE TABLE IF NOT EXISTS itinerary_shares (
    share_id     VARCHAR(40)  NOT NULL,
    itinerary_id VARCHAR(255) NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (share_id),
    CONSTRAINT uk_itinerary_shares_itinerary UNIQUE (itinerary_id)
);

-- 교체 후보 (alternatives, meal-candidates 조회 결과). 슬롯이 바뀌면 지운다
CREATE TABLE IF NOT EXISTS slot_candidates (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    itinerary_id VARCHAR(255)  NOT NULL,
    slot_id      VARCHAR(255)  NOT NULL,
    candidate_id VARCHAR(255)  NOT NULL,
    event_id     VARCHAR(255),
    payload      VARCHAR(4000),
    PRIMARY KEY (id)
);

-- S6 내 코스. 일정 하나에 한 줄
CREATE TABLE IF NOT EXISTS courses (
    course_id    BIGINT       NOT NULL AUTO_INCREMENT,
    itinerary_id VARCHAR(255) NOT NULL,
    title        VARCHAR(255),
    saved_at     DATETIME(6),
    PRIMARY KEY (course_id),
    CONSTRAINT uk_courses_itinerary UNIQUE (itinerary_id)
);
