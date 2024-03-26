AlTER TABLE arena_data ADD CONSTRAINT "arena_data_ingest_status_check"
    CHECK ( ingest_status in ('NEW', 'HANDLED', 'HANDLED_AND_IGNORED', 'RETRY', 'FAILED', 'IGNORED', 'INVALID', 'QUEUED') );