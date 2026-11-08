# US-15: Milestones auf dem Board

**ID:** US-15
**Issue:** [#15](https://github.com/LukasW/PIckel/issues/15)
**Status:** Umgesetzt
**Priorität:** Mittel
**Schätzung:** 3

## Beschreibung

Als RTE möchte ich Milestones mit Name und Datum erfassen und als Marker über den Iterationen sehen, damit die
Teams Termine wie Releases, Messen oder Audits bei der Planung berücksichtigen.

## Akzeptanzkriterien

1. Ein Milestone hat einen Namen (Pflicht, getrimmt, höchstens 100 Zeichen) und ein Datum im Zeitraum des PI
2. Ein Datum ausserhalb des PI wird mit einer verständlichen Meldung abgelehnt
3. Milestones lassen sich ändern und nach Bestätigung löschen
4. Das Board zeigt jeden Milestone als Marker mit Name und Datum (TT.MM.) über der Iteration, in die sein Datum
   fällt

Ein Milestone gehört zum PI (ADR 0005). Erfasst, geändert und gelöscht wird er auf der Detailseite des PI, angezeigt
auf dem Board. Nicht in dieser Story: Milestones einem Team oder Feature zuordnen, wiederkehrende Termine.

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/15-milestones.feature`](../../src/test/resources/features/program-increments/15-milestones.feature).

## Tests

| Kriterium | Test |
|---|---|
| 1 Name Pflicht, getrimmt, ≤ 100; Datum im PI | `MilestoneTest` → `createKeepsNameDateAndProgramIncrementAndAssignsId`, `createTrimsName`, `createRejectsMissingOrBlankName`, `createAcceptsNameOfExactly100Characters`, `createRejectsNameOf101Characters`, `createRejectsMissingDate`; `MilestoneServiceTest` → `addSavesMilestoneWithTrimmedNameInProgramIncrementAndReturnsIt`, `addRejectsBlankNameAndSavesNothing`; CucumberIT «Milestone wird erfasst und über der passenden Iteration angezeigt», «Milestone ohne Namen wird abgelehnt»; Vitest `milestones.component.spec.ts` → «erfasst einen Milestone mit Name und Datum und zeigt ihn in der Liste (US-15)», «schickt einen Namen aus Leerzeichen nicht ab (US-15)», «schickt einen Milestone ohne Datum nicht ab (US-15)»; `program-increment-detail.component.spec.ts` → «bindet den Abschnitt «Milestones» des geöffneten PI auf der Detailseite ein (US-15)» |
| 2 Datum ausserhalb des PI abgelehnt | `MilestoneTest` → `createAcceptsStartDateOfProgramIncrement`, `createAcceptsEndDateOfProgramIncrement`, `createRejectsDayBeforeStartOfProgramIncrement`, `createRejectsDayAfterEndOfProgramIncrement`; `MilestoneServiceTest` → `addRejectsDateOutsideProgramIncrementAndSavesNothing`; CucumberIT «Milestone ausserhalb des PI wird abgelehnt»; Vitest `milestones.component.spec.ts` → «zeigt die Meldung des Servers als Alarm, wenn das Datum ausserhalb des PI liegt (US-15)» |
| 3 Ändern, Löschen nach Bestätigung | `MilestoneTest` → `updateChangesNameAndDateAndKeepsIdAndProgramIncrement`, `updateRejectsDayAfterEndOfProgramIncrement`; `MilestoneServiceTest` → `updateSavesNewNameAndDateUnderSameIdAndReturnsIt`, `updateRejectsMilestoneOfAnotherProgramIncrement`, `deleteRemovesMilestone`, `deleteRejectsMilestoneOfAnotherProgramIncrement`; CucumberIT «Milestone wird verschoben», «Milestone wird gelöscht», «Unbekannter Milestone wird nicht gefunden»; Vitest `milestones.component.spec.ts` → «verschiebt einen Milestone auf ein neues Datum und zeigt es (US-15)», «löscht einen Milestone erst nach der Bestätigung und zeigt dann den Leerzustand (US-15)», «behält den Milestone, wenn das Löschen abgebrochen wird (US-15)»; `milestones.service.spec.ts` |
| 4 Marker mit Name und TT.MM. über der Iteration | `ProgramIncrementTest` → `iterationOfFindsIterationWhosePeriodContainsTheDate`, `iterationOfRejectsDateOutsideProgramIncrement`; `ProgramBoardTest` → `milestoneStandsOverIterationContainingItsDate`; `BoardServiceTest` → `boardShowsMilestonesOfProgramIncrementInOrderOfUseCase`, `milestoneOnBoardStandsOverIterationOfItsDate`; CucumberIT «Milestone wird erfasst …», «Milestone wird verschoben»; Vitest `program-board.component.spec.ts` → «zeigt einen Milestone als Marker mit Name und Datum TT.MM. im Kopf seiner Iteration (US-15)», «stellt jeden Marker über die Iteration mit seiner Nummer, auch über die IP (US-15)», «zeigt mehrere Milestones einer Iteration alle, in der Reihenfolge des Servers (US-15)» |

## Änderungen

- #15: Milestones auf dem Board — `spec/changes/15-milestones/delta.md`
