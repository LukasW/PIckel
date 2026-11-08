# US-12: Program Board eines PI anzeigen

**ID:** US-12
**Issue:** [#12](https://github.com/LukasW/PIkel/issues/12)
**Status:** Umgesetzt
**Priorität:** Hoch
**Schätzung:** 5

## Beschreibung

Als Teilnehmerin oder Teilnehmer des PI Planning möchte ich das Program Board eines PI als Raster sehen, damit die
Klebezettel-Wand durch eine gemeinsame Ansicht ersetzt wird, auf der Einplanen, Kapazität und Abhängigkeiten
aufbauen.

## Akzeptanzkriterien

1. Das Board eines PI zeigt ein Raster mit einer Zeile pro Team (alphabetisch) und einer Spalte pro Iteration
   (zeitlich), die IP-Iteration sichtbar abgesetzt
2. Spaltenköpfe zeigen Name und Zeitraum der Iteration (TT.MM.)
3. Eine Backlog-Spalte zeigt die nicht eingeplanten Features des PI als Karten mit Titel, WSJF-Rang und Schätzung,
   sortiert wie das Backlog
4. Ohne Teams zeigt das Board einen Hinweis mit Link zur Teamseite statt eines leeren Rasters
5. Das Board ist von der Detailseite des PI aus erreichbar

Nur Anzeige: Einplanen per Drag & Drop folgt mit #13. Das Raster scrollt waagrecht in seinem eigenen Container;
Mobile ist für das Board kein Ziel in M1.

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/12-program-board-anzeigen.feature`](../../src/test/resources/features/program-increments/12-program-board-anzeigen.feature).

## Tests

| Kriterium | Test |
|---|---|
| 1 Raster: Teams alphabetisch, Iterationen zeitlich, IP abgesetzt | `BoardServiceTest` → `rowsAreTheTeamsInAlphabeticalOrder`, `columnsAreTheIterationsInTimeOrderWithIpLast`; CucumberIT «Board liefert Teams, Iterationen und Backlog»; Vitest `program-board.component.spec.ts` → «zeigt eine Zeile je Team …», «zeigt eine Spalte je Iteration …», «markiert die Spalte der IP-Iteration als IP-Iteration (US-12)» |
| 2 Spaltenköpfe Name und Zeitraum TT.MM. | Vitest `program-board.component.spec.ts` → «zeigt im Spaltenkopf Name und Zeitraum der Iteration als TT.MM. (US-12)» |
| 3 Backlog-Spalte mit Karten in Backlog-Reihenfolge | `BoardServiceTest` → `backlogKeepsBacklogOrder`, `backlogLeavesOutScheduledFeaturesAndKeepsOrderOfTheRest`, `backlogIsEmptyWhenEveryFeatureIsScheduled`; CucumberIT «Board liefert Teams, Iterationen und Backlog»; Vitest → «zeigt das Backlog als Karten …», «übernimmt die Reihenfolge des Backlogs vom Server (US-12)», «zeigt die Backlog-Spalte links neben dem Raster (US-12)» |
| 4 Ohne Teams Hinweis mit Link | `BoardServiceTest` → `boardWithoutTeamsHasNoRowsButColumnsAndBacklog`; Vitest → «zeigt ohne Teams einen Hinweis mit Link zur Teamseite statt eines Rasters (US-12)», «zeigt mit Teams keinen Hinweis, sondern das Raster (US-12)» |
| 5 Von der Detailseite erreichbar | Vitest `program-increment-detail.component.spec.ts` → «verlinkt auf das Program Board des geöffneten PI (US-12)»; `program-board.component.spec.ts` → «öffnet das Board über die Route program-increments/:id/board (US-12)» |
| Unbekanntes PI → 404 | `BoardServiceTest` → `getRejectsUnknownProgramIncrement`; CucumberIT «Board eines unbekannten PI»; Vitest `board.service.spec.ts` |

## Änderungen

- #12: Program Board eines PI anzeigen — `spec/changes/12-program-board-anzeigen/delta.md`
