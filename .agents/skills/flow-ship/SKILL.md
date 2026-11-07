---
name: flow-ship
description: Schliesst ein Issue ab — DoD, Spec-Drift und CI prüfen, PR squash-mergen, aufräumen, Lernfragen. Einsetzen nach /flow-implement, wenn der PR freigegeben ist.
compatibility: Claude Code im Repository PIkel. Braucht git, gh (GitHub), den Maven Wrapper und die Subagenten unter .claude/agents; Projektangaben in .claude/flow.md.
argument-hint: "[--nachtlauf]"
disable-model-invocation: true
model: sonnet
---

Schliesse die aktuelle Arbeit ab (nach `/flow-implement`).

**Mit `--nachtlauf`** (Aufruf durch `scripts/night-batch.sh`, niemand antwortet): Lies zuerst
`.claude/flow/shared/nachtlauf.md`, Abschnitt «In `/flow-ship`». Statt zu fragen wird abgebrochen,
Aufräumen entfällt, «Lernen» geht als Kommentar ans Issue.

Ablauf-Verzeichnis: `.claude/flow` (Fragmente unter `shared/`, Skripte unter `scripts/`), relativ zum Repository-Root.

## 1. Projekt und Provider

Lies und befolge `.claude/flow/shared/project.md` und `.claude/flow/shared/git-provider.md`.
Hauptzweig: `base-branch` aus dem Kopf von `flow.md`, sonst `main`.

## 2. Branch & Push

- Branch-Namen merken: `git branch --show-current`
- `git status` — bei uncommitteten Änderungen: abbrechen und den Nutzer fragen
- `git push`, falls noch nicht geschehen

## 3. DoD & CI

- DoD erfüllt? Abgleichen mit `.claude/flow/shared/dod-checklist.md`
- Drift: `.claude/flow/scripts/check-story-drift.sh` muss ohne Meldung durchlaufen; der CI-Job «Spec-Drift» ist
  dasselbe Gate. Meldet er etwas: nicht mergen, sondern nachholen (Delta einpflegen, Feature-Datei nachziehen,
  `Delta: keines — <Grund>` im Plan) oder melden. Das Label `spec-unchanged` setzt nur der Nutzer, nie du. Kamen im
  Review Änderungen am Verhalten dazu, gehören sie vor dem Merge ins Delta und in die Story
- Change-Ordner `spec/changes/<nr>-*/` ist im PR enthalten (Delta bzw. Plan, Notizen)
- CI-Status des PR prüfen — **warten, bis alle Checks grün sind**
- Bei Fehlschlag: abbrechen, den Nutzer informieren, nicht mergen
- **CI läuft nicht** (Jobs ohne ausgeführten Schritt abgebrochen, etwa weil das Actions-Kontingent aufgebraucht ist,
  oder nach einigen Minuten noch keine Checks): das sagen und `scripts/ci-local.sh` starten (`flow.md` → «Ohne
  GitHub Actions»). Es prüft lokal dasselbe und meldet die Pflicht-Checks als Commit-Status; danach wie bei grüner CI
  weiter. Einen Status nie selbst setzen, eine rote CI nie lokal überstimmen
- PR trägt das Label `needs-human-review` (Risiko-Pfad: Schema, Pipeline, Agenten-Konfiguration, Abhängigkeiten):
  die betroffenen Dateien nennen und erst nach ausdrücklicher Freigabe im Chat mergen. Ein früheres «ok» zum Diff
  zählt nicht, weil die Freigabe genau diesem Risiko gelten soll

## 4. PR mergen

- PR zum aktuellen Branch finden
- **Squash merge** — ein sauberer Commit pro Feature oder Task auf dem Hauptzweig
- Titel des Squash-Commits mit der Issue-Nummer, `<typ>(<scope>): <Beschreibung> (#<issue-nr>)`, Body mit
  `Closes #<issue-nr>` (GitHub: `gh pr merge --squash --subject … --body …`). Daran erkennen `night-batch.sh` und jede spätere Suche, dass das Issue auf dem Hauptzweig liegt

## 5. Issue schliessen

- Issue-Nummer aus dem Branch-Namen (`feature/<NR>-…` oder `hotfix/<NR>-…`)
- Nicht automatisch durch `Closes #N` geschlossen: Issue auf `closed` setzen
- Story: `**Status:**` in der Story-Datei steht auf «Umgesetzt» (`flow.md` → «User Stories»; gehört in den PR;
  fehlt es, vor dem Merge nachholen)

