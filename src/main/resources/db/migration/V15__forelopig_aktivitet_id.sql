
CREATE TABLE forelopig_aktivitet_id
(
    id                  UUID PRIMARY KEY NOT NULL,
    deltakelse_id       BIGINT UNIQUE    NOT NULL,
    kategori  VARCHAR          NOT NULL
);