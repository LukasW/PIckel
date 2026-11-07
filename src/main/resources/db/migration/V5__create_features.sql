CREATE TABLE feature (
    id                    UUID           PRIMARY KEY,
    program_increment_id  UUID           NOT NULL REFERENCES program_increment (id),
    title                 VARCHAR(200)   NOT NULL,
    description           VARCHAR(5000)  NOT NULL,
    owner                 VARCHAR(100)   NOT NULL,
    wsjf_rank             INTEGER,
    estimate              INTEGER
);

CREATE INDEX feature_program_increment_idx ON feature (program_increment_id);
