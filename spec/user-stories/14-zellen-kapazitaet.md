# US-14: Kapazität pro Zelle mit Überlast anzeigen

**ID:** US-14
**Issue:** [#14](https://github.com/LukasW/PIckel/issues/14)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als Team-Mitglied möchte ich in jeder Zelle die geplanten gegenüber den verfügbaren Story Points sehen und Überlast
sofort erkennen, damit wir uns nicht überplanen.

## Akzeptanzkriterien

1. Jede Zelle ausser in der IP-Iteration zeigt «geplant / verfügbar SP»
2. Geplant ist die Summe der Schätzungen der Features in der Zelle; Features ohne Schätzung zählen 0
3. Ist geplant grösser als verfügbar, ist die Zelle rot hervorgehoben und mit «Überlast» beschriftet
4. Nach dem Einplanen, Verschieben oder Zurücknehmen eines Features stimmen die Werte ohne Neuladen
5. Feature-Karten ohne Schätzung tragen den Hinweis «ohne Schätzung»

Verfügbar ist die erfasste Kapazität des Teams in der Iteration, ohne Eintrag 0 (US-7, ADR 0006); gleich viel wie
verfügbar ist keine Überlast. Die IP-Iteration zeigt keine Kapazität. Überlast wird nur angezeigt: Einplanen über
die Kapazität bleibt erlaubt. Das Board liefert pro Zelle die verfügbare Kapazität; geplant und Überlast leitet die
Oberfläche aus den Karten auf dem Board ab. Nicht in dieser Story: Summen pro Team oder Iteration, Warnung oder
Sperre beim Einplanen, Kapazität auf dem Board bearbeiten.

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/14-zellen-kapazitaet.feature`](../../src/test/resources/features/program-increments/14-zellen-kapazitaet.feature).

## Tests

| Kriterium | Test |
|---|---|
| 1 «geplant / verfügbar SP» ausser IP | `BoardServiceTest` → `boardCarriesCapacitiesOfProgramIncrementAsDeliveredByUseCase`; CucumberIT «Board liefert die Kapazität pro Zelle»; `@Unit` «Zelle zeigt geplant gegenüber verfügbar»; Vitest `board.service.spec.ts` → «zeigt geplant gegenüber verfügbar: 13 von 20, nicht überlastet (US-14)», «hat in der IP-Iteration keine Last (US-14)»; `program-board.component.spec.ts` → «zeigt in der Zelle «geplant / verfügbar SP» und keine Überlast (US-14)», «zeigt die Anzeige in jeder Zelle einer Entwicklungs-Iteration, auch leer (US-14)», «zeigt in der IP-Spalte keine Kapazität (US-14)» |
| 2 Summe, ohne Schätzung 0 | `@Unit` «Feature ohne Schätzung zählt 0»; Vitest `board.service.spec.ts` → «summiert die Schätzungen der Features einer Zelle, Karten anderer Zellen und des Backlogs zählen nicht (US-14)», «zählt ein Feature ohne Schätzung als 0: «0 / 0» bei einem Team ohne Kapazität (US-14)», «zählt ein Feature ohne Schätzung neben geschätzten Features als 0 (US-14)»; `program-board.component.spec.ts` → «zählt ein Feature ohne Schätzung als 0 und zeigt auf der Karte «ohne Schätzung» (US-14)» |
| 3 Überlast rot und beschriftet | `@Unit` «Zelle über der Kapazität ist überlastet», «Genau ausgelastete Zelle ist nicht überlastet»; Vitest `board.service.spec.ts` → «ist überlastet, wenn geplant grösser als verfügbar ist: 21 > 20 (US-14)», «ist bei genau ausgelasteter Zelle nicht überlastet: 13 = 13 (US-14)»; `program-board.component.spec.ts` → «hebt eine Zelle über der Kapazität rot hervor und beschriftet sie mit «Überlast» (US-14)», «markiert eine genau ausgelastete Zelle nicht als Überlast (US-14)» |
| 4 Ohne Neuladen | `@Unit` «Werte folgen dem Einplanen ohne Neuladen»; Vitest `board.service.spec.ts` → «folgt dem Verschieben sofort, ohne neu zu laden (US-14)», «zeigt in beiden Zellen wieder die Werte von vorher, wenn das Verschieben scheitert (US-14)», «steigt sofort, wenn ein Feature aus dem Backlog eingeplant wird (US-14)», «sinkt sofort, wenn ein Feature zurück ins Backlog genommen wird (US-14)», «zeigt wieder die Werte von vorher, wenn das Zurücknehmen scheitert (US-14)»; `program-board.component.spec.ts` → «zeigt nach dem Verschieben die neuen Werte ohne Neuladen, nach einem Fehler wieder die alten (US-14)» |
| 5 «ohne Schätzung» auf der Karte | `@Unit` «Feature ohne Schätzung zählt 0»; Vitest `board-card.component.spec.ts` → «trägt auf einer Karte ohne Schätzung den Hinweis «ohne Schätzung» (US-14)», «zeigt bei Schätzung 0 «0 SP» und keinen Hinweis (US-14)» |

## Änderungen

- #14: Kapazität pro Zelle mit Überlast anzeigen — `spec/changes/14-zellen-kapazitaet/delta.md`
