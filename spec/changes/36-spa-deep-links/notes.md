# Notizen — Issue #36

- `UriInfo.getAbsolutePathBuilder()` (Location-Header in `ProgramIncrementResource`, `MilestoneResource`) geht vom
  tatsächlichen Request-Pfad aus und enthält `quarkus.rest.path` deshalb schon; der Header bleibt `/api/v1/…` ohne
  Codeänderung. Belegt ist das für PIs (`ProgramIncrementSteps` → `verweistLocationAufDasNeuePi`); für Milestones
  prüft kein Test den Header, dort gilt dieselbe Begründung
- Der `SmokeIT`-Test deckt `/teams` und `/program-increments/<uuid>/board` ab; `/program-increments/<id>` läuft über
  denselben Weg (jeder Pfad ausserhalb von `/api` erreicht REST nicht mehr) und ist deshalb nicht eigens drin. Von Hand
  im Dev-Modus geprüft: alle drei Unterseiten 200 mit `index.html`, `GET /api/v1/teams` 200, kaputtes JSON 400 mit
  Fehlerkörper, unbekannter Pfad unter `/api` 404
- Testname ohne Suffix `_36` (Reviewer-Befund, `.claude/rules/java.md` → «Testmethoden ohne Story-Suffix»)
