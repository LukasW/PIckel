# US-14 — Kapazität pro Zelle mit Überlast anzeigen (Issue #14)

## Übersicht

Das Board liefert pro Zelle Team × Entwicklungs-Iteration die verfügbare Kapazität mit (aus `CapacityUseCase.list`,
ohne Eintrag 0). Das Frontend leitet daraus und aus den Karten im Board-Signal pro Zelle «geplant», «verfügbar» und
«Überlast» als `computed` ab (`entscheide.md` → «Ort der Berechnung»). Weil der Wert aus demselben Signal kommt, das
das optimistische Einplanen aus #13 ändert, stimmen die Zahlen sofort nach Einplanen, Verschieben und Zurücknehmen
und kehren bei einem Fehler mit der Karte zurück. Karten ohne Schätzung tragen «ohne Schätzung». Keine Migration.

## Zu erstellende Dateien
- [x] `spec/user-stories/14-zellen-kapazitaet.md` — Story (aus dem Delta eingepflegt)
- [x] `src/test/resources/features/program-increments/14-zellen-kapazitaet.feature` — Szenarien wie im Delta, ein
  CucumberIT-Szenario und fünf `@Unit` mit `# Vitest:`-Verweisen

## Zu ändernde Dateien
- [x] `src/main/java/.../domain/model/ProgramBoard.java` — Komponente `List<Capacity> capacities`
- [x] `src/main/java/.../application/service/BoardService.java` — `CapacityUseCase` injizieren, `list(piId)` ins Board
- [x] `src/main/java/.../adapter/in/rest/BoardDto.java` — Feld `capacities` (`List<CapacityDto>`, bestehender DTO)
- [x] `src/test/java/.../application/service/BoardServiceTest.java` — `@Mock CapacityUseCase`, neuer Test
- [x] `src/test/java/.../domain/model/ProgramBoardTest.java` — Konstruktoraufrufe um `capacities` ergänzen
- [x] `src/test/java/.../cucumber/steps/CapacitySteps.java` — Gegeben-Step «das Team … mit der Kapazität … in …
  von …» (legt Team an und erfasst die Kapazität über die REST-API)
- [x] `src/test/java/.../cucumber/steps/BoardSteps.java` — Dann-Steps «liefert das Board für … in … die Kapazität
  …» und «… für die IP-Iteration keine Kapazität»
- [x] `src/main/webapp/src/app/core/models/board.model.ts` — `capacities: readonly Capacity[]`, Typ `CellLoad`
  (`planned`, `available`, `overloaded`)
- [x] `src/main/webapp/src/app/core/services/board.service.ts` — `computed` mit der Last pro Zelle, Methode
  `loadOf(teamId, iterationNumber): CellLoad | undefined`
- [x] `src/main/webapp/src/app/features/program-board/program-board.component.html` — Anzeige «geplant / verfügbar
  SP» oben in jeder Entwicklungs-Zelle, Überlast mit `border-danger`, `bg-danger/10` und Text «Überlast»
- [x] `src/main/webapp/src/app/features/program-board/board-card.component.html` — «ohne Schätzung», wenn
  `estimate` fehlt (0 ist eine Schätzung, kein Hinweis)
- [x] Vitest-Fixtures mit einem `Board` (`board.service.spec.ts`, `program-board.component.spec.ts`, `app.spec.ts`,
  `milestones.*.spec.ts`, soweit sie ein `Board` bauen) — Pflichtfeld `capacities: []` ergänzen, sonst nichts

## Delta
`spec/changes/14-zellen-kapazitaet/delta.md` — Neu: 5 AK, 6 Szenarien (1 CucumberIT, 5 `@Unit`). Geändert: keine.
Entfernt: keine.

## Architektur-Entscheide
- **Backend:** `ProgramBoard` (Domain) trägt die Kapazitäten, `BoardService` (Application) holt sie über den
  Driving Port `CapacityUseCase`, wie er schon `TeamUseCase`, `FeatureUseCase` und `MilestoneUseCase` nutzt. Kein
  neuer Port, keine neue Tabelle, kein neuer Endpunkt; das Feld im Board-JSON ist eine Erweiterung, kein Breaking
  Change
- **«Ohne Eintrag 0»** bleibt an einem Ort (`CapacityService.list`); `BoardService` rechnet nichts nach
- **Frontend:** Die Regel (Summe, ohne Schätzung 0, Überlast bei `>`) steht in `BoardService` (core/services), nicht
  in der Komponente (`.claude/rules/angular.md` → «keine Fachlogik in Komponenten»). Ein `computed` über dem
  Board-Signal baut eine Map `teamId|iterationNumber → CellLoad`; die Komponente liest nur
