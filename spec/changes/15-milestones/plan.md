# Plan US-15 — Milestones auf dem Board

Delta: `delta.md` (nur «Neu»; ADR 0005: Milestone ist ein eigenes Aggregate am PI)

## Aufgaben

- [x] Domain: `Milestone` (Name Pflicht/getrimmt/≤100, Datum im PI-Zeitraum), `InvalidMilestoneException`, `MilestoneNotFoundException`, `ProgramIncrement.iterationOf(date)`, `ProgramBoard` mit Milestones
- [x] Ports: `MilestoneUseCase`, `MilestoneRepository`; `MilestoneService`; `BoardService` liefert Milestones
- [x] Persistenz: `MilestoneEntity`, `MilestoneRepositoryAdapter`, Migration V8 (nur neue Tabelle, keine Datenänderung)
- [x] REST: `MilestoneResource` (GET/POST/PUT/DELETE unter `/program-increments/{id}/milestones`), Mapper, `BoardDto` mit `milestones` (inkl. `iterationNumber`)
- [x] Frontend: Modell, Port, HTTP-Adapter, `MilestonesService`, Abschnitt auf der PI-Detailseite (erfassen, ändern, Löschen nach Bestätigung), Marker auf dem Board
- [x] Feature-Datei `program-increments/15-milestones.feature`, Story `15-milestones.md`

## Test-Strategie

- Unit (JUnit): `MilestoneTest`, `ProgramIncrementTest` (iterationOf), `ProgramBoardTest`, `MilestoneServiceTest`, `BoardServiceTest`
- CucumberIT: alle sechs Szenarien des Deltas
- Vitest: `milestones.service.spec.ts`, `milestones.component.spec.ts`, Detailseite, Board-Marker
