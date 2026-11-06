---
name: flow-fast
description: Führt /flow-task, /flow-plan und /flow-implement in einem Durchlauf aus, mit Duplikat-Prüfung und Plan-Vorlage nur bei heiklen Änderungen. Einsetzen für kleine, klar umrissene Änderungen.
argument-hint: <beschreibung>
disable-model-invocation: true
model: sonnet
---

Führe die folgenden Schritte nacheinander aus.

**Eingabe:** $ARGUMENTS

Ablauf-Verzeichnis: `.claude/flow`. Projektangaben: `.claude/flow/shared/project.md`.

## Schritt 0: Duplikat-Prüfung

1. Codebase durchsuchen (Grep, Glob) nach Hinweisen, dass das Feature schon existiert
2. Offene und kürzlich geschlossene Issues prüfen
3. Stories und Spezifikation (Orte laut `flow.md`) auf eine passende Story prüfen

**Falls Duplikat:** klare Meldung (Datei/Klasse oder Issue-Link), sofort abbrechen. **Kein neues Issue.**

## Schritt 1: Issue erstellen

Führe `/flow-task $ARGUMENTS` aus. Merke die Issue-Nummer.

Bei echten Unklarheiten (mehrdeutige Anforderungen, unklar ob Story oder Task): nachfragen, auf Antwort warten.

## Schritt 2: Plan erstellen

Führe `/flow-plan <issue-nummer>` aus.

## Schritt 3: Plan-Bestätigung — nur wenn nötig

Normalerweise überspringt `/flow-fast` die Plan-Bestätigung. **Ausnahme — den Plan vorlegen und auf Bestätigung
warten**, wenn mindestens eins davon zutrifft:

- Story mit **mehr als 3 Akzeptanzkriterien**
- Plan berührt **mehr als eine Schicht**
- Plan enthält **offene Fragen** oder mehrere valide Lösungsansätze
- Plan widerspricht einer ADR oder verlangt eine neue
- Das Delta der Story hat Einträge unter **Geändert** oder **Entfernt** (`shared/story-delta.md`)
- `plan.md` hat **mehr als 30 Zeilen** nach dem ersten Entwurf
- Einer der heiklen Bereiche aus `flow.md` → «Rückfragen» ist berührt

Sonst direkt weiter zu Schritt 4.

## Schritt 4: Implementieren

Führe `/flow-implement <issue-nummer>` aus. Die Pflicht-Pause vor dem Commit (Schritt 12 dort) gilt auch hier.

## Regeln

- Automatischer Durchlauf — bei echten Unklarheiten interaktiv nachfragen
- Fehler in einem Schritt: melden und abbrechen, nicht weitermachen
- Duplikat erkannt = sofortiger Abbruch
