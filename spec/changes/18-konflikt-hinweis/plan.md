# US-18 — Hinweis bei Konflikt einer Abhängigkeit (Issue #18)

## Übersicht

Reines Frontend, baut auf #17 (das Board liefert die Abhängigkeiten, `BoardService` kennt die Links). `BoardService`
leitet als `computed` über dem Board-Signal je Abhängigkeit ab, ob sie ein Konflikt ist (Lieferung später als der
Bedarf) oder ob die Lieferung fehlt (Bedarf eingeplant, Lieferung im Backlog). Das Board zeichnet die Linie eines
Konflikts rot und gestrichelt, die Karte des Bedarfs trägt bei fehlender Lieferung den Hinweis «Lieferung nicht
eingeplant», und eine Liste «Konflikte» nennt alles im Klartext. Weil alles aus demselben Signal kommt wie bei #14
und #17, stimmt es sofort nach dem Einplanen und kehrt bei einem Fehler zurück. Kein Backend, keine Migration.

## Zu erstellende Dateien
- [x] `spec/user-stories/18-konflikt-hinweis.md` — Story (aus dem Delta eingepflegt)
- [x] `src/test/resources/features/program-increments/18-konflikt-hinweis.feature` — fünf `@Unit` mit
  `# Vitest:`-Verweisen

## Zu ändernde Dateien
- [x] `src/main/webapp/src/app/core/models/board.model.ts` — Typ `DependencyIssue` (`kind: 'conflict' |
  'unscheduledDelivery'`, `needingFeatureId`, `neededFeatureId`, Titel und Iterationsnamen für den Text);
  `DependencyLink` um `conflict: boolean`
- [x] `src/main/webapp/src/app/core/services/board.service.ts` — `computed` `dependencyIssues`, Konfliktflag an
  `dependencyLinks`, Methode `lacksDelivery(featureId)`
- [x] `src/main/webapp/src/app/core/services/board.service.spec.ts` — Tests zur Regel
- [x] `src/main/webapp/src/app/features/program-board/program-board.component.html` / `.ts` — Konfliktlinie
  (`text-danger`, `stroke-dasharray`, eigener roter Pfeil-Marker), Abschnitt «Konflikte» mit Liste bzw. «Keine
  Konflikte»
- [x] `src/main/webapp/src/app/features/program-board/program-board.component.spec.ts` — Tests dazu
- [x] `src/main/webapp/src/app/features/program-board/board-card.component.ts` / `.html` — Input `lacksDelivery`,
  Hinweis «Lieferung nicht eingeplant» (`text-danger`, mit Text, nicht nur Farbe)
- [x] `src/main/webapp/src/app/features/program-board/board-card.component.spec.ts` — Tests dazu

