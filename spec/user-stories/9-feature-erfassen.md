# US-9: Feature im PI-Backlog erfassen

**ID:** US-9
**Issue:** [#9](https://github.com/LukasW/PIkel/issues/9)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als Product Manager möchte ich Features im Backlog eines PI erfassen, damit sie auf dem Program Board eingeplant werden
können.

## Akzeptanzkriterien

1. Ein Feature hat einen Titel; Pflicht, getrimmt, höchstens 200 Zeichen
2. Beschreibung (höchstens 5000 Zeichen), WSJF-Rang (ganze Zahl ab 1), Owner (höchstens 100 Zeichen) und Schätzung
   (ganze Zahl ab 0, Story Points) sind optional
3. Ein neues Feature gehört zum PI, in dem es erfasst wird, und ist noch nicht eingeplant
4. Das Formular zeigt fehlende oder ungültige Angaben am Feld an und legt dann nichts an
5. Das Backlog ist nach WSJF-Rang aufsteigend sortiert, Features ohne Rang am Ende, bei gleichem Rang nach Titel ohne
   Gross-/Kleinschreibung

## BDD-Szenarien

Siehe [`src/test/resources/features/features/9-feature-erfassen.feature`](../../src/test/resources/features/features/9-feature-erfassen.feature).

## Änderungen

- #9: Feature im PI-Backlog erfassen — `spec/changes/9-feature-erfassen/delta.md`
