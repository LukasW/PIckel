# Plan #12 — Program Board eines PI anzeigen

Ohne `/flow-plan` im Nachtlauf geschrieben (`/flow-implement` Schritt 4).

## Aufgaben

- [x] Domain: Record `ProgramBoard` (PI, Teams, Iterationen, nicht eingeplantes Backlog)
- [x] Port `BoardUseCase#get(piId)`; `BoardService` setzt es aus `ProgramIncrementUseCase#get`, `TeamUseCase#listAll`
  (alphabetisch) und `FeatureUseCase#listBacklog` (Backlog-Reihenfolge, #10) zusammen und filtert
  `Feature#scheduled()`; keine neue Persistenz
- [x] REST: `GET /api/v1/program-increments/{piId}/board` → `{ teams, iterations, backlog }`; 404 über den
  bestehenden `ProgramIncrementNotFoundExceptionMapper`
- [x] Frontend: Modell `board.model.ts`, Port `BoardRepository`, `BoardHttpAdapter`, `BoardService` (Signals),
  Komponente `features/program-board` unter Route `program-increments/:id/board`, Link von der PI-Detailseite
- [x] Story `spec/user-stories/12-program-board-anzeigen.md`, Feature-Datei `12-program-board-anzeigen.feature`

Keine Liquibase-Migration: Das Board ist eine Sicht auf Bestehendes. Einplanen (#13) ergänzt später die Zellen.

## Test-Strategie

| Kriterium | Ebene |
|---|---|
| Zeilen alphabetisch, Spalten zeitlich, IP zuletzt (AK 1) | `BoardServiceTest`, `CucumberIT` (Szenario 1), Vitest `program-board.component.spec.ts` (IP abgesetzt) |
| Spaltenköpfe Name und Zeitraum (AK 2) | Vitest Komponente |
| Backlog-Spalte: Karten, nur nicht eingeplante, Reihenfolge (AK 3) | `BoardServiceTest`, `CucumberIT`, Vitest Komponente |
| Ohne Teams Hinweis mit Link (AK 4) | Vitest Komponente (`@Unit`) |
| Von der Detailseite erreichbar (AK 5) | Vitest `program-increment-detail.component.spec.ts` |
| Unbekanntes PI → 404 | `BoardServiceTest`, `CucumberIT` |
