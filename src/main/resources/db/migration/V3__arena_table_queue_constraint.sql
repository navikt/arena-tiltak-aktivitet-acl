ALTER TABLE arena_data DROP CONSTRAINT "arena_data_ingest_status_check";
AlTER TABLE arena_data ADD CONSTRAINT "arena_data_ingest_status_check"
    CHECK ( ingest_status in ('NEW', 'HANDLED', 'RETRY', 'FAILED', 'IGNORED', 'INVALID', 'QUEUED') );