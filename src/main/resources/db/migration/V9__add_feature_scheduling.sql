-- Einplanung eines Features auf dem Program Board (US-13): Team und Iterationsnummer im PI, beides oder nichts.
-- Kein ON DELETE am Team: TeamService gibt die Features vor dem Entfernen zurück ins Backlog; ein vergessenes
-- Zurückgeben scheitert laut, statt eine Iterationsnummer ohne Team stehen zu lassen. Ändert keine Daten.
ALTER TABLE feature
    ADD COLUMN team_id          UUID REFERENCES team (id),
    ADD COLUMN iteration_number INTEGER,
    ADD CONSTRAINT feature_scheduling_complete CHECK ((team_id IS NULL) = (iteration_number IS NULL));

CREATE INDEX feature_team_idx ON feature (team_id);