## 6. Lokal aufräumen

**Worktree?** `git rev-parse --show-toplevel` mit dem Haupt-Repository vergleichen.

- **Ja (Worktree):**
  ```bash
  cd $(git worktree list --porcelain | head -1 | cut -d' ' -f2)
  git worktree remove <worktree-pfad>
  git branch -D <branch-name>
  git worktree prune
  ```
- **Nein:**
  ```bash
  git checkout <hauptzweig> && git pull
  git branch -d <branch-name>
  ```

## 7. Lernen

Fünf Minuten, damit der Harness mit jedem Change besser wird statt zu verrotten. Quellen: Issue, Plan, PR samt
Review-Kommentaren, die Meldungen von Reviewern und Hooks, die Rückfragen und Fehlversuche dieser Arbeit, dazu die
Implementierungsnotizen (`notes.md` im Change-Ordner) und die verworfenen Reviewer-Befunde.

1. Hat die Spezifikation die Anforderung erfasst, oder kam im Review Wesentliches erst dazu? Mit Delta: Stand im
   Delta vor dem Code schon, was am Ende eingepflegt wurde?
2. Hat der Plan die Trade-offs vorweggenommen?
3. Waren die Done-Checks kalibriert: eindeutig prüfbar, weder trivial noch unerfüllbar?
4. Gab es ein Muster (derselbe Fehler zum zweiten Mal, dieselbe Rückfrage, derselbe Reviewer-Befund)? Dann
   zuerst an eine deterministische Prüfung denken (ArchUnit-Regel, Hook, CI-Schritt), erst danach an eine Regel, einen
   Skill-Schritt oder eine Angabe in `flow.md`: Jede weitere Textregel senkt die Treue zu allen anderen
5. Was sollte als Nächstes spezifiziert werden?

Zeig die Antworten knapp und **schlag** Änderungen vor, jede mit Ziel und Begründung: Story oder Spezifikation,
`CLAUDE.md`, `.claude/rules/`, `flow.md`, Reviewer-Agent, oder eine Änderung am Ablauf unter `.claude/flow/` bzw.
`.agents/skills/flow-*`. Nichts davon ohne
Freigabe ändern; der Nutzer entscheidet.

**Nach der Freigabe** steht die Session schon auf dem Hauptzweig (Schritt 6). Deshalb:

- Änderungen an Doku und Regeln (Story, Spezifikation, `CLAUDE.md`, `.claude/rules/`, `flow.md`, Reviewer-Agent)
  gehen über einen eigenen Branch `chore/<slug>` ab `origin/<hauptzweig>` mit PR, **nie direkt auf den
  Hauptzweig**. Mehrere freigegebene Kleinigkeiten dürfen in einen PR
- Punkte mit eigenem Umfang (Code, neue Prüfung, mehr als eine Regel) werden als Issue angelegt, wie in
  `/flow-task`, statt sofort umgesetzt
- Änderungen am Ablauf (`.claude/flow/`, `.agents/skills/`, `.claude/agents/`, `.claude/rules/`) gehen wie Doku
  und Regeln über einen eigenen `chore/<slug>`-PR mit Change-Ordner `spec/changes/chore-<slug>/`
- Danach Schritt 8, mit den neuen PR- und Issue-Nummern

**Regeln kuratieren:** Eine neue Regel in Textform ist vorläufig. Schlag zugleich vor, wie sie deterministisch
wird (Linter, Architekturtest, Hook) oder wann sie wieder gestrichen wird. Eine Regel, die ein Werkzeug prüfen
kann, gehört nicht in den Kontext des Modells. Hat sich nichts gezeigt, genügt ein Satz.

## 8. Bestätigung

Ausgeben: Issue-Nummer, PR-Nummer, Merge-Status, Branch-Löschung, dazu den Hinweis aus `flow.md` → «Abschluss»
(etwa: Was es braucht, bis die Änderung live ist). Nichts davon selbst auslösen.

Zum Schluss `/clear` empfehlen: Das nächste Issue beginnt in einer frischen Session. Im Nachtlauf stattdessen die
Zeile `NACHTLAUF: FERTIG` (oder `NACHTLAUF: ABGEBROCHEN — <grund>`).
