# Plan US-9 — Feature im PI-Backlog erfassen (Issue #9)

Delta: `delta.md` (nur «Neu»). Betroffene Schichten: Domain, Application, Adapter (REST, Persistenz), Frontend.

## Aufgaben

- [x] Domain: `Feature` (Record, Regeln im Compact Constructor), `InvalidFeatureException`, Ports `FeatureUseCase`,
  `FeatureRepository`
- [x] Application: `FeatureService` (PI muss existieren → `ProgramIncrementNotFoundException`; Backlog sortiert)
- [x] Persistenz: Migration `V5__create_features.sql` (nur neue Tabelle, ändert keine Daten) + `changeLog.xml`,
  `FeatureEntity`, `FeatureRepositoryAdapter`
- [x] REST: `FeatureResource` unter `/api/v1/program-increments/{id}/features` (GET, POST), `FeatureRequest`,
  `FeatureDto`, `InvalidFeatureExceptionMapper`
- [x] Frontend: Model, Port, HTTP-Adapter, `FeaturesService`, `FeatureBacklogComponent` in der PI-Detailseite
- [x] Story `spec/user-stories/9-feature-erfassen.md`, Feature-Datei `features/features/9-feature-erfassen.feature`

## Test-Strategie

- Unit (JUnit): `FeatureTest` (Regeln, Trimmen, Grenzen), `FeatureServiceTest` (Ports gemockt, unbekanntes PI,
  Sortierung)
- Unit (Vitest): `FeaturesService`, `FeatureBacklogComponent` (Feldfehler, Leerzustand, Absenden)
- Integration (`CucumberIT`): alle Szenarien des Deltas gegen REST und PostgreSQL

## Entscheide beim Umsetzen

- Leere Beschreibung/Owner = leerer Text, kein `null` (Domain), `OptionalInt` für Rang und Schätzung
- Backlog-Reihenfolge: WSJF-Rang aufsteigend, ohne Rang am Ende, dann Titel (siehe `notes.md`)