- **IP-Iteration:** `CapacityUseCase.list` liefert dort keine Zelle, `loadOf` gibt `undefined`; das Template zeigt
  in der IP-Spalte ohnehin keine Ablage und keine Anzeige
- **Abwägung Map vs. Berechnung pro Aufruf:** Map im `computed`, damit das Template bei jeder Change Detection nicht
  pro Zelle über alle Karten filtert; rechnet nur neu, wenn sich das Board ändert
- Keine neue ADR, kein Abschnitt in `docs/arc42.md` nachzuziehen

## Aufgaben
- [x] 1. `ProgramBoard` um `capacities` erweitern, `BoardService` holt sie über `CapacityUseCase` — erledigt, wenn
  `BoardServiceTest` (neu: «Board trägt die Kapazitäten des PI») und `ProgramBoardTest` grün sind
- [x] 2. `BoardDto.capacities` und Cucumber-Steps; Feature-Datei mit dem CucumberIT-Szenario «Board liefert die
  Kapazität pro Zelle» — erledigt, wenn das Szenario in `CucumberIT` grün ist
- [x] 3. Frontend-Modell `Board.capacities`, `CellLoad`; Fixtures um `capacities: []` ergänzen — erledigt, wenn
  `npm test` grün ist
- [x] 4. `BoardService.loadOf` als `computed` — erledigt, wenn die Tests zu Summe, ohne Schätzung, Überlast,
  Gleichstand, Verschieben und Rücknahme in `board.service.spec.ts` grün sind
- [x] 5. Zellenanzeige und Überlast im Board-Template — erledigt, wenn die Tests in
  `program-board.component.spec.ts` grün sind
- [x] 6. Hinweis «ohne Schätzung» auf der Karte — erledigt, wenn der Test in `board-card.component.spec.ts` grün ist
- [x] 7. `@Unit`-Szenarien mit `# Vitest:`-Verweisen in der Feature-Datei; Story anlegen, Delta einpflegen —
  erledigt, wenn `check-story-drift.sh` nichts meldet
- [x] 8. `scripts/gate.sh` grün
- [ ] 8a. Screenshot einer Zelle mit und ohne Überlast für den PR — im Nachtlauf ohne Browser offen, von Hand

## Test-Strategie
- [x] Unit (JUnit): `BoardService.get` übergibt die Kapazitätsliste aus `CapacityUseCase.list` unverändert ans Board;
  ein unbekanntes PI bleibt 404 (bestehender Test)
- [x] Integration (CucumberIT, «Board liefert die Kapazität pro Zelle»): das Board-JSON enthält für "Adler" ×
  "Iteration 1" 20, für "Iteration 2" ohne Eintrag 0, für die IP-Iteration keinen Eintrag
- [x] Unit (Vitest, `board.service.spec.ts`):
  - geplant = Summe der Schätzungen der Karten der Zelle (13 + 8 = 21), Karten anderer Zellen zählen nicht
  - eine Karte ohne Schätzung zählt 0 («0 / 0 SP» bei Team ohne Kapazität)
  - Überlast bei 21 > 20, keine Überlast bei 13 = 13 und bei 13 < 20
  - nach `schedule` in eine andere Zelle sinkt die alte Zelle und steigt die neue, ohne neuen `get`-Aufruf am
    Repository; nach `unschedule` sinkt die Zelle
  - scheitert das Speichern, zeigen beide Zellen wieder die alten Werte
  - IP-Iteration: `loadOf` gibt `undefined`
- [x] Unit (Vitest, `program-board.component.spec.ts`): jede Entwicklungs-Zelle zeigt «13 / 20 SP»; die IP-Spalte
  zeigt keine Kapazität; eine überlastete Zelle trägt den Text «Überlast» und ist markiert, eine genau ausgelastete
  nicht
- [x] Unit (Vitest, `board-card.component.spec.ts`): Karte ohne Schätzung zeigt «ohne Schätzung», Karte mit
  Schätzung 0 zeigt «0 SP» und keinen Hinweis
- [x] Bestehende Tests: `BoardServiceTest`, `ProgramBoardTest` und die Vitest-Fixtures ändern sich nur durch das
  neue Pflichtfeld (Konstruktor bzw. `capacities: []`); keine Erwartung wird abgeschwächt. Grund: Delta → AK 1
- [ ] Von Hand: Screenshot im PR (Farbe und Platz in der Zelle, jsdom misst kein Layout)

## Offene Fragen
- keine
