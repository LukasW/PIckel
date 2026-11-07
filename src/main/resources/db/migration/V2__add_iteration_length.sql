ALTER TABLE program_increment
    ADD COLUMN iteration_length_weeks INTEGER NOT NULL DEFAULT 2,
    ADD CONSTRAINT program_increment_iteration_length_ck CHECK (iteration_length_weeks BETWEEN 1 AND 4);

ALTER TABLE program_increment ALTER COLUMN iteration_length_weeks DROP DEFAULT;
