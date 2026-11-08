# US-6: Teams des ART erfassen

**ID:** US-6
**Issue:** [#6](https://github.com/LukasW/PIckel/issues/6)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als RTE möchte ich die Teams des ART erfassen, umbenennen und entfernen, damit sie in jedem PI für die Planung
bereitstehen.

## Akzeptanzkriterien

1. Ein Team hat einen Namen; Pflicht, getrimmt, höchstens 50 Zeichen
2. Zwei Teams können nicht gleich heissen, auch nicht in anderer Gross- und Kleinschreibung
3. Die Teamliste ist alphabetisch nach Name sortiert
4. Ein Team lässt sich umbenennen, mit denselben Regeln wie beim Anlegen
5. Ein Team lässt sich nach Bestätigung entfernen und erscheint danach in keinem PI mehr; die Bestätigung weist
   darauf hin, dass seine eingeplanten Features in allen PIs zurück ins Backlog gehen
6. Ohne Teams zeigt die Seite einen Leerzustand mit dem Hinweis, das erste Team zu erfassen

## BDD-Szenarien

Siehe [`src/test/resources/features/teams/6-teams-erfassen.feature`](../../src/test/resources/features/teams/6-teams-erfassen.feature).

## Änderungen

- #6: Teams des ART erfassen — `spec/changes/6-teams-erfassen/delta.md`
- #13: Feature einplanen — Hinweis in der Bestätigung beim Entfernen; `spec/changes/13-feature-einplanen/delta.md`
