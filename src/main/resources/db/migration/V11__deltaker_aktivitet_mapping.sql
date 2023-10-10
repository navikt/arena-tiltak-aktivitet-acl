create table deltaker_aktivitet_mapping
(
    deltaker_id numeric,
    aktivitet_id uuid,
    oppfolgingsperiode_uuid uuid,
    primary key (deltaker_id, aktivitet_id, oppfolgingsperiode_uuid)
);
create index deltaker_id_idx on deltaker_aktivitet_mapping(deltaker_id);
insert into deltaker_aktivitet_mapping select cast(substr(arena_id, 8) as numeric), id, oppfolgingsperiode_uuid from aktivitet;
