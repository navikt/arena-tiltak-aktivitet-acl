alter table aktivitet
    add column ARENA_ID varchar,
    add column TILTAK_KODE varchar,
    add column OPPFOLGINGSPERIODE_UUID uuid,
    add column HISTORISK bool;

