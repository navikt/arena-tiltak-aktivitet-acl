CREATE UNIQUE INDEX IF NOT EXISTS only_one_open_periode_per_arena_id_index
    ON aktivitet (arena_id)
    WHERE aktivitet.oppfolgingsperiode_slutt_tidspunkt IS NULL;