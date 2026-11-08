CREATE TABLE milestone (
    id                   UUID PRIMARY KEY,
    program_increment_id UUID         NOT NULL REFERENCES program_increment (id) ON DELETE CASCADE,
    name                 VARCHAR(100) NOT NULL,
    milestone_date       DATE         NOT NULL
);

CREATE INDEX milestone_program_increment_idx ON milestone (program_increment_id);
