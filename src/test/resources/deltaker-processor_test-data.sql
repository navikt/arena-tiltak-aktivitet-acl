-- non-ignored gjennomføring
insert into gjennomforing(arena_id, tiltak_kode, arrangor_virksomhetsnummer, arrangor_navn, navn, start_dato, slutt_dato, status)
values (1, 'AVKLARKV', '1234', 'arrangornavn', 'navn', date '2022-08-01', date '2022-09-30', 'GJENNOMFOR');
-- ignored gjennføring
insert into gjennomforing(arena_id, tiltak_kode, arrangor_virksomhetsnummer, arrangor_navn, navn, start_dato, slutt_dato, status)
values (2, 'ARBTREN', '1234', 'arrangornavn', 'navn', date '2022-08-01', date '2022-09-30', 'GJENNOMFOR');

insert into arena_tiltak(id, kode, navn, administrasjonskode)
values ('92dc9515-1246-48c2-a7ef-3dbd53713bb6', 'ARBTREN', 'tiltaknavn', 'IND');

insert into arena_tiltak(id, kode, navn, administrasjonskode)
values ('92dc9515-1246-48c2-a7ef-3dbd53713bb7', 'AVKLARKV', 'tiltaknavn', 'AMO');