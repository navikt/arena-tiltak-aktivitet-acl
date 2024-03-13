ALTER TABLE arena_data ADD COLUMN operation_pos_numeric numeric (20,0);
CREATE INDEX operation_pos_numeric_index on arena_data(operation_pos_numeric);

CREATE OR REPLACE FUNCTION convert_to_numeric()
    RETURNS trigger AS $$
BEGIN
    IF (tg_op = 'INSERT') THEN
        NEW.operation_pos_numeric = operation_pos::bigint;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER convert_to_numeric_trigger
    BEFORE INSERT ON arena_data
    FOR EACH ROW EXECUTE PROCEDURE convert_to_numeric();
