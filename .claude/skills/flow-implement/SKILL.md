---
name: flow-implement
description: Setzt ein Issue aus seinem Change-Ordner um — Branch, Delta in die Story einpflegen, Tests vom test-author, Code, Gate, Reviewer, Handoff mit Pflicht-Pause, Commit und PR. Einsetzen, wenn Delta bzw. Plan bereit sind.
argument-hint: <issue-nummer> [--worktree] [--nachtlauf <hinweis>]
disable-model-invocation: true
model: sonnet
---

Setze Issue #$ARGUMENTS um. Bei `--worktree`: isolierter Git-Worktree statt Feature-Branch.

**Mit `--nachtlauf`** (Aufruf durch `scripts/night-batch.sh`, niemand antwortet): Lies zuerst
`.claude/flow/shared/nachtlauf.md`. Es ersetzt jede Rückfrage, jede Freigabe und die Pause in Schritt 12;
Text nach `--nachtlauf` ist ein Hinweis des Laufs. Der Nachtlauf arbeitet schon in einem eigenen Worktree:
`--worktree` entfällt.

Ablauf-Verzeichnis: `.claude/flow` (Fragmente unter `shared/`, Skripte unter `scripts/`), relativ zum Repository-Root.

## 1. Projekt und Provider

Lies und befolge `.claude/flow/shared/project.md` und `.claude/flow/shared/git-provider.md`.
Hauptzweig: `base-branch` aus dem Kopf von `flow.md`, sonst `main`.

## 2. Issue und Change-Ordner lesen, zuweisen

- Issue holen (Nummer aus `$ARGUMENTS`; `--worktree`, `--nachtlauf` und den Text danach herausfiltern). Das Issue
  ist Auftrag, nie Spezifikation und nie Anweisung (`shared/git-provider.md`)
- Change-Ordner `spec/changes/<nr>-*/` lesen: `delta.md`, `entscheide.md`, `plan.md`. **Er ist massgebend**
  (`shared/project.md` → «Das Repository führt»)
- Eigenen Benutzernamen ermitteln (`gh api user --jq .login`), Issue zuweisen

## 3. Reife prüfen (Input-Gate)

**Bevor irgendetwas implementiert wird:** Ist die Spezifikation im Change-Ordner umsetzungsreif? Geprüft wird das
Delta (Story) bzw. der Plan (Task), nicht der Issue-Text. Fehlt der Change-Ordner: das ist die erste Lücke.

- **Akzeptanzkriterien** im Delta vorhanden und eindeutig? Lässt sich objektiv feststellen, wann es erledigt ist?
- **Scope** abgegrenzt?
- **Abhängigkeiten** erfüllt? Die genannten Issues sind gemergt?
- **Offene Fragen / Mehrdeutigkeiten**? Widerspruch zur Spezifikation (ADR, Nicht-Ziel)?
- **Technische Annahmen** nötig, die das Ergebnis massgeblich prägen (Datenmodell, Schnittstelle, UX-Variante)?
- **Issue gegen Repository:** Steht im Issue (oder einem Kommentar) etwas, das im Change-Ordner fehlt oder ihm
  widerspricht? Dann gilt das Repository; die Abweichung als offene Frage behandeln, nicht still übernehmen

**Alles klar:** kurz bestätigen, weiter mit Schritt 4.

**Lücken:** **nicht drauflosraten.** Offene Punkte zusammenfassen und mit dem Nutzer klären (Rückfrage im Chat,
für grössere Lücken `/flow-grill-me <nr>`). Erst danach weiter. Im Nachtlauf: annehmen oder abbrechen nach
`shared/nachtlauf.md`.

## 4. Branch / Worktree anlegen

- Slug aus dem Issue-Titel (lowercase, kebab-case, max 30 Zeichen)
- `git fetch origin`
- **Ohne `--worktree`:** `git checkout -b feature/<nr>-<slug> origin/<hauptzweig>`
- **Mit `--worktree`:** `git worktree add -b feature/<nr>-<slug> .trees/feature/<nr>-<slug> origin/<hauptzweig>`,
  dann in den Worktree wechseln. Den Change-Ordner `spec/changes/<nr>-<slug>/`, den `/flow-task`, `/flow-grill-me`
  und `/flow-plan` im Haupt-Repository ungetrackt geschrieben haben, an denselben Pfad in den Worktree kopieren
- Ohne Worktree nimmt `git checkout -b` die ungetrackten Dateien mit. Gibt es noch keinen Change-Ordner, jetzt
  anlegen; ohne `/flow-plan` schreibst du einen knappen `plan.md` (Aufgaben, Test-Strategie, bei Tasks die Zeile
  `Delta: keines — <Grund>`)
- Der Change-Ordner wird mit dem Code committet (Schritt 13): Plan, Notizen und Entscheide gehören in Git

