# US-7 — Kapazität pro Team und Iteration erfassen (Issue #7)

Nachtlauf-Plan, knapp. Massgebend ist `delta.md` (nur «Neu»).

## Übersicht

Neues Aggregat `Capacity` (PI, Team, Iterationsnummer, Story Points) am PI (ADR 0005, 0006). Die Domain validiert
Punkte (ganzzahlig ab 0) und Iteration (existiert, nicht IP). Der Service lädt PI und Team (404 bei Unbekanntem),
setzt den Wert und liefert für `list` die **volle Matrix** Teams × Entwicklungs-Iterationen mit Standard 0 — die
Regel «ohne Eintrag 0» steht damit nur im Backend. REST: `GET /api/v1/program-increments/{id}/capacities`,
`PUT /api/v1/program-increments/{id}/capacities/{teamId}/{iterationNumber}` mit `{ "points": n }`.
Migration V6 legt nur die Tabelle `capacity` an (Schema, keine Datenänderung); Fremdschlüssel mit `ON DELETE CASCADE`,
damit ein entferntes Team (US-6) seine Kapazität mitnimmt. Frontend: Tabelle Teams × Iterationen auf der PI-Detailseite,
Zelle speichert beim Verlassen des Feldes; Leerzustand ohne Teams mit Link auf `/teams`.

## Aufgaben

- [x] 1. Gerüst (Domain, Ports, Service, Resource, Adapter, Migration V6, Frontend-Typen)
- [x] 2. Tests über `test-author` (JUnit Domain/Service, Cucumber, Vitest Service/Komponente)
- [x] 3. Backend implementieren
- [x] 4. Frontend implementieren
- [x] 5. Story/Delta einpflegen, `gate.sh`

## Architektur-Entscheide

- **Fehler:** `InvalidCapacityException` → 400 mit Meldung; unbekanntes PI/Team → bestehende 404-Mapper
- **Ganzzahl:** Jackson kürzt `1.5` standardmässig auf `1`; `WholeNumberDeserializer` nur am Feld `points` lehnt
  ab → `MismatchedInputException` → 400 «Die Anfrage enthält einen ungültigen Wert.» (andere Felder unverändert).
  Das Formular meldet Kommazahl, negative Zahl und Text vorher mit der Meldung der Domain
- **Matrix im Backend:** `list` liefert jede Zelle, auch ohne Eintrag (Punkte 0)
- **Unbekannte Iterationsnummer:** `InvalidCapacityException` «Die Iteration gibt es nicht.» (Annahme, nicht im Delta)
- **ADR:** keine neue (ADR 0005 und 0006 decken Ort und Einheit)

## Test-Strategie

| Szenario | Ebene |
|---|---|
| Kapazität wird erfasst / Ohne Eintrag 0 / pro PI getrennt | CucumberIT |
| Negative Kapazität, IP-Iteration | CucumberIT (REST-Vertrag) und JUnit `CapacityTest` für die Regel |
| Ohne Teams Hinweis mit Link | `@Unit` Vitest, Komponente |
| Erfassen in der Tabelle | Vitest, Service und Komponente |

Delta: `delta.md` — Neu (US-7): 5 AK, 9 Szenarien (4 im Nachtlauf ergänzt: Nachkommastellen, unbekanntes Team,
unbekanntes PI, Hinweis ohne Teams). Geändert/Entfernt: keine.
