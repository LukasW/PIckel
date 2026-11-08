# Notizen — Issue #13

- `@angular/cdk` ^22.2.6 in diesem PR statt per Vorab-`chore/`-PR (Entscheid des Nutzers); CI setzt `needs-human-review`
- Arbeit im Worktree `.trees/feature/13-feature-einplanen`, weil im Hauptverzeichnis uncommittete Änderungen von #26 lagen
- Kleine Lücken selbst entschieden (Annahmen, im Handoff): Hinweistext «Seine eingeplanten Features gehen in allen
  PIs zurück ins Backlog.»; optimistische Karte nach WSJF-Rang einsortiert; `actionError` wird bei jeder Aktion und
  beim Laden zurückgesetzt; Formular schliesst nach «Einplanen» und «Zurück ins Backlog», eingeplante Karte mit ihrer
  Zelle vorbelegt; Ablegen in dieselbe Liste ruft nichts auf; `unschedule` eines Backlog-Features speichert nicht;
  Prüfreihenfolge beim Einplanen: PI, Feature, Team (404), dann Domain (400)
- Drei `@Unit`-Szenarien (AK 4, 5, 7) fehlten im Delta; ins Delta und die Feature-Datei ergänzt, zur Bestätigung im
  Handoff
- «Einplanen» ist nicht `disabled`, sondern `aria-disabled`, und ohne vollständige Auswahl wirkungslos: Das Spec setzt
  die Auswahl und klickt ohne Zwischenrendern
- `TeamSteps` «eine leere Teamliste» löscht über `TeamUseCase.remove`, weil der Fremdschlüssel `feature.team_id` ohne
  `ON DELETE` ein direktes `deleteAll` verhindert