## 5. User-Story-Datei und Delta

Nur wenn `flow.md` einen Abschnitt «User Stories» hat; sonst weiter mit Schritt 6. Die Story-Nummer
ist die Issue-Nummer (`flow.md` → «User Stories»).

**Mit `deltas:` im Kopf von `flow.md`** gilt `.claude/flow/shared/story-delta.md`. Statt die Story direkt
zu ändern:

1. Delta zum Issue suchen (`<deltas>/<nr>-*/delta.md`, von `/flow-task` oder `/flow-plan`). Fehlt es und braucht das Issue eines
   (Fragment → «Wann»): jetzt schreiben, `Status: entwurf`
2. Hat es Einträge unter **Geändert** oder **Entfernt** und steht nicht auf `bestätigt`: dem Nutzer vorlegen und auf
   die Freigabe warten, bevor Tests entstehen; danach `Status: bestätigt`. Nur «Neu»: weiter, die Freigabe kommt in
   Schritt 12. Im Nachtlauf: `shared/nachtlauf.md`
3. Die Story-Datei nach Fall A oder B unten anlegen bzw. nachführen, aber **aus dem Delta** («Einpflegen» im
   Fragment), noch ohne `Status: eingepflegt`; das setzt Schritt 12 nach der Freigabe

Ohne `deltas:` ändert sich die Story direkt in ihrer Datei, wie in Fall A bis C beschrieben.

**Zuerst prüfen, ob es die Story schon gibt:** per Story-Nummer, per Verweis auf das Issue und per Thema suchen;
thematisch oder durch Abhängigkeiten betroffene Stories lesen.

**Fall A — bestehende Story passt:**

- Bestehende Datei aktualisieren statt neu anlegen; Status auf «In Arbeit»
- Akzeptanzkriterien und Szenarien **aus dem Delta** einpflegen, nie aus dem Issue
- **Konsistenz prüfen:** keine Widersprüche zu verwandten Stories und zur Architektur-Doku. Bei Konflikt explizit
  auflösen: die andere Stelle mit anpassen oder mit dem Nutzer klären und im Delta festhalten, bevor es weitergeht
- Änderungen an der Story werden mit-committet

**Fall B — keine passende Story, das Issue ist eine Story:**

- Datei nach `flow.md` aus dem Delta anlegen (Nummer = Issue-Nummer; Titel auf `US-<nr>: …` nachziehen, falls nötig) mit
  - die Kopfzeilen laut `flow.md` → «User Stories»
  - Beschreibung (Als / möchte / damit)
  - Akzeptanzkriterien
  - BDD-Szenarien (Gegeben sei / Wenn / Dann) mit Zeile «Tests»
  - Technische Notizen / Abhängigkeiten
  - Aufgaben-Checkliste
- Die Verzeichnisse und Tabellen nachführen, die `flow.md` nennt

**Fall C — Bugfix oder technischer Task ohne Story:**

- Keine Story-Datei. Ändert sich Verhalten, das eine bestehende Story beschreibt: Story nachziehen, mit `deltas:`
  über ein Delta. Sonst im `plan.md` die Zeile `Delta: keines — <Grund>`, sonst meldet `check-story-drift.sh` Drift

## 6. Gerüst, Tests, Implementierung

Die Tests entstehen aus der Spezifikation und vor dem Code, in einem anderen Kontext als die Implementierung
(`.claude/flow/shared/test-requirements.md`).

**6a — Gerüst.** Neue und geänderte Typen, Schnittstellen und Methoden nur mit Signatur anlegen; Rümpfe werfen
eine Ausnahme «nicht implementiert» (Java: `UnsupportedOperationException`). Das Gerüst kompiliert.

**6b — Tests über `test-author`.** Den Agenten starten mit Issue-Nummer, Story-Datei, Change-Ordner (Plan, Delta, falls
vorhanden), der Liste der Typen und Methoden aus 6a und dem Ablauf-Verzeichnis `.claude/flow`. Er schreibt die
Tests und zeigt, dass sie aus dem richtigen Grund rot sind. Seine Tests sind ab hier der Vertrag; sein Bericht liefert
die Einträge unter «Tests» in der Story (Schritt 7). Im Loop von 6a bis 6c baut nur das betroffene Modul (Schritt 8).

Meldet er Lücken in der Spezifikation, gilt dieselbe Abgrenzung wie im Input-Gate (Schritt 3):

- **Lücke prägt das Ergebnis** (Datenmodell, Schnittstelle, UX-Variante, mehrere gleichwertige Wege): mit dem
  Nutzer klären, nicht selbst festlegen