## Delta
`spec/changes/18-konflikt-hinweis/delta.md` — Neu: 5 AK, 5 Szenarien (alle `@Unit`). Gegenüber dem Entwurf:
Entscheid «Berechnung in der Domain» durch «Frontend» ersetzt (Rückfrage, wie #14); Szenarien von «Wenn ich das
Board abrufe» (REST) auf «Wenn ich das Board ansehe» (Oberfläche) umgestellt; AK 2 nennt die Karte des Bedarfs als
Ort des Hinweises; AK 5 und Szenario «Gescheitertes Speichern stellt den Konflikt wieder her» um die Rücknahme
ergänzt. Geändert: keine. Entfernt: keine.

## Architektur-Entscheide
- **Schicht:** Die Konfliktregel ist Fachlogik und steht in `BoardService` (`core/services`), nicht in der
  Komponente; die Komponente setzt nur Klassen und Text. Kein Java-Code, `ArchitectureTest` unberührt
- **Datenquelle:** die Abhängigkeiten im Board aus #17 und die Einplanung an den Features; Iterationsnamen aus
  `board.iterations` (keine zweite Quelle für «Iteration 4»)
- **Regel:** Bedarf nicht eingeplant → nichts. Bedarf eingeplant, Lieferung nicht → `unscheduledDelivery`. Beide
  eingeplant und Iterationsnummer der Lieferung > der des Bedarfs → `conflict`. Gleich oder früher → nichts. Das Team
  spielt keine Rolle
- **Reihenfolge der Liste:** nach Iteration des Bedarfs, dann Titel des Bedarfs, dann Titel der Lieferung (stabil,
  unabhängig von der Reihenfolge der Abhängigkeiten im JSON)
- **Text** steht im Template, nicht im Service: «{Lieferung} wird in {Iteration L} geliefert, {Bedarf} braucht es in
  {Iteration B}.» bzw. «{Lieferung} ist nicht eingeplant, {Bedarf} braucht es in {Iteration B}.»
- **Abwägung Ort der Berechnung:** Frontend statt Domain (Rückfrage). Die Domain-Variante hätte die Regel für
  andere Konsumenten (Bericht, Export) in der API, braucht aber ein Nachladen nach jedem Einplanen und hinkt dem
  optimistischen Board nach. Braucht später ein anderer Konsument Konflikte, kommt die Regel mit dieser Story in die
  Domain (wie bei #14 festgehalten)
- Keine neue ADR

## Aufgaben
- [x] 1. `DependencyIssue` und `BoardService.dependencyIssues` / `lacksDelivery` / Konfliktflag am Link — erledigt,
  wenn die Regel-Tests in `board.service.spec.ts` grün sind
- [x] 2. Hinweis «Lieferung nicht eingeplant» auf der Karte — erledigt, wenn die Tests in
  `board-card.component.spec.ts` grün sind
- [x] 3. Konfliktlinie rot und gestrichelt — erledigt, wenn der Test in `program-board.component.spec.ts` grün ist
- [x] 4. Liste «Konflikte» mit Klartext und «Keine Konflikte» — erledigt, wenn die Tests in
  `program-board.component.spec.ts` grün sind
- [x] 5. Feature-Datei mit `@Unit`-Verweisen; Story anlegen, Delta einpflegen — erledigt, wenn
  `check-story-drift.sh` nichts meldet; `scripts/gate.sh` grün; Screenshot für den PR
- [x] 6. `docs/arc42.md` → «Frontend-State» um den Satz zu Board-Regeln als `computed` ergänzen — erledigt, wenn der
  Satz dort steht

## Test-Strategie
- [x] Unit (Vitest, `BoardService`), mit Iterationsnummern:
  - Bedarf 2, Lieferung 4 → ein `conflict`, Link `conflict: true`
  - Bedarf 2, Lieferung 2 → nichts, Link `conflict: false` (Grenzfall gleich)
  - Bedarf 3, Lieferung 1 → nichts
  - Bedarf 2, Lieferung im Backlog → ein `unscheduledDelivery`, `lacksDelivery("Portal")` wahr,
    `lacksDelivery("Login")` falsch
  - Bedarf im Backlog, Lieferung eingeplant oder im Backlog → nichts
  - Reihenfolge: zwei Konflikte mit Bedarf in Iteration 3 und 1 → der mit 1 zuerst
  - `schedule("Login", Iteration 1)` löst den Konflikt ohne zweiten `get`-Aufruf am Repository; `unschedule("Login")`
    macht daraus `unscheduledDelivery`
  - scheitert das Speichern, gilt wieder der Konflikt
- [x] Unit (Vitest, Board): Text «Login wird in Iteration 4 geliefert, Portal braucht es in Iteration 2.» bzw.
  «Login ist nicht eingeplant, Portal braucht es in Iteration 2.» in der Liste; ohne Befund «Keine Konflikte»;
  Konfliktlinie trägt `data-conflict` und `stroke-dasharray`, eine Linie ohne Konflikt nicht; nach Einplanen per
  Aktion «Keine Konflikte»; bei Fehler des Repositorys wieder der Konflikttext
- [x] Unit (Vitest, Karte): mit `lacksDelivery` der Text «Lieferung nicht eingeplant», ohne nicht
- [x] Bestehende Tests: keine Änderung erwartet; die Tests aus #17 zu `dependencyLinks` vergleichen ganze Objekte
  vermutlich mit `toEqual` und brauchen dann das Feld `conflict: false`. Grund: Delta US-18 → AK 3
- [ ] Von Hand: Screenshot im PR mit roter gestrichelter Linie, Hinweis an der Karte und Liste (Farbe und Strich
  misst jsdom nicht)

## Offene Fragen
Entschieden mit «OK»: Liste «Konflikte» oberhalb des Rasters; arc42-Satz als Aufgabe 6; Reihenfolge #14 → #17 → #18.

- **Lage der Liste «Konflikte»:** Das Board nutzt seit #37 die volle Fensterbreite. Vorschlag: Abschnitt
  «Konflikte» oberhalb des Rasters (unter der Überschrift), damit er ohne Scrollen sichtbar ist und das Raster nicht
  schmaler wird; AK 4 sagt «neben dem Board». Alternative: eigene Spalte rechts
- **arc42 nachführen?** Mit #14 und #18 rechnet das Frontend zum zweiten Mal Board-Regeln aus dem Board-Signal,
  weil das Board optimistisch einplant. Vorschlag: ein Satz in `docs/arc42.md` → «Frontend-State» («Regeln, die dem
  optimistischen Einplanen sofort folgen müssen, leitet `BoardService` als `computed` aus dem Board-Signal ab; die
  REST-API liefert sie nicht»), als Aufgabe 6 in diesem Plan
- Reihenfolge: #18 setzt #17 voraus (Board liefert Abhängigkeiten, `dependencyLinks`, SVG-Overlay); umsetzen erst
  nach dem Merge von #17
