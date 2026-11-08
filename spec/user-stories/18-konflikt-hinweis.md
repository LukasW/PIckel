# US-18: Hinweis bei Konflikt einer Abhängigkeit

**ID:** US-18
**Issue:** [#18](https://github.com/LukasW/PIckel/issues/18)
**Status:** Umgesetzt
**Priorität:** Mittel
**Schätzung:** —

## Beschreibung

Als RTE möchte ich sehen, wenn ein Feature später geliefert wird, als es gebraucht wird, damit der Konflikt noch im
Planning gelöst wird.

Eine Abhängigkeit ([US-16](16-abhaengigkeit-erfassen.md), als Linie in [US-17](17-abhaengigkeit-linie.md)) ist nur
dann ein Problem, wenn die Lieferung zu spät kommt. Ein **Konflikt** liegt vor, wenn die Lieferung in einer späteren
Iteration eingeplant ist als der Bedarf; dieselbe Iteration ist kein Konflikt. Ist der Bedarf eingeplant und die
Lieferung nicht, gibt es den eigenen Hinweis «Lieferung nicht eingeplant». Ist der Bedarf nicht eingeplant, gibt es
weder Konflikt noch Hinweis. Das Team spielt keine Rolle.

## Akzeptanzkriterien

1. Eine Abhängigkeit ist ein Konflikt, wenn die Lieferung in einer späteren Iteration eingeplant ist als der Bedarf
2. Ist der Bedarf eingeplant und die Lieferung nicht, zeigt die Karte des Bedarfs den Hinweis «Lieferung nicht
   eingeplant»
3. Die Linie eines Konflikts ist rot und gestrichelt
4. Eine Liste neben dem Board nennt jeden Konflikt und jede nicht eingeplante Lieferung im Klartext; ohne solche
   zeigt sie «Keine Konflikte»
5. Nach dem Einplanen, Verschieben oder Zurücknehmen stimmen Konflikte und Hinweise ohne Neuladen; scheitert das
   Speichern, gilt wieder der alte Stand

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/18-konflikt-hinweis.feature`](../../src/test/resources/features/program-increments/18-konflikt-hinweis.feature).

## Tests

| Kriterium | Test |
|---|---|
| 1 Konflikt bei späterer Lieferung | Vitest `board.service.spec.ts` → «meldet einen Konflikt, wenn Login in Iteration 4 geliefert wird und Portal es in Iteration 2 braucht (US-18)», «meldet keinen Konflikt, wenn Login in derselben Iteration geliefert wird, wie Portal es braucht (US-18)», «meldet keinen Konflikt, wenn Login früher geliefert wird, als Portal es braucht (US-18)», «meldet den Konflikt auch, wenn beide Features beim selben Team liegen (US-18)», «markiert die Linie von Login zu Portal als Konflikt, wenn Login später kommt (US-18)» |
| 2 Hinweis «Lieferung nicht eingeplant» an der Karte des Bedarfs | Vitest `board.service.spec.ts` → «meldet «Lieferung nicht eingeplant», wenn Portal in Iteration 2 eingeplant ist und Login im Backlog liegt (US-18)», «meldet nichts, wenn Portal im Backlog liegt und Login eingeplant ist (US-18)», «meldet nichts, wenn Portal und Login beide im Backlog liegen (US-18)», «fehlt an Portal eine Lieferung, wenn nur eine von zwei Lieferungen nicht eingeplant ist (US-18)»; `board-card.component.spec.ts` → «trägt den Hinweis «Lieferung nicht eingeplant», wenn dem Feature eine Lieferung fehlt (US-18)», «zeigt den Hinweis rot mit dem Token text-danger (US-18)», «trägt den Hinweis nicht, wenn keine Lieferung fehlt (US-18)»; `program-board.component.spec.ts` → «nennt in der Liste, dass Login nicht eingeplant ist, und Portal trägt «Lieferung nicht eingeplant» (US-18)», «zeigt an Portal keinen Hinweis «Lieferung nicht eingeplant», wenn Login eingeplant ist, auch zu spät (US-18)» |
| 3 Konfliktlinie rot und gestrichelt | Vitest `program-board.component.spec.ts` → «markiert die Linie von Login zu Portal als Konflikt: rot und gestrichelt (US-18)», «markiert die Linie von Login zu Portal nicht, wenn Login in derselben Iteration kommt (US-18)», «zeichnet den Pfeil einer Konfliktlinie mit eigenem Marker, eine normale Linie nicht (US-18)», «bleibt rot und gestrichelt, wenn die Karte der Konfliktlinie hervorgehoben ist (US-18)» |
| 4 Liste «Konflikte» im Klartext | Vitest `program-board.component.spec.ts` → «nennt in der Liste «Konflikte», dass Login in Iteration 4 zu spät für Portal in Iteration 2 kommt (US-18)», «nennt jeden Konflikt und jede nicht eingeplante Lieferung als eigenen Eintrag (US-18)», «zeigt «Keine Konflikte», wenn Login in derselben Iteration geliefert wird, wie Portal es braucht (US-18)», «zeigt «Keine Konflikte» bei einem Board ohne Abhängigkeiten (US-18)», «zeigt den Abschnitt «Konflikte» mit Überschrift oberhalb des Rasters (US-18)»; `board.service.spec.ts` → «ordnet die Befunde nach der Iteration des Bedarfs, früheste zuerst (US-18)», «ordnet bei gleicher Iteration nach Titel des Bedarfs, dann nach Titel der Lieferung (US-18)» |
| 5 Ohne Neuladen, Rücknahme bei Fehler | Vitest `board.service.spec.ts` → «löst den Konflikt sofort, wenn Login in Iteration 1 verschoben wird, ohne das Board neu zu laden (US-18)», «meldet den Konflikt sofort, wenn Login aus dem Backlog in eine spätere Iteration eingeplant wird (US-18)», «macht aus dem Konflikt eine nicht eingeplante Lieferung, wenn Login zurück ins Backlog geht (US-18)», «meldet nichts mehr, wenn Portal zurück ins Backlog geht (US-18)», «stellt den Konflikt wieder her, wenn das Verschieben von Login scheitert (US-18)», «meldet wieder den Konflikt statt der fehlenden Lieferung, wenn das Zurücknehmen von Login scheitert (US-18)»; `program-board.component.spec.ts` → «zeigt «Keine Konflikte», nachdem Login per Aktion in Iteration 1 eingeplant ist, ohne Neuladen (US-18)», «nennt wieder den Konflikt, wenn das Speichern beim Einplanen von Login scheitert (US-18)» |

Farbe und Strich der Linie misst kein Test (jsdom rendert kein SVG); geprüft sind `data-conflict`,
`stroke-dasharray` und das Token `text-danger`.

## Änderungen

- #18: Hinweis bei Konflikt einer Abhängigkeit — `spec/changes/18-konflikt-hinweis/delta.md`
