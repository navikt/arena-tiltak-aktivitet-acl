ALTER TABLE aktivitet
    DROP COLUMN historisk;
ALTER TABLE aktivitet
    ADD COLUMN oppfolgingsperiode_slutt_tidspunkt TIMESTAMP with time zone;