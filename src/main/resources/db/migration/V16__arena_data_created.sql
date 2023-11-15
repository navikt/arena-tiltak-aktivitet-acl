ALTER TABLE arena_data
    ADD COLUMN created_at timestamptz DEFAULT NULL;
ALTER TABLE arena_data
    ALTER COLUMN created_at SET DEFAULT NOW();
