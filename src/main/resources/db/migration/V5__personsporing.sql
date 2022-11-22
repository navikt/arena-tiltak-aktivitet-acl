CREATE TABLE personsporing
(
    person_id bigint NOT NULL ,
    fodselsnummer varchar NOT NULL,
    tiltakgjennomforing_id bigint NOT NULL,
    PRIMARY KEY (person_id, tiltakgjennomforing_id)
);
