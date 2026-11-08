# US-13: Feature auf dem Board einplanen

**ID:** US-13
**Issue:** [#13](https://github.com/LukasW/PIkel/issues/13)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 8

## Beschreibung

Als Team-Mitglied möchte ich ein Feature per Drag & Drop in eine Zelle Team × Iteration legen, verschieben und
zurück ins Backlog nehmen, damit die Planung so schnell geht wie mit Klebezetteln.

## Akzeptanzkriterien

1. Ein Feature lässt sich aus dem Backlog in eine Zelle Team × Iteration ziehen und ist danach dort eingeplant
2. Ein eingeplantes Feature lässt sich in eine andere Zelle verschieben oder zurück ins Backlog ziehen
3. In die IP-Iteration lässt sich kein Feature einplanen; der Versuch wird mit einer verständlichen Meldung abgelehnt
4. Jede Karte lässt sich auch ohne Maus über die Aktion «Einplanen …» einplanen, verschieben und zurücknehmen
5. Schlägt das Speichern fehl, springt die Karte an ihren alten Ort zurück und eine Meldung erscheint
6. Wird ein Team entfernt, kommen seine eingeplanten Features in allen PIs zurück ins Backlog
7. Die Bestätigung beim Entfernen eines Teams weist darauf hin, dass seine eingeplanten Features in allen PIs
   zurück ins Backlog gehen
8. Wird eine Karte ausserhalb einer Ablage losgelassen, bleibt sie an ihrem bisherigen Ort; über der IP-Iteration
   erscheint dazu die Meldung aus AK 3

Die Einplanung ist ein Wert am Feature (Team und Iterationsnummer), ein Feature liegt in höchstens einer Zelle
(ADR 0005, ADR 0006). Innerhalb einer Zelle gilt die Reihenfolge des Backlogs (WSJF-Rang). Nicht in dieser Story:
gleichzeitiges Arbeiten mehrerer Personen, Reihenfolge innerhalb einer Zelle, Kapazitätsprüfung (Überlast zeigt #14).

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/13-feature-einplanen.feature`](../../src/test/resources/features/program-increments/13-feature-einplanen.feature).

## Tests

| Kriterium | Test |
|---|---|
| 1 Aus dem Backlog in eine Zelle ziehen | `FeatureTest` → `scheduleIntoDevelopmentIterationSetsTeamAndIteration`, `scheduleKeepsIdProgramIncrementAndAllValues`, `scheduleAcceptsFirstAndLastDevelopmentIteration`; `ProgramIncrementTest` → `iterationByNumber*`; `FeatureServiceTest` → `scheduleSavesAndReturnsFeatureInCell`, `scheduleRejectsUnknownTeam`; `BoardServiceTest` → `scheduledListsScheduledFeaturesInBacklogOrderWithTheirCells`, `everyFeatureIsEitherInBacklogOrScheduled`; CucumberIT «Feature wird in eine Zelle eingeplant», «Einplanen bei einem unbekannten Team wird abgelehnt», «Einplanen in eine Iteration, die es im PI nicht gibt, wird abgelehnt», «Einplanen ohne Team und Iteration wird abgelehnt» (`SchedulingTest` → `of*`, `FeatureServiceTest` → `scheduleRejectsMissingTeamOrIterationAndSavesNothing`); Vitest `board.service.spec.ts` → «zeigt die Karte sofort in der Zelle, bevor der Server antwortet (US-13)»; `program-board.component.spec.ts` → «zeigt eine eingeplante Karte in ihrer Zelle und nicht im Backlog (US-13)», «plant eine Karte ein, die aus dem Backlog in eine Zelle gezogen wird (US-13)» |
| 2 Verschieben, zurück ins Backlog | `FeatureTest` → `rescheduleReplacesTheCell`, `unscheduleReturnsFeatureToBacklog`, `reviseKeepsScheduling`; `FeatureServiceTest` → `scheduleMovesScheduledFeatureToNewCell`, `unscheduleSavesAndReturnsFeatureWithoutCell`; CucumberIT «Eingeplantes Feature wird verschoben», «Eingeplantes Feature geht zurück ins Backlog», «Bearbeiten eines eingeplanten Features lässt die Einplanung stehen»; Vitest `program-board.component.spec.ts` → «verschiebt eine eingeplante Karte, die in eine andere Zelle gezogen wird (US-13)», «nimmt eine eingeplante Karte zurück, die ins Backlog gezogen wird (US-13)» |
| 3 IP-Iteration gesperrt | `FeatureTest` → `scheduleIntoIpIterationIsRejected`, `ipIterationDependsOnIterationLength`; `FeatureServiceTest` → `scheduleIntoIpIterationSavesNothing`; CucumberIT «Einplanen in die IP-Iteration wird abgelehnt»; Vitest `board-card.component.spec.ts` → «bietet nur die Entwicklungs-Iterationen an, nicht die IP-Iteration (US-13)»; `program-board.component.spec.ts` → «macht das Backlog und die Zellen der Entwicklungs-Iterationen zu Ablagen, die IP nicht (US-13)» |
| 4 Ohne Maus | `@Unit` «Karte wird ohne Maus eingeplant und zurückgenommen»; Vitest `board-card.component.spec.ts` → «lässt sich ganz mit nativen, per Tastatur bedienbaren Elementen einplanen (US-13)»; `program-board.component.spec.ts` → «plant über die Aktion «Einplanen …» an der Karte ohne Maus ein (US-13)», «nimmt über die Aktion «Einplanen …» eine eingeplante Karte ohne Maus zurück (US-13)» |
| 5 Rücknahme bei Fehler | `@Unit` «Gescheitertes Speichern nimmt die Karte zurück»; Vitest `board.service.spec.ts` → «nimmt das Einplanen bei einem Fehler zurück und zeigt die Meldung des Servers (US-13)», «springt beim Verschieben bei einem Fehler in die alte Zelle zurück (US-13)», «nimmt bei einem Fehler nur diese Karte zurück, nicht eine inzwischen gelungene Aktion (US-13)» |
| 6 Team entfernen → Backlog | `TeamServiceTest` → `removeReturnsScheduledFeaturesOfTeamToBacklogBeforeDeletingTeam`; CucumberIT «Entferntes Team gibt seine Features ins Backlog zurück», «Entferntes Team gibt seine Features in allen PIs zurück»; Vitest `program-board.component.spec.ts` → «ruft nichts auf, wenn eine Karte in dieselbe Zelle oder ins Backlog zurückgelegt wird (US-13)» |
| 7 Hinweis in der Bestätigung | `@Unit` «Bestätigung beim Entfernen eines Teams weist auf zurückgehende Features hin»; Vitest `teams.component.spec.ts` → «weist in der Bestätigung darauf hin, dass die eingeplanten Features in allen PIs ins Backlog zurückgehen (US-6)» |
| 8 Ausserhalb einer Ablage losgelassen | `@Unit` «Karte über der IP-Iteration losgelassen bleibt an ihrem Ort», «Karte ausserhalb einer Ablage losgelassen bleibt an ihrem Ort»; Vitest `program-board.component.spec.ts` → «lässt eine Karte, die über der IP-Iteration losgelassen wird, an ihrem Ort und meldet die Sperre der IP (US-13)», «meldet die Sperre der IP auch, wenn die Karte zuletzt nur über ihrer eigenen Zelle war (US-13)», «erkennt die IP auch, wenn der Ablagepunkt auf einem Element in der IP-Zelle liegt (US-13)», «lässt eine Karte, die neben dem Raster losgelassen wird, im Backlog, ohne Meldung (US-13)», «nimmt eine eingeplante Karte nicht zurück, die zuletzt über dem Backlog war und ausserhalb einer Ablage losgelassen wird (US-13)», «meldet nichts, wenn die Karte im Raster, aber nicht über der IP losgelassen wird (US-13)»; `board.service.spec.ts` → «meldet die Sperre der IP-Iteration, ohne den Server zu fragen und ohne das Board zu ändern (US-13)», «ersetzt eine frühere Meldung durch die Sperre der IP-Iteration (US-13)» |

## Änderungen

- #13: Feature einplanen — `spec/changes/13-feature-einplanen/delta.md`
- #50: Nacharbeiten aus dem Playwright-Test — `spec/changes/50-nacharbeiten-playwright/delta.md`
