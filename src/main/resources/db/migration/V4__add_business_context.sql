ALTER TABLE program_increment
    ADD COLUMN business_context TEXT NOT NULL DEFAULT '',
    ADD COLUMN art_objectives TEXT NOT NULL DEFAULT '';

CREATE TABLE pi_link (
    id                   UUID PRIMARY KEY,
    program_increment_id UUID         NOT NULL REFERENCES program_increment (id) ON DELETE CASCADE,
    position             INTEGER      NOT NULL,
    title                VARCHAR(100) NOT NULL,
    url                  TEXT         NOT NULL
);

CREATE INDEX pi_link_program_increment_idx ON pi_link (program_increment_id);
