CREATE UNIQUE INDEX IF NOT EXISTS aktivitet_arenaid_oppfolg_ux
    ON aktivitet (arena_id, oppfolgingsperiode_uuid);