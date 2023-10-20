create unique index if not exists
    deltaker_aktivitet_mapping_aktid_unique on deltaker_aktivitet_mapping (aktivitet_id) ;

alter table deltaker_aktivitet_mapping
    add column gjeldende bool not null default false
;

update deltaker_aktivitet_mapping set gjeldende = true where aktivitet_id in (
    select aktivitet_id from (select distinct on (deltaker_id) deltaker_id,
                                                               deltaker_aktivitet_mapping.aktivitet_id,
                                                               COALESCE(aktivitet.oppfolgingsperiode_slutt_tidspunkt,
                                                                        TO_TIMESTAMP('9999', 'YYYY')) slutt
                              from deltaker_aktivitet_mapping
                                       join aktivitet on deltaker_aktivitet_mapping.aktivitet_id = aktivitet.id
                              order by deltaker_id, slutt desc) nyesteEntry
);
