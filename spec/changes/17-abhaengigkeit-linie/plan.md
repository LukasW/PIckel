# US-17 — Abhängigkeiten als Linie auf dem Board (Issue #17)

## Übersicht

Das Board liefert die Abhängigkeiten des PI mit (`ProgramBoard.dependencies`, über `FeatureDependencyUseCase.list`).
Das Frontend leitet daraus als `computed` über dem Board-Signal ab, welche Abhängigkeiten eine Linie bekommen (beide
Features eingeplant) und wie viele Abhängigkeiten jeder Karte ohne Linie sind. Weil das Signal dasselbe ist, das
das optimistische Einplanen aus #13 ändert, folgen Linien und Zähler ohne Neuladen und kehren bei einem Fehler mit
der Karte zurück. Die Komponente misst die Karten im Raster und zeichnet die Linien als SVG über der Tabelle; die
Geometrie ist eine reine Funktion. Fokus oder Hover auf einer Karte hebt deren Linien hervor. Keine Migration.

## Zu erstellende Dateien
- [x] `src/main/webapp/src/app/features/program-board/dependency-lines.ts` — reine Funktionen: Geometrie einer Linie
  aus zwei Karten-Rechtecken, Hervorhebung einer Linie bei fokussierter Karte (UI, ohne Angular)
- [x] `src/main/webapp/src/app/features/program-board/dependency-lines.spec.ts` — Vitest dazu
- [x] `spec/user-stories/17-abhaengigkeit-linie.md` — Story (aus dem Delta eingepflegt)
- [x] `src/test/resources/features/program-increments/17-abhaengigkeit-linie.feature` — ein CucumberIT-Szenario,
  vier `@Unit` mit `# Vitest:`-Verweisen

## Zu ändernde Dateien
- [x] `src/main/java/ch/grudligstrasse/pikel/domain/model/ProgramBoard.java` — Komponente
  `List<FeatureDependency> dependencies` (Kopie im Compact Constructor)
- [x] `src/main/java/ch/grudligstrasse/pikel/application/service/BoardService.java` — `FeatureDependencyUseCase`
  injizieren, `list(piId)` ins Board
- [x] `src/main/java/ch/grudligstrasse/pikel/adapter/in/rest/BoardDto.java` — Feld `dependencies`
  (`List<FeatureDependencyDto>`, bestehender DTO)
- [x] `src/test/java/ch/grudligstrasse/pikel/application/service/BoardServiceTest.java` — `@Mock
  FeatureDependencyUseCase`, neuer Test
- [x] `src/test/java/ch/grudligstrasse/pikel/domain/model/ProgramBoardTest.java` — Konstruktoraufruf ergänzen
- [x] `src/test/java/ch/grudligstrasse/pikel/cucumber/steps/BoardSteps.java` — Dann-Step «liefert das Board die
  Abhängigkeit {string} braucht {string}»
- [x] `src/main/webapp/src/app/core/models/board.model.ts` — `dependencies: readonly FeatureDependency[]`, Typ
  `DependencyLink` (`neededFeatureId`, `needingFeatureId`)
- [x] `src/main/webapp/src/app/core/services/board.service.ts` — `computed` `dependencyLinks` (beide eingeplant) und
  Map der Abhängigkeiten ohne Linie pro Feature, Methode `unlinkedDependencyCount(featureId)`
- [x] `src/main/webapp/src/app/core/services/board.service.spec.ts` — Tests dazu; Fixtures `dependencies: []`
- [x] `src/main/webapp/src/app/features/program-board/program-board.component.ts` / `.html` — Raster in einen
  `relative`-Container, SVG-Overlay (`pointer-events-none`, `aria-hidden`), Messung der Karten über
  `data-feature-id`, Signal `focusedFeatureId` aus `mouseenter`/`mouseleave`/`focusin`/`focusout` am `li`
- [x] `src/main/webapp/src/app/features/program-board/program-board.component.spec.ts` — Tests; Fixture
  `dependencies: []`
- [x] `src/main/webapp/src/app/features/program-board/board-card.component.ts` / `.html` — Input
  `unlinkedDependencies` (Zahl), Anzeige «1 Abhängigkeit» / «n Abhängigkeiten», bei 0 nichts
