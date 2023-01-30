alter table aktivitet
    add column ARENA_ID varchar(20),
    add column TILTAK_KODE varchar(14),
    add column OPPFOLGINGSPERIODE_UUID uuid,
    add column HISTORISK bool;

