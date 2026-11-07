-- Kapazität in Story Points je PI, Team und Entwicklungs-Iteration (US-7). Nur erfasste Werte sind Zeilen; ohne Zeile
-- gilt 0. Die Fremdschlüssel räumen mit: Ein entferntes Team (US-6) oder PI nimmt seine Kapazität mit.
CREATE TABLE capacity (
    program_increment_id UUID    NOT NULL REFERENCES program_increment (id) ON DELETE CASCADE,
    team_id              UUID    NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    iteration_number     INTEGER NOT NULL,
    points               INTEGER NOT NULL CHECK (points >= 0),
    PRIMARY KEY (program_increment_id, team_id, iteration_number)
);

CREATE INDEX capacity_team_idx ON capacity (team_id);
