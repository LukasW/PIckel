---
name: flow-plan
description: Analysiert ein Issue gegen Spezifikation und Code und schreibt plan.md und das Delta in den Change-Ordner, ohne Code zu ändern. Einsetzen vor grösseren Features oder wenn die Umsetzung Abwägungen braucht.
compatibility: Claude Code im Repository PIkel. Braucht git, gh (GitHub), den Maven Wrapper und die Subagenten unter .claude/agents; Projektangaben in .claude/flow.md.
argument-hint: <issue-nummer>
disable-model-invocation: true
model: opus
---

Analysiere Issue #$ARGUMENTS und erstelle einen Plan.

Ablauf-Verzeichnis: `.claude/flow` (Fragmente unter `shared/`, Skripte unter `scripts/`), relativ zum Repository-Root.

## 1. Projekt und Provider

Lies und befolge `.claude/flow/shared/project.md` und `.claude/flow/shared/git-provider.md`.

## 2. Issue und Change-Ordner lesen

Das Issue ist der Auftrag. Massgebend ist der Change-Ordner `spec/changes/<nr>-*/`: Delta-Entwurf aus
`/flow-task`, `entscheide.md` aus `/flow-grill-me` (verbindlich), ein früherer Plan. Fehlt der Ordner, legst du ihn
in Schritt 5 an. Steht im Issue etwas, das dem Change-Ordner widerspricht oder dort fehlt: als offene Frage
aufnehmen, nicht still übernehmen (`shared/project.md` → «Das Repository führt»).

## 3. Spezifikation abgleichen

- Architektur-Doku und ADRs (`flow.md` → «Spezifikation»): Welche Bausteine, Abläufe und Konzepte sind betroffen?
  Widerspricht das Issue einem Nicht-Ziel? Grosse Dateien nie ganz lesen: Kapitel mit Grep (`^#+ `) finden und
  nur diese Abschnitte mit `offset`/`limit` lesen
- Die User Story zum Issue, falls vorhanden (Ort laut `flow.md` → «User Stories»; in PIkel ist die Story-Nummer
  die Issue-Nummer), und thematisch nahe Stories
- Verlangt der Auftrag eine Änderung an der bestehenden Spezifikation (Architektur-Doku, ADR, Story), gehört sie
  als Aufgabe in den Plan bzw. ins Delta — nach Rückfrage, nie still aus dem Issue übernommen
- Mit `deltas:` im Kopf von `flow.md`: Welche Akzeptanzkriterien und Szenarien der Story sind neu, welche ändern
  sich, welche fallen weg? Das wird das Delta (Schritt 6)

## 4. Codebase erkunden

- Welche Typen, Schnittstellen, Adapter, Templates, Migrationen sind betroffen?
- Gibt es ähnliche bestehende Lösungen als Vorlage, auch in den Repos aus `flow.md` → «Spezifikation»?
- Welche Tests existieren bereits für die betroffenen Bereiche?
- Welche Regeln (`CLAUDE.md`, `.claude/rules/`, `flow.md` → «Regeln beim Implementieren») sind berührt? Die
  passenden lesen

## 5. Plan als Datei speichern

`spec/changes/<nr>-<slug>/plan.md` (Change-Ordner, `shared/story-delta.md` → «Ort»). Er kommt mit dem PR in Git:
Nach dem Merge ist nachvollziehbar, was geplant war und warum.

Struktur (Markdown-Tasks `- [ ]` für alle Implementierungsschritte):

