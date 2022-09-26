INSERT INTO translation(aktivitet_id, aktivitet_kategori, arena_id)
VALUES ('702f2474-29ca-42f1-afc2-9851ec23fa80', 'TILTAKSAKTIVITET', 1);

INSERT INTO translation(aktivitet_id, aktivitet_kategori, arena_id)
VALUES ('d3ac3321-2d77-43f7-8ca1-1e38a78846b5', 'TILTAKSAKTIVITET', 2);

insert into gjennomforing(arena_id, tiltak_kode, arrangor_virksomhetsnummer, arrangor_navn, navn, start_dato, slutt_dato, status)
values (1, 'ARBTREN', '1234', 'arrangornavn', 'navn', date '2022-08-01', date '2022-09-30', 'GJENNOMFOR');

insert into arena_tiltak(id, kode, navn, administrasjonskode)
values ('92dc9515-1246-48c2-a7ef-3dbd53713bb6', 'ARBTREN', 'tiltaknavn', 'IND');
