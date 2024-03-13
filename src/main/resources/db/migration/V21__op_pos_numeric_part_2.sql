-- Stopp consumer! (slå på disable)
update arena_data set operation_pos_numeric = operation_pos::bigint where operation_pos_numeric is null;
alter table arena_data alter column operation_pos_numeric set not null;
-- Nå kommer consumer til å krasje
CREATE UNIQUE INDEX arena_data_table_operation_pos_idx on arena_data (arena_table_name, operation_pos_numeric);

alter table arena_data drop column operation_pos;
alter table arena_data rename column operation_pos_numeric to operation_pos;
-- Kan starte consumer igjen nå (slå av disable)