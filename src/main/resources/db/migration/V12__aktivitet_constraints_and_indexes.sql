CREATE INDEX IF NOT EXISTS
    aktivitet_arena_id ON aktivitet (arena_id);

CREATE UNIQUE INDEX IF NOT EXISTS only_one_open_periode_per_arena_id_index
    ON aktivitet (arena_id)
    WHERE aktivitet.oppfolgingsperiode_slutt_tidspunkt IS NULL;

ALTER TABLE aktivitet
    ALTER COLUMN oppfolgingsperiode_uuid SET NOT NULL;

DROP TABLE translation;
DROP TABLE deltaker_aktivitet_mapping;