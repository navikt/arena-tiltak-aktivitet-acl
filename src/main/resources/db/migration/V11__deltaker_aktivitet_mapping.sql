create table deltaker_aktivitet_mapping
(
    deltaker_id numeric not null ,
    aktivitet_id uuid not null ,
    aktivitet_kategori varchar not null ,
    oppfolgingsperiode_uuid uuid not null ,
    primary key (deltaker_id, aktivitet_id, aktivitet_kategori, oppfolgingsperiode_uuid)
);
create index deltaker_idx on deltaker_aktivitet_mapping(deltaker_id, aktivitet_kategori);
insert into deltaker_aktivitet_mapping select cast(substr(arena_id, 8) as numeric), id, kategori_type, oppfolgingsperiode_uuid from aktivitet;
