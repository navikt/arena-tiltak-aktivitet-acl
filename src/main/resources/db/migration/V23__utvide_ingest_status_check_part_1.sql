/*
 Husk
 update arena_data set ingest_status = 'HANDLED_AND_IGNORED'
    where ingest_status = 'HANDLED' and note in ('ignorert slettemelding','foreløpig ignorert')
 Eller helst sette til 'QUEUED' og la acl oppdatere, dersom mulig
 */
ALTER TABLE arena_data DROP CONSTRAINT "arena_data_ingest_status_check";