- [x] `src/main/webapp/src/app/features/program-board/board-card.component.spec.ts` — Tests dazu
- [x] `src/main/webapp/src/app/app.spec.ts` — Fixture `EMPTY_BOARD` um `dependencies: []` ergänzen, sonst nichts
- [x] `spec/user-stories/16-abhaengigkeit-erfassen.md` — Satz «Die Linie auf dem Board folgt mit #17» durch Verweis
  auf US-17 ersetzen (Text der Beschreibung, kein Kriterium)

## Delta
`spec/changes/17-abhaengigkeit-linie/delta.md` — Neu: 5 AK, 5 Szenarien (1 CucumberIT «Board liefert die
Abhängigkeiten», 4 `@Unit`). Gegenüber dem Entwurf: Entscheid «Nur Frontend» ersetzt (Board-Endpunkt liefert die
Abhängigkeiten), Zähler auf beiden Karten präzisiert, Szenarien «Board liefert die Abhängigkeiten» und «Linie folgt
dem Einplanen ohne Neuladen» ergänzt. Geändert: keine. Entfernt: keine.

## Architektur-Entscheide
- **Backend:** `ProgramBoard` (Domain) trägt die Abhängigkeiten, `BoardService` (Application) holt sie über den
  Driving Port `FeatureDependencyUseCase`, wie schon Teams, Features und Milestones. Kein neuer Port, keine Tabelle,
  kein neuer Endpunkt; das Feld im Board-JSON ist eine Erweiterung, kein Breaking Change
- **Frontend, Fachregel:** Welche Abhängigkeit eine Linie hat und welche nicht, ist Fachlogik → `BoardService`
  (`core/services`), nicht in der Komponente (`.claude/rules/angular.md`)
- **Frontend, Darstellung:** Geometrie und Hervorhebung sind UI-Regeln ohne Fachbezug → reine Funktionen in
  `features/program-board/dependency-lines.ts`. Die Komponente misst nur und zeichnet
- **Messung:** `afterRenderEffect` liest nach jeder Änderung des Board-Signals die Rechtecke der Karten
  (`getBoundingClientRect`, relativ zur Tabelle, damit waagrechtes Scrollen nichts verschiebt); ein `ResizeObserver`
  auf der Tabelle misst bei Grössenänderung neu. Ergebnis als Signal `Map<featureId, Rect>`
- **Linie:** gerade Strecke zwischen den Kanten der beiden Karten (Lieferung rechts vom Bedarf → linke Kante der
  Lieferung zur rechten Kante des Bedarfs und umgekehrt; dieselbe Spalte → untere/obere Kante), Pfeil über
  SVG-`<marker>` am Bedarf. Farbe über Design-Token (`text-muted` mit `stroke-current`), keine Tailwind-Palette
- **Abwägung Daten:** Board-Endpunkt statt zweitem Aufruf an `/dependencies` (Rückfrage): ein konsistentes Bild in
  einem Aufruf, Entscheid aus #12; kostet ein CucumberIT-Szenario
- **Abwägung Linienform:** gerade Strecke statt Kurve: bei wenigen Linien gleich lesbar, einfacher zu prüfen. Kurven
  wären eine spätere Verbesserung, wenn Linien sich in der Praxis verdecken
- **Barrierefreiheit:** Die Linien sind dekorativ (`aria-hidden`); die Abhängigkeiten selbst stehen im Backlog
  (US-16). Fokus auf einer Karte hebt wie Hover hervor
- Keine neue ADR, kein Abschnitt in `docs/arc42.md` nachzuziehen (siehe aber die Anmerkung in `18-…/plan.md`)

## Aufgaben
- [x] 1. `ProgramBoard.dependencies`, `BoardService` holt sie über `FeatureDependencyUseCase` — erledigt, wenn
  `BoardServiceTest` («Board trägt die Abhängigkeiten des PI») und `ProgramBoardTest` grün sind
- [x] 2. `BoardDto.dependencies`, Dann-Step, Feature-Datei mit dem Szenario «Board liefert die Abhängigkeiten» —
  erledigt, wenn das Szenario in `CucumberIT` grün ist
- [x] 3. Frontend-Modell `Board.dependencies`, Fixtures um `dependencies: []` ergänzen — erledigt, wenn `npm test`
  grün ist
- [x] 4. `BoardService.dependencyLinks` und `unlinkedDependencyCount` als `computed` — erledigt, wenn die Tests in
  `board.service.spec.ts` grün sind
