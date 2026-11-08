# language: de
Funktionalität: US-15 Milestones auf dem Board

  Als RTE
  möchte ich Milestones mit Name und Datum erfassen und als Marker über den Iterationen sehen
  damit die Teams Termine wie Releases, Messen oder Audits bei der Planung berücksichtigen

  # JUnit: domain/model/MilestoneTest → createKeepsNameDateAndProgramIncrementAndAssignsId
  # JUnit: domain/model/ProgramIncrementTest → iterationOfFindsIterationWhosePeriodContainsTheDate
  # JUnit: domain/model/ProgramBoardTest → milestoneStandsOverIterationContainingItsDate
  # JUnit: application/service/BoardServiceTest → milestoneOnBoardStandsOverIterationOfItsDate
  Szenario: Milestone wird erfasst und über der passenden Iteration angezeigt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Wenn ich im PI "PI 27.1" den Milestone "Release 4.0" am "2027-02-10" erfasse
    Dann liegt der Milestone "Release 4.0" auf dem Board über "Iteration 3"

  # JUnit: domain/model/MilestoneTest → createRejectsDateOfScenarioAfterProgramIncrement
  # JUnit: application/service/MilestoneServiceTest → addRejectsDateOutsideProgramIncrementAndSavesNothing
  Szenario: Milestone ausserhalb des PI wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Wenn ich im PI "PI 27.1" den Milestone "Release 4.0" am "2027-04-01" erfasse
    Dann wird das Erfassen mit der Meldung "Das Datum muss im Zeitraum des PI liegen." abgelehnt

  # JUnit: domain/model/MilestoneTest → createRejectsMissingOrBlankName
  Szenario: Milestone ohne Namen wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Wenn ich im PI "PI 27.1" den Milestone " " am "2027-02-10" erfasse
    Dann wird das Erfassen mit der Meldung "Der Name darf nicht leer sein." abgelehnt

  # JUnit: domain/model/MilestoneTest → updateChangesNameAndDateAndKeepsIdAndProgramIncrement
  # JUnit: application/service/MilestoneServiceTest → updateSavesNewNameAndDateUnderSameIdAndReturnsIt
  Szenario: Milestone wird verschoben
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und im PI "PI 27.1" der Milestone "Release 4.0" am "2027-02-10"
    Wenn ich beim Milestone "Release 4.0" das Datum auf "2027-01-05" setze
    Dann liegt der Milestone "Release 4.0" auf dem Board über "Iteration 1"

  # JUnit: application/service/MilestoneServiceTest → deleteRemovesMilestone
  Szenario: Milestone wird gelöscht
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und im PI "PI 27.1" der Milestone "Release 4.0" am "2027-02-10"
    Wenn ich den Milestone "Release 4.0" lösche
    Dann hat das Board von "PI 27.1" keine Milestones

  Szenario: Unbekannter Milestone wird nicht gefunden
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Wenn ich im PI "PI 27.1" einen unbekannten Milestone lösche
    Dann antwortet der Server mit 404 und der Meldung "Den Milestone gibt es nicht."
