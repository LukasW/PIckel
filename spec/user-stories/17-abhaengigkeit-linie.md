# US-17: Abhängigkeiten als Linie auf dem Board

**ID:** US-17
**Issue:** [#17](https://github.com/LukasW/PIckel/issues/17)
**Status:** Umgesetzt
**Priorität:** Mittel
**Schätzung:** —

## Beschreibung

Als Teilnehmerin oder Teilnehmer des Plannings möchte ich Abhängigkeiten als Linie zwischen den Features auf dem
Board sehen, damit ich die Verflechtung der Teams auf einen Blick erkenne.

Der «Red String» an der Wand zeigt auf einen Blick, welche Teams voneinander abhängen; das Board zeigt dieselbe
Sicht. Erfasst werden die Abhängigkeiten in [US-16](16-abhaengigkeit-erfassen.md). Die Linie führt von der
liefernden zur brauchenden Karte, mit Pfeil zur brauchenden Karte. Die Linien sind dekorativ; die Abhängigkeiten
selbst stehen im Backlog.

## Akzeptanzkriterien

1. Das Board liefert die Abhängigkeiten des PI mit
2. Sind beide Features einer Abhängigkeit eingeplant, verbindet eine Linie mit Pfeil die liefernde mit der
   brauchenden Karte
3. Liegt eines der beiden Features im Backlog, gibt es keine Linie; beide Karten zeigen die Anzahl ihrer
   Abhängigkeiten ohne Linie
4. Fokus oder Hover auf einer Karte hebt deren Linien hervor und blendet die übrigen ab
5. Nach dem Einplanen, Verschieben oder Zurücknehmen folgen die Linien ohne Neuladen

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/17-abhaengigkeit-linie.feature`](../../src/test/resources/features/program-increments/17-abhaengigkeit-linie.feature).

## Tests

| Kriterium | Test |
|---|---|
| 1 Board liefert die Abhängigkeiten | `BoardServiceTest` → `boardCarriesDependenciesOfProgramIncrement`; CucumberIT «Board liefert die Abhängigkeiten» |
| 2 Linie mit Pfeil zwischen zwei eingeplanten Karten | Vitest `board.service.spec.ts` → «zieht eine Linie von Login zu Portal, wenn beide eingeplant sind (US-17)», «zieht nur Linien für Abhängigkeiten, deren beide Features eingeplant sind (US-17)»; `dependency-lines.spec.ts` → `lineBetween` (Seitenkanten, dieselbe Spalte, teilweise Überlappung, Berührung an der Kante, nicht gemessene Karte); `program-board.component.spec.ts` → «zieht eine Linie von Login zu Portal, wenn beide eingeplant sind (US-17)», «zeichnet die Linie mit einem Pfeil und verbirgt die Linien vor Screenreadern (US-17)» |
| 3 Keine Linie mit Backlog-Feature, Anzahl an beiden Karten | Vitest `board.service.spec.ts` → «zieht keine Linie, wenn das liefernde Feature im Backlog liegt (US-17)», «zieht keine Linie, wenn das brauchende Feature im Backlog liegt (US-17)», «zählt an beiden Karten die Abhängigkeit ohne Linie, wenn ein Feature im Backlog liegt (US-17)», «zählt Abhängigkeiten ohne Linie in beide Richtungen, eingeplante Paare nicht (US-17)», «zählt nichts, wenn beide Features eingeplant sind (US-17)»; `board-card.component.spec.ts` → «zeigt «1 Abhängigkeit» bei einer Abhängigkeit ohne Linie (US-17)», «zeigt «2 Abhängigkeiten» bei zwei Abhängigkeiten ohne Linie (US-17)», «zeigt keine Anzahl, wenn alle Abhängigkeiten eine Linie haben (US-17)»; `program-board.component.spec.ts` → «zieht keine Linie und zeigt an beiden Karten 1 Abhängigkeit, wenn Login im Backlog liegt (US-17)» |
| 4 Fokus oder Hover hebt hervor | Vitest `dependency-lines.spec.ts` → `emphasisOf` (alle fünf Fälle); `program-board.component.spec.ts` → «hebt bei Hover auf Suche die Linie Login → Suche hervor und blendet Login → Portal ab (US-17)», «hebt bei Tastaturfokus auf Suche dieselben Linien hervor wie Hover (US-17)», «behält die Hervorhebung, wenn der Fokus innerhalb derselben Karte weiterwandert (US-17)», «zeigt ohne Hover und Fokus alle Linien normal (US-17)» |
| 5 Linien folgen ohne Neuladen | Vitest `board.service.spec.ts` → «zieht nach dem Einplanen die Linie und zählt nichts mehr, ohne das Board neu zu laden (US-17)», «behält die Linie, wenn ein Feature in eine andere Zelle verschoben wird (US-17)», «nimmt die Linie weg und zählt wieder, wenn ein Feature zurück ins Backlog geht (US-17)», «lässt Linie und Zähler wie vorher, wenn das Einplanen scheitert (US-17)»; `program-board.component.spec.ts` → «zieht nach dem Einplanen per Aktion die Linie, ohne das Board neu zu laden (US-17)» |

Lage und Aussehen der Linien prüft kein Test (jsdom misst kein Layout); ein Screenshot im PR belegt sie.

## Änderungen

- #17: Abhängigkeiten als Linie auf dem Board — `spec/changes/17-abhaengigkeit-linie/delta.md`
