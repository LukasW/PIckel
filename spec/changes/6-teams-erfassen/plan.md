# US-6 — Teams des ART erfassen (Issue #6)

Nachtlauf-Plan, knapp. Massgebend ist `delta.md` (nur «Neu»).

## Übersicht

Neues Aggregat `Team` (ID, Name) am impliziten ART (ADR 0005). Domain validiert den Namen (Pflicht, getrimmt,
≤ 50 Zeichen). Der Service prüft die Eindeutigkeit ohne Gross-/Kleinschreibung (Repository `findByNameIgnoreCase`;
beim Umbenennen darf der Treffer das Team selbst sein) und sortiert alphabetisch (`Collator` de-CH). REST
`/api/v1/teams` (GET, POST, PUT `/{id}`, DELETE `/{id}`). Migration V3 legt nur die Tabelle `team` samt Unique-Index
auf `lower(name)` an (Schema, keine Datenänderung). Frontend: Seite `/teams` mit Formular, Liste, Umbenennen inline,
Entfernen mit Inline-Bestätigung, Leerzustand.

## Aufgaben

- [x] 1. Gerüst (Domain, Ports, Service, Resource, Adapter, Migration V3, Frontend-Typen)
- [x] 2. Tests über `test-author` (JUnit Domain/Service, Cucumber, Vitest Service/Komponente)
- [x] 3. Backend implementieren
- [x] 4. Frontend implementieren
- [x] 5. Story/Delta einpflegen, `gate.sh`

## Architektur-Entscheide

- **Fehler:** `DuplicateTeamNameException extends InvalidTeamException` → ein Mapper, 400 mit Meldung (wie PI-Regeln);
  `TeamNotFoundException` → 404. Ungültige UUID = unbekanntes Team (wie bei PIs)
- **Sortierung im Service**, nicht in der DB-Collation: ein Ort, unit-testbar
- **Entfernen:** `204`; kein Bezug zu Features (kommen mit #9/#13)
- **Frontend-Bestätigung:** Inline («Wirklich entfernen?» mit Ja/Abbrechen), kein `window.confirm`, damit testbar
- **ADR:** keine neue (ADR 0005 deckt Teams am ART)

## Test-Strategie

| Szenario | Ebene |
|---|---|
| Team wird erfasst / alphabetisch / doppelt / umbenannt / entfernt | CucumberIT |
| Leerer Teamname, zu langer Name | `@Unit` JUnit `TeamTest` (plus Service-Tests für Duplikat, Umbenennen, Sortierung, Not-Found) |
| Entfernen verlangt Bestätigung, Leerzustand | `@Unit` Vitest `teams.component.spec.ts` |

Delta: `delta.md` — Neu (US-6): 6 AK, 9 Szenarien (zwei ergänzt vom Lauf: Name zu lang, Bestätigung). Geändert/Entfernt: keine.