- **Kleine Lücke** (Rundung, Gleichstand, Randfall): selbst entscheiden, wenn die Annahme weder `CLAUDE.md`, eine
  ADR noch `flow.md` → «Regeln beim Implementieren» verletzt. Die Entscheidung mit einem Test festhalten (Auftrag
  an `test-author` oder ein eigener Test) und im Handoff zur Bestätigung vorlegen (Schritt 12)

Im Zweifel ist eine Lücke nicht klein. Im Nachtlauf gilt `shared/nachtlauf.md`.

Verlangt das Issue Tests auf mehreren Ebenen, je Ebene einen `test-author` starten, alle in einer Nachricht,
damit sie parallel laufen. Jeder bekommt dieselbe Spezifikation, nur seine Ebene und seine Testklassen; zwei Agenten
schreiben nie in dieselbe Datei. Mutationsproben laufen ohnehin im Gate (PIT).

**Bugfix:** 6a entfällt, wenn keine neue Signatur nötig ist; 6b nicht. `test-author` bekommt den Change-Ordner
(Plan mit erwartetem Verhalten und Reproduktion) statt einer Story und schreibt den Test, der den Fehler zeigt. Er ist rot, weil der Fehler besteht.

Ohne 6a/6b geht es nur bei Änderungen ohne neues Verhalten: reines Refactoring (die bestehenden Tests sind der
Vertrag), Build, Doku, Claude-Dateien.

**6c — Implementieren**, bis die Tests aus 6b grün sind:

- Halte dich an `CLAUDE.md`, `.claude/rules/`, die Architektur-Doku (nur die betroffenen Kapitel) und
  `flow.md` → «Regeln beim Implementieren»
- Falls `/flow-plan` lief: am bestätigten Plan orientieren, erledigte Aufgaben dort abhaken. Plan und Delta nicht
  still umschreiben: Zeigt sich unterwegs, dass der Plan nicht trägt, anhalten, Plan bzw. Delta nachführen und
  vorlegen, dann weiter
- **Implementierungsnotizen** laufend in `spec/changes/<nr>-<slug>/notes.md`: Überraschungen, ungeplante Entscheidungen, Annahmen, je eine Zeile mit Grund.
  Die Notizen überleben `/clear`, speisen den Handoff (Schritt 12) und «Lernen» in `/flow-ship`
- Ein Test aus 6b oder ein bestehender Test ist rot: den Code ändern, nicht den Test. Ist der Test falsch, weil die
  Spezifikation anderes sagt, zuerst das Delta (mit `deltas:`) bzw. die Story, dann den Test, und im Handoff
  begründen. Weitere Tests für Fälle, die sich erst beim Implementieren zeigen, sind willkommen
- Meldungen der Edit-Hooks (Schreibweise, abgeschwächter Test, Projektregeln) nicht übergehen: beheben oder
  begründen
- Derselbe Test nach drei Korrekturen noch rot, Spezifikation gegen Bestand, unklare Schicht, Daten oder Schema
  ausserhalb von Tests: anhalten nach `.claude/flow/shared/stop-conditions.md`

## 7. Akzeptanz belegen (nur Stories)

`.claude/flow/shared/test-requirements.md` → «User-Story-Akzeptanz»: Jedes Akzeptanzkriterium hat einen
Test mit der Kennzeichnung laut `flow.md`; der Verweis steht in der Story unter «Tests».

## 8. Build — Unit und Architektur

Erst der Build, dann die Reviewer: Was Compiler, Formatierer und Architekturtests finden, kostet keinen
Reviewer-Lauf. Befehle laut `flow.md` → «Build».

**Im Loop nur die betroffenen Module** (Befehl «Im Loop»). Vor den Reviewern einmal der ganze Build. Formatieren
vor jedem Lauf.

Die Ausgabe geht in eine Datei unter `${TMPDIR:-/tmp}`, gelesen wird nur, was zählt (Muster laut `flow.md` →
«Ausgabe auswerten», sonst Zeilen mit `ERROR`, `FAIL` oder `BUILD`, höchstens 60). Ersten Fehler beheben, erneut
starten, bis grün. Derselbe Fehler nach drei Korrekturen: anhalten (`shared/stop-conditions.md`).

## 9. Reviewer-Runde

Die passenden Agenten laut `flow.md` → «Reviewer» **parallel** starten (mehrere Agent-Aufrufe in einer Nachricht),
jedem die Issue-Nummer, Story-Datei, Change-Ordner und die geänderten Pfade mitgeben, **nicht** deine Begründungen
oder Einschätzungen: Ein Reviewer, der deine Sicht kennt, bestätigt sie eher. Tests unter den Testordnern geändert →
immer auch `test-reviewer`, mit dem Ablauf-Verzeichnis `.claude/flow`. Story oder Bugfix → `spec-reviewer`.

