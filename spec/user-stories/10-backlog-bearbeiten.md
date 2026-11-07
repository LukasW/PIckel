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
3. Ein Feature lässt sich bearbeiten, mit denselben Regeln wie beim Erfassen
4. Ein Feature lässt sich nach Bestätigung löschen
5. Ohne Features zeigt das Backlog einen Leerzustand mit dem Hinweis, das erste Feature zu erfassen
6. Ein unbekanntes Feature oder eines aus einem anderen PI lässt sich unter diesem PI weder bearbeiten noch löschen
   (404 mit Meldung)

Nicht in dieser Story: Rang per Drag & Drop ändern, Filter und Suche. Was mit Abhängigkeiten eines gelöschten Features
geschieht, regelt #16.

## BDD-Szenarien

Siehe [`src/test/resources/features/features/10-backlog-bearbeiten.feature`](../../src/test/resources/features/features/10-backlog-bearbeiten.feature).

## Änderungen

- #10: Backlog nach WSJF-Rang bearbeiten — `spec/changes/10-backlog-bearbeiten/delta.md`
