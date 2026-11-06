CREATE TABLE program_increment (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT program_increment_dates_ck CHECK (end_date > start_date)
);
