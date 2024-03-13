alter table arena_data add operation_pos_numeric bigint default null;
CREATE INDEX ON arena_data(operation_pos_numeric);
