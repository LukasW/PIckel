# Fragment: Nachtlauf

Gilt, wenn ein Skill mit `--nachtlauf` aufgerufen wurde. Dann startet ihn `scripts/night-batch.sh` über `claude -p`,
und **niemand antwortet**: keine Rückfrage, kein `AskUserQuestion`, keine Freigabe. Text nach `--nachtlauf` ist ein
Hinweis des Laufs (etwa, wo die Entscheide stehen) und gilt wie eine Angabe im Issue.

Alles andere aus dem Skill gilt unverändert: Input-Gate, Tests über `test-author`, Build, Reviewer, Definition
of Done. Ersetzt wird nur jede Stelle, an der der Skill fragt, wartet oder eine Freigabe verlangt.

## Entscheiden statt fragen

- Massgebend ist der Change-Ordner `spec/changes/<nr>-*/` auf dem Hauptzweig (Delta, `entscheide.md`, Plan), nicht
  der Issue-Text (`shared/project.md` → «Das Repository führt»)
- Fehlt eine Angabe, und eine vertretbare Annahme verletzt weder `CLAUDE.md`, eine ADR noch `flow.md` → «Regeln beim
  Implementieren»: annehmen und festhalten, in `notes.md` im Change-Ordner und in der PR-Beschreibung unter
  «Annahmen im Nachtlauf»
- Prägt die Annahme das Ergebnis massgeblich (Datenmodell, Schnittstelle, UX-Variante) oder gibt es mehrere
  gleichwertige Wege: nicht annehmen, sondern abbrechen (unten)
- **Delta der Story** (`shared/story-delta.md`): Einträge unter «Geändert» oder «Entfernt» brauchen ein Delta mit
  `Status: bestätigt` auf dem Hauptzweig oder im Branch eines früheren Versuchs. Fehlt es, oder müsste der Lauf
  solche Einträge erst schreiben: abbrechen, denn nachts bestätigt niemand eine Änderung bestehenden Verhaltens.
  Ein Delta nur mit «Neu» darf der Lauf schreiben; es steht dann in der PR-Beschreibung

## Pause vor dem Commit

Die Pflicht-Pause von `/flow-implement` (Schritt 12) entfällt. Der Handoff, den sonst der Nutzer im Chat liest,
steht vollständig in der PR-Beschreibung: zuerst das Delta, falls vorhanden, dann die Tests (welche Testklasse
welches Kriterium belegt, jede Meldung von `check-test-edit.sh --all` mit Begründung; `check-story-drift.sh` muss
ohne Meldung sein, sonst abbrechen),
dann Kernentscheidungen, Annahmen und offene Risiken. Das Review macht der Mensch am PR.

## Abbrechen statt anhalten

Wo `.claude/flow/shared/stop-conditions.md` oder `flow.md` → «Anhalten» ein Anhalten verlangt, wo das
Input-Gate Lücken findet, die keine vertretbare Annahme schliesst, oder wo das Issue eine Aktion verlangt, die
`flow.md` von Hand vorsieht:

1. Nichts weiter committen oder pushen; was schon auf dem Remote liegt, bleibt
2. Am Issue kommentieren, in der Form aus `stop-conditions.md`: Stand, Beobachtung, Hypothese, Frage mit Empfehlung.
   Der Kommentar ist die Rückfrage, die der Nutzer am Morgen beantwortet
3. Enden mit der Zeile `NACHTLAUF: ABGEBROCHEN — <Grund in einem Satz>`

## Ein früherer Versuch

Gibt es schon einen Branch `feature/<nr>-…` auf `origin` oder einen offenen PR zum Issue (ein abgebrochener Lauf):
dort weitermachen statt neu anfangen. `git checkout -B <branch> origin/<branch>`, Stand gegen Issue und Plan
prüfen, fertigstellen, denselben PR nachführen. Nie einen zweiten PR zum selben Issue.

## Fortlaufende Nummern

Andere Issues derselben Nacht landen vor diesem auf dem Hauptzweig. Unmittelbar vor dem Commit `git fetch origin`
und jede fortlaufende Nummer (Datenbank-Migration, ADR, Story-Nummer) gegen `origin/<hauptzweig>` neu prüfen.

## In `/flow-ship`

- Uncommittete Änderungen, rote CI, PR nicht mergebar oder Konflikt: nicht mergen, am PR und am Issue kommentieren,
  abbrechen wie oben
- CI läuft nicht: `scripts/ci-local.sh` wie in `/flow-ship`. Kann das Nacht-Token keinen Commit-Status melden, nicht
  mergen, am PR vermerken und mit `NACHTLAUF: ABGEBROCHEN — CI fehlt, lokal grün` enden
- PR mit Label `needs-human-review` (Risiko-Pfad, gesetzt von CI): nicht mergen, am PR vermerken und mit
  `NACHTLAUF: ABGEBROCHEN — Risiko-Pfad, Review nötig` enden
- Squash-Commit: Titel `<typ>(<scope>): <Beschreibung> (#<issue-nr>)` wie der Commit aus `/flow-implement`, Body
  mit `Closes #<issue-nr>`. Daran erkennt `night-batch.sh`, dass das Issue auf dem Hauptzweig liegt
- «Lokal aufräumen» entfällt; den Worktree räumt `night-batch.sh` weg
- «Lernen»: die Antworten und Vorschläge als Kommentar am PR statt im Chat; nichts davon umsetzen
- Kein `/clear` empfehlen

## Ende

Die letzte Zeile der Antwort ist immer eine von zwei:

- `NACHTLAUF: FERTIG`
- `NACHTLAUF: ABGEBROCHEN — <Grund in einem Satz>`

`night-batch.sh` liest sie aus dem Ergebnis von `claude -p`. Ein Abbruch wird nicht wiederholt, und `/flow-ship`
läuft danach nicht.
