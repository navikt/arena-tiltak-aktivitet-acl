CREATE TABLE arena_data
(
    id                  SERIAL PRIMARY KEY NOT NULL,
    arena_table_name    VARCHAR            NOT NULL,
    arena_id            VARCHAR            NOT NULL,
    operation_type      VARCHAR            NOT NULL CHECK ( operation_type IN ('CREATED', 'MODIFIED', 'DELETED')),
    operation_pos       VARCHAR            NOT NULL,
    operation_timestamp TIMESTAMP          NOT NULL,
    ingest_status       VARCHAR            NOT NULL DEFAULT 'NEW' CHECK ( ingest_status IN
                                                                          ('NEW', 'HANDLED', 'RETRY', 'FAILED',
                                                                           'IGNORED', 'INVALID')),
    ingested_timestamp  TIMESTAMP,
    ingest_attempts     INT                NOT NULL DEFAULT 0,
    last_attempted      TIMESTAMP,
    before              JSONB,
    after               JSONB,
    note                VARCHAR
);

CREATE UNIQUE INDEX arena_data_table_operation_type_operation_pos_idx on arena_data (arena_table_name, operation_type, operation_pos);

create table aktivitet
(
    id            UUID PRIMARY KEY NOT NULL,
    person_ident  VARCHAR          NOT NULL,
    kategori_type VARCHAR          NOT NULL, --tiltakV1/gruppeV1/utdanningV1
    data          JSONB            NOT NULL
);

CREATE TABLE translation
(
    aktivitet_id       UUID PRIMARY KEY NOT NULL,-- REFERENCES aktivitet(id),
    arena_id           BIGINT UNIQUE    NOT NULL,
    aktivitet_kategori VARCHAR          NOT NULL
);

CREATE TABLE arena_tiltak
(
    id                  UUID PRIMARY KEY NOT NULL,
    kode                VARCHAR UNIQUE   NOT NULL,
    navn                VARCHAR          NOT NULL,
    administrasjonskode VARCHAR          NOT NULL
);

CREATE TABLE gjennomforing
(
    arena_id                   BIGINT PRIMARY KEY       NOT NULL,
    tiltak_kode                VARCHAR                  NOT NULL,
    arrangor_virksomhetsnummer VARCHAR,
    arrangor_navn              VARCHAR,
    navn                       VARCHAR                  NOT NULL,
    start_dato                 DATE,
    slutt_dato                 DATE,
    status                     VARCHAR                  NOT NULL,
    modified_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);