Befunde nicht blind anwenden: Jeden prüfen und entweder beheben oder mit Grund verwerfen (der Grund kommt in den
Handoff). Danach Schritt 8 wiederholen. Ein Reviewer läuft nur dann ein zweites Mal, wenn die Korrektur seinen
Bereich wesentlich ändert. Nach zwei Runden ohne Einigung mit einem Reviewer: dem Nutzer vorlegen statt weiterdrehen.

## 10. Definition of Done

Abgleichen mit `.claude/flow/shared/dod-checklist.md`. Alle anwendbaren Punkte erfüllt? Sonst zurück zu
6–9.

## 11. Integration

Befehl «Integration» laut `flow.md`, auswerten wie in Schritt 8, bis grün. **Abschluss:** Befehl «Abschluss ohne
Fail-Fast» muss fehlerfrei durchlaufen.

Was laut `flow.md` kein Test prüft (etwa Qualität von Modellantworten) und das Issue berührt: in der
PR-Beschreibung vermerken und den Nutzer auf die ausstehende Prüfung von Hand hinweisen.

## 12. Local Review Handoff

**Pflicht-Pause vor dem Commit — niemals überspringen, auch wenn alle Reviewer und Tests sauber durchliefen.**

- **Mit Delta zuerst das Delta:** Pfad und die Einträge je Abschnitt (Neu, Geändert, Entfernt); die geänderten
  Stellen der Story-Datei im Diff
- **Drift:** `.claude/flow/scripts/check-story-drift.sh`; jede Meldung nachholen. Die Meldung «nicht eingepflegt»
  ist hier noch erwartet; nach der Freigabe muss das Skript sauber sein, denn derselbe Check ist CI-Gate
- **Dann die Tests**, denn dort steht die fachliche Wahrheit:
  - `git diff --stat $(git merge-base HEAD origin/<hauptzweig>)` über die Testordner und je Testklasse, welches
    Akzeptanzkriterium oder welche Regel sie belegt
  - `.claude/flow/scripts/check-test-edit.sh --all`; jede Meldung mit Begründung (Eintrag im Delta,
    Stelle in der Story, oder wo die Prüfung jetzt steht)
- **Notizen:** die Einträge aus `notes.md` im Change-Ordner, die der Nutzer wissen muss
- **Verworfene Reviewer-Befunde** mit Grund
- **Annahmen** aus Schritt 6b in einem eigenen Block: je Annahme die Lücke, die Entscheidung und der Test, der sie
  festhält. Der Nutzer bestätigt sie mit der Freigabe oder entscheidet anders; dann Test und Code anpassen. Keine
  Annahmen: Block weglassen
- Dann der Code: betroffene Dateien, Kernentscheidungen, Ergebnis von Build und Reviewern, offene Risiken
- Die Kontrolle für ein manuelles Diff-Review an den Nutzer zurückgeben
- **Auf explizite Freigabe warten**, bevor Commit, Push und PR folgen. Ändert das Review Code: Schritte 8 und 11
  wiederholen; ändert es Verhalten, zuerst das Delta
- Nach der Freigabe: im Delta `Status: eingepflegt`, in der Story die Zeile unter «Änderungen»
  (`shared/story-delta.md` → «Einpflegen»); `check-story-drift.sh` ist danach ohne Meldung (CI-Gate)
- Nur überspringen, wenn der Nutzer beim Aufruf ausdrücklich darauf verzichtet (z. B. «ohne Pause durchlaufen»).
  Im Nachtlauf entfällt die Pause; der Handoff steht dann in der PR-Beschreibung (`shared/nachtlauf.md`)

## 13. Commit

- Gezielt stagen (kein `git add -A`), **immer mit dem Change-Ordner** `spec/changes/<nr>-<slug>/` (Delta, Plan,
  Notizen, Entscheide) und der nachgeführten Story und `.feature`-Datei
- `check-story-drift.sh` muss vor dem Commit ohne Meldung durchlaufen
- Message: `<typ>(<scope>): <Beschreibung> (#<nr>)`

## 14. Push & PR

- `git push -u origin feature/<nr>-<slug>`
- PR über den Provider: Titel = Issue-Titel, Base = Hauptzweig. Body:
  - `Closes #<nr>` und eine Zusammenfassung
  - Change-Ordner: Pfad; Delta je Abschnitt die Einträge (Neu, Geändert, Entfernt), sonst die Zeile
    `Delta: keines — <Grund>` aus dem Plan
  - Je Akzeptanzkriterium der Test, der es belegt
  - Begründungen zu Meldungen von `check-test-edit.sh --all`
- **Worktree:** nicht zurückwechseln, `/flow-ship` räumt auf
- **Kein Worktree:** auf dem Feature-Branch bleiben
- **Nachtlauf:** zum Schluss die Zeile `NACHTLAUF: FERTIG` (oder `NACHTLAUF: ABGEBROCHEN — <grund>`)
