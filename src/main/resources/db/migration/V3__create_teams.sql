CREATE TABLE team (
    id    UUID        PRIMARY KEY,
    name  VARCHAR(50) NOT NULL
);

-- Der Name ist ohne Rücksicht auf Gross- und Kleinschreibung eindeutig (US-6); die Domain prüft zuerst und meldet
-- verständlich, der Index fängt gleichzeitige Anfragen ab.
CREATE UNIQUE INDEX team_name_lower_uk ON team (lower(name));