```markdown
# US-<us-nr> — <Titel> (Issue #<issue-nr>)

## Übersicht
<kurze Zusammenfassung>

## Zu erstellende Dateien
- [ ] `pfad/Datei` — Zweck

## Zu ändernde Dateien
- [ ] `pfad/Existing` — Art der Änderung

## Delta
`spec/changes/<nr>-<slug>/delta.md` — Neu: …, Geändert: …, Entfernt: …

<!-- ohne Delta stattdessen genau diese Zeile, sie gilt für check-story-drift.sh als Begründung: -->
Delta: keines — <Grund: Refactoring / Build / Bugfix bei korrekter Story / …>

## Architektur-Entscheide
- Schicht und Modul
- Neue Schnittstellen, Tabellen, Konfiguration
- Neue ADR nötig? Abschnitte der Architektur-Doku, die nachzuziehen sind

## Aufgaben
Atomar, in Umsetzungsreihenfolge, jede mit einem Erledigt-Kriterium, das sich prüfen lässt (am besten ein Test):
- [ ] 1. <Aufgabe> — erledigt, wenn <Test/Prüfung>
- [ ] 2. …

## Test-Strategie
Verhalten und Invarianten, die die Tests belegen, nicht Testklassen; daraus schreibt `test-author` die Tests
(`/flow-implement` 6b). Je Punkt die Ebene nach `flow.md` → «Tests».
- [ ] Unit: «Ein zweiter Lauf von … ändert nichts», Grenzfälle mit Werten
- [ ] Integration / Adapter: …
- [ ] Akzeptanz (US-<us-nr>): je Akzeptanzkriterium das beobachtbare Ergebnis
- [ ] Bestehende Tests, die sich wegen geänderter Spezifikation ändern (mit Eintrag im Delta, sonst Stelle in der
  Story)
- [ ] Prüfung von Hand nötig? (was laut `flow.md` kein Test prüft)

## Offene Fragen
- …
```

## 5b. Plan selbst prüfen

Bevor er vorgelegt wird, vier Fragen an den Plan; die Antworten gehören in die Zusammenfassung:

- **Architektur-Fit:** Passt jede Änderung in die Schicht aus `CLAUDE.md`? Würde `ArchitectureTest` etwas melden?
- **Echte Abwägungen:** Wo gibt es zwei gangbare Wege? Den gewählten mit Grund nennen, nicht verschweigen
- **Reihenfolge:** Baut jede Aufgabe nur auf früheren auf? Ist nach jeder Aufgabe der Build grün?
- **Lücken:** Was sagt die Spezifikation nicht? Zeigt der Plan eine Lücke in der Story, zuerst Story bzw. Delta
  nachführen oder als offene Frage stellen, nicht im Plan still entscheiden

**Zu grosse Aufgaben teilen:** mehr als ein «und» im Erledigt-Kriterium, oder mehr als zwei, drei Dateien über
mehrere Schichten. Ein Issue mit mehr als etwa acht Aufgaben oder einem Diff, der kaum in einem Review zu lesen
ist: dem Nutzer vorschlagen, es in mehrere Issues zu teilen.

## 6. Delta der Story (nur mit `deltas:`)

Ist das Issue eine Story oder ändert es Verhalten, das eine Story beschreibt
(`.claude/flow/shared/story-delta.md` → «Wann»): das Delta unter `<deltas>/<issue-nr>-<slug>/delta.md`
schreiben, im Format aus dem Fragment, `Status: entwurf`. Die Entscheide aus `entscheide.md`, die das Delta
prägen, kommen unter «Entscheide». Gibt es das Delta schon (Entwurf aus `/flow-task`, früherer Plan), nachführen
statt neu anlegen. Szenario-Titel so wählen, wie sie in der `.feature`-Datei stehen werden.

Die Story-Datei selbst ändert der Plan nicht; das Einpflegen gehört zu `/flow-implement`.

## 7. Präsentieren und warten

Zeige Datei-Pfad + Zusammenfassung. **Implementiere nichts vor Bestätigung.** Gibt es ein Delta, zeig es zuerst,
mit allen Einträgen unter «Geändert» und «Entfernt» im Wortlaut: Das ist die Prüfung der Spezifikation, und sie
kommt vor dem Wie.
Bei Korrekturen: `plan.md` und Delta anpassen, erneut vorlegen. Erst nach «OK»/«ja»/«go»: im Delta
`Status: bestätigt`, dann `/flow-implement <nr>`.

Dabei empfehlen, vorher `/clear` einzugeben: Alles, was `/flow-implement` braucht, steht im Change-Ordner, und eine frische Session liest nicht bei jedem Turn die ganze Erkundung mit.
