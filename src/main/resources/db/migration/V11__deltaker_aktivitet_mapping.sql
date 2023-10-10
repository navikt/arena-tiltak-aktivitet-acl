create table deltaker_aktivitet_mapping as
select id as aktivitet_id, oppfolgingsperiode_uuid, arena_id as deltaker_id
from aktivitet;
create index deltaker_id_idx on deltaker_aktivitet_mapping(deltaker_id);