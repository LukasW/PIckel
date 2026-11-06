# Fragment: Definition of Done

Vor jedem Commit abprüfen, zusammen mit den Punkten aus `flow.md` → «Definition of Done».

## Tests
- [ ] Tests für neue oder geänderte Logik auf der tiefstmöglichen Ebene vorhanden und grün
  (`.claude/flow/shared/test-requirements.md`, Ebenen laut `flow.md` → «Tests»)
- [ ] Neues Verhalten: Tests aus der Spezifikation, geschrieben von `test-author` vor der Implementierung
- [ ] Kein bestehender Test abgeschwächt, abgeschaltet oder gelöscht
  (`.claude/flow/scripts/check-test-edit.sh --all`), oder jede Meldung im Handoff begründet
- [ ] Was kein Test prüft (laut `flow.md`, etwa Qualität von Modellantworten): im PR vermerkt, Prüfung von Hand
  angekündigt

## Akzeptanz (nur User Stories)
- [ ] Jedes Akzeptanzkriterium hat mindestens einen Test mit der Kennzeichnung laut `flow.md` → «User Stories»
- [ ] Jedes Szenario der Story ist durch einen Test belegt oder mit Begründung als nicht automatisierbar vermerkt

## Dokumentation
- [ ] Story-Datei vollständig und aktuell, Verweis auf die Tests eingetragen, Aufgaben abgehakt, Status nachgeführt
- [ ] Change-Ordner `spec/changes/<nr>-<slug>/` im Commit: Delta (eingepflegt, Story und `.feature` nachgeführt)
  oder `plan.md` mit `Delta: keines — <Grund>`, dazu Plan und Notizen (`.claude/flow/shared/story-delta.md`)
- [ ] `.claude/flow/scripts/check-story-drift.sh` ohne Meldung — CI-Gate, keine Begründung im Handoff ersetzt es
- [ ] Architektur-Doku nachgezogen, wenn sich Bausteine, Schnittstellen, Datenmodell oder Betrieb ändern; neuer
  Entscheid als ADR
- [ ] README, `CLAUDE.md` und Regeln passen zum neuen Stand

## Code-Qualität
- [ ] Keine toten Code-Pfade, keine auskommentierten Stellen, keine TODOs ohne Issue-Referenz
- [ ] Keine offenen Meldungen der Edit-Hooks
- [ ] Passende Reviewer liefen (`flow.md` → «Reviewer»), bei geänderten Tests auch `test-reviewer`