- [x] 5. `dependency-lines.ts` (Geometrie, Hervorhebung) — erledigt, wenn `dependency-lines.spec.ts` grün ist
- [x] 6. Zähler auf der Karte — erledigt, wenn die Tests in `board-card.component.spec.ts` grün sind
- [x] 7. SVG-Overlay, Messung und Hover/Fokus im Board — erledigt, wenn die Tests in
  `program-board.component.spec.ts` grün sind
- [x] 8. `@Unit`-Szenarien mit `# Vitest:`-Verweisen; Story anlegen, Delta einpflegen, Satz in US-16 nachführen —
  erledigt, wenn `check-story-drift.sh` nichts meldet; `scripts/gate.sh` grün; Screenshot für den PR

## Test-Strategie
- [x] Unit (JUnit, `BoardService`): Das Board trägt genau die Liste aus `FeatureDependencyUseCase.list` des PI;
  unbekanntes PI bleibt 404 (bestehender Test)
- [x] Integration (CucumberIT, «Board liefert die Abhängigkeiten»): Das Board-JSON enthält die Abhängigkeit mit
  `needingFeatureId` von "Portal" und `neededFeatureId` von "Login"
- [x] Unit (Vitest, `BoardService`):
  - beide eingeplant → genau ein Link Login → Portal; einer im Backlog → kein Link
  - Zähler ohne Linie zählt beide Richtungen: "Login" (Backlog) gebraucht von "Portal" und "Suche" (beide
    eingeplant) → "Login" 2, "Portal" 1, "Suche" 1; eingeplante Paare zählen nicht
  - nach `schedule` von "Login" entsteht der Link und die Zähler fallen auf 0, ohne zweiten `get`-Aufruf am
    Repository; nach `unschedule` verschwindet er wieder
  - scheitert das Speichern, gelten Link und Zähler wie vorher
- [x] Unit (Vitest, `dependency-lines.ts`), mit Werten:
  - Lieferung {x 0, y 0, b 100, h 40}, Bedarf {x 300, y 100, b 100, h 40} → Strecke von (100, 20) nach (300, 120)
  - Lieferung rechts vom Bedarf → von der linken Kante der Lieferung zur rechten Kante des Bedarfs
  - gleiche Spalte → von der unteren zur oberen Kante (bzw. umgekehrt)
  - fehlt ein Rechteck (Karte nicht gemessen), gibt es keine Linie
  - Hervorhebung: ohne Fokus alle «normal»; Fokus auf "Suche" → Linie Login → Suche «hervorgehoben», Login → Portal
    «abgeblendet»; Fokus auf "Login" → beide hervorgehoben
- [x] Unit (Vitest, Karte): 1 → «1 Abhängigkeit», 2 → «2 Abhängigkeiten», 0 → kein Text
- [x] Unit (Vitest, Board): pro Link ein SVG-Pfad mit `data-from`/`data-to`; `mouseenter` und `focusin` auf
  "Suche" setzen `data-emphasis` der Linien wie im Szenario, `mouseleave`/`focusout` setzen zurück; Backlog-Karte
  "Login" zeigt «1 Abhängigkeit»; nach Einplanen per Aktion entsteht die Linie ohne neuen `get`-Aufruf. (jsdom misst
  0 × 0; geprüft wird, welche Linien da sind und wie sie markiert sind, nicht wo)
- [x] Bestehende Tests: `BoardServiceTest`, `ProgramBoardTest` und die Vitest-Fixtures ändern sich nur durch das
  neue Pflichtfeld; keine Erwartung wird abgeschwächt. Grund: Delta → AK 1
- [ ] Von Hand (offen, im Nachtlauf kein Browser): Screenshot im PR mit Linien zwischen Zellen, Hover-Zustand und waagrecht gescrolltem Raster (jsdom
  misst kein Layout)

## Offene Fragen
Entschieden mit «OK»: Reihenfolge #14 → #17 → #18.

- Reihenfolge mit #14: Beide erweitern `ProgramBoard`, `BoardDto`, `board.model.ts` und dieselben Fixtures. Der
  Change-Ordner von #14 liegt ungetrackt bzw. geändert auf `main` im Arbeitsverzeichnis und käme mit
  `git checkout -b` auf den Branch von #17. Vorschlag: #14 zuerst umsetzen und mergen, dann #17, dann #18. Sonst
  den Change-Ordner von #14 vorher sichern bzw. per `chore/`-PR auf `main` bringen
