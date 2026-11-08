# US-10: Backlog nach WSJF-Rang bearbeiten

**ID:** US-10
**Issue:** [#10](https://github.com/LukasW/PIkel/issues/10)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als Product Manager möchte ich die Features eines PI nach WSJF-Rang sehen, bearbeiten und löschen, damit das Planning
mit einer aktuellen, priorisierten Liste startet.

## Akzeptanzkriterien

1. Das Backlog eines PI listet seine Features mit Titel, WSJF-Rang, Owner und Schätzung, aufsteigend nach WSJF-Rang
2. Features ohne WSJF-Rang stehen am Ende; bei gleichem Rang entscheidet der Titel alphabetisch
3. Ein Feature lässt sich in seiner Zeile bearbeiten, mit denselben Regeln wie beim Erfassen; das Formular zum
   Erfassen bleibt dabei leer, und es wird höchstens ein Feature zugleich bearbeitet
4. Ein Feature lässt sich nach Bestätigung löschen
5. Ohne Features zeigt das Backlog einen Leerzustand mit dem Hinweis, das erste Feature zu erfassen
6. Ein unbekanntes Feature oder eines aus einem anderen PI lässt sich unter diesem PI weder bearbeiten noch löschen
   (404 mit Meldung)

Nicht in dieser Story: Rang per Drag & Drop ändern, Filter und Suche. Was mit Abhängigkeiten eines gelöschten Features
geschieht, regelt #16.

## BDD-Szenarien

Siehe [`src/test/resources/features/features/10-backlog-bearbeiten.feature`](../../src/test/resources/features/features/10-backlog-bearbeiten.feature).

## Tests

- AK 3 (Bearbeiten in der Zeile): `@Unit` «Feature wird in seiner Zeile bearbeitet»; Vitest
  `feature-backlog.component.spec.ts` → «macht beim Bearbeiten die Zeile des Features zum Formular, das Erfassen
  bleibt leer (US-10)», «zeigt nach dem Speichern die neuen Angaben in der Zeile des Features (US-10)», «schliesst
  beim Bearbeiten eines zweiten Features das erste ohne zu speichern (US-10)», «füllt beim Bearbeiten das Formular in
  der Zeile mit den Angaben des Features (US-10)», die Tests «lehnt beim Bearbeiten … am Feld der Zeile ab und
  speichert nichts (US-10)»; CucumberIT «Feature wird bearbeitet», «Bearbeiten mit leerem Titel wird abgelehnt»
- Übrige Kriterien: siehe Feature-Datei

## Änderungen

- #10: Backlog nach WSJF-Rang bearbeiten — `spec/changes/10-backlog-bearbeiten/delta.md`
- #50: Nacharbeiten aus dem Playwright-Test — `spec/changes/50-nacharbeiten-playwright/delta.md`
