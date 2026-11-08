---
name: flow-task
description: Legt aus einer Beschreibung ein Issue (nur Auftrag und Verweis) und bei einer Story den Delta-Entwurf mit Akzeptanzkriterien und Szenarien im Change-Ordner an. Einsetzen, wenn eine neue Idee, Story oder ein technischer Task erfasst werden soll.
compatibility: Claude Code oder Copilot CLI (in einem vertrauten Ordner, ADR 0007) im Repository PIkel. Braucht git, gh (GitHub) und den Maven Wrapper; Projektangaben in .claude/flow.md.
argument-hint: <beschreibung>
disable-model-invocation: true
model: opus
---

Erfasse **$ARGUMENTS** als Issue (Auftrag und Verweis) und, bei einer Story, als Delta-Entwurf im Repository.
Das Repository führt, das Issue verweist (`.claude/flow/shared/project.md`): Akzeptanzkriterien und Szenarien
stehen im Delta, nie im Issue.

Ablauf-Verzeichnis: `.claude/flow` (Fragmente unter `shared/`, Skripte unter `scripts/`), relativ zum Repository-Root.

## 1. Projekt und Provider

Lies und befolge `.claude/flow/shared/project.md` und `.claude/flow/shared/git-provider.md`.

## 2. Typ bestimmen

- **User Story** (Benutzer-Feature) → Pfad A
- **Technischer Task** (Refactoring, Tests, CI/CD, Abhängigkeiten, Betrieb, Tech Debt) → Pfad B

Referenziert die Beschreibung eine bestehende Story, lies deren Datei (Ort laut `flow.md` → «User Stories») und
verwende ihren Inhalt. Sonst lies den passenden Abschnitt der Architektur-Doku (`flow.md` → «Spezifikation»).

Ist der Typ nicht eindeutig → als offene Frage in Schritt 3 klären.

---

## 3. Offene Fragen klären (interaktiv, einzeln)

Prüfe die Beschreibung auf Lücken. Typische Kandidaten:

- **Typ-Zuordnung:** Pfad A oder B
- **Rolle** (Pfad A): Wer ist «Als …»?
- **Scope:** Welche Schicht, welches Modul, welche Phase?
- **Akzeptanzkriterien:** Fehlen messbare Erfolgsbedingungen?
- **Edge Cases:** die Bereiche aus `flow.md` → «Rückfragen», dazu Ausfall externer Systeme und Abbruch mittendrin
- **Widerspruch zur Spezifikation:** ADR, Nicht-Ziel oder Risiko berührt?
- **Labels / Priorität** strittig?
- **Abhängigkeiten:** Blockiert durch andere Issues?

**Regeln für das Nachfragen:**

- **Eine Frage pro Turn.** Nicht bündeln, erst die Antwort abwarten.
- **Immer mit Empfehlung + Pro/Contra.** Der Nutzer soll informiert entscheiden, nicht raten.
- Bevorzugt das Auswahl-Werkzeug des Agenten (in Claude Code `AskUserQuestion`), die empfohlene Option zuerst;
  sonst als Text:

  ```markdown
  **Frage:** <konkrete Frage>

  **Empfehlung:** <Option X> — <ein-Satz-Begründung>

  **Optionen:**
  - **<Option A>**
    - Pro: <stichpunkt>
    - Contra: <stichpunkt>
  - **<Option B>**
    - Pro: <stichpunkt>
    - Contra: <stichpunkt>
  ```

- Keine Fragen zu Dingen, die du per Lookup in Codebase, Spezifikation oder Issues selbst klären kannst.
- Nach allen Antworten → kurze Zusammenfassung der Annahmen, **dann** Issue und Delta-Entwurf erstellen.

---

## Pfad A: User Story

**Titel:** nach `flow.md` → «User Stories», sonst `US-<us-nr>: [Prägnanter Titel]` (max. 70 Zeichen).

> In PIkel **ist die Story-Nummer die Issue-Nummer.** Issue mit Platzhalter-Titel `US: …` anlegen, die vergebene
> Nummer lesen und den Titel auf `US-<nr>: …` ändern. Die Nummer nie vorab raten: Parallel angelegte Issues würden
> sonst dieselbe Nummer bekommen.

**Issue-Body** (nur Auftrag und Verweis):

```markdown
Als [Rolle] möchte ich [Ziel], damit [Nutzen].

**Priorität:** [Hoch / Mittel / Tief] · **Schichten:** [Domain / Application / Adapter / Frontend]
**Abhängig von:** [#YY oder «keine»]

**Spezifikation:** `spec/changes/<nr>-<slug>/delta.md` (massgebend; entsteht mit diesem Issue, kommt mit dem PR
auf `main`)
```

**Label:** Feature-Label laut `flow.md`.

**Delta-Entwurf:** Nach dem Anlegen (Nummer bekannt) `spec/changes/<nr>-<slug>/delta.md` im Format aus
`.claude/flow/shared/story-delta.md` schreiben, `Status: entwurf`. Darin, was das Interview ergeben hat:

- «Warum»: Zielbild und Nutzen
- «Neu» bzw. «Geändert»/«Entfernt» (bestehende Story): Akzeptanzkriterien als prüfbare Aussagen und Szenarien
  (Happy Path, Edge Case, Fehler) mit Titeln, die später wörtlich in der `.feature`-Datei stehen
- «Entscheide»: die Antworten aus Schritt 3 mit Begründung

Die Story-Datei pflegt `/flow-implement` aus dem Delta ein. Der Entwurf liegt bis zum Branch ungetrackt im
Arbeitsverzeichnis; `/flow-implement` nimmt ihn mit.

---

## Pfad B: Technischer Task

**Titel:** `Task: [Prägnanter Titel]` (max. 70 Zeichen).

**Body:**

```markdown
## Beschreibung
[Was ist das Problem / was soll verbessert werden?]

## Motivation
[Warum nötig? Welches technische Problem löst es?]

**Schichten/Module:** […] · **Abhängig von:** [#YY oder «keine»]

**Plan:** `spec/changes/<nr>-<slug>/plan.md` (entsteht mit `/flow-plan` oder `/flow-implement`)
```

Aufgaben und Umsetzung stehen im Plan, nicht im Issue. Ändert der Task Verhalten einer Story, ist er fachlich: dann
wie Pfad A ein Delta schreiben.

**Label:** Task- oder Bug-Label laut `flow.md`.

---

## 4. Ausgabe

- Issue-Nummer und, bei Pfad A, die Story-Nummer (z. B. `US-93` zu Issue `#93`)
- Issue-Link und Pfad des Delta-Entwurfs
- Nächster Schritt: `/flow-grill-me <nr>` bei offenen Punkten, sonst `/flow-plan <nr>`

## Regeln

- BDD-Szenarien logisch ableiten (Happy Path, Edge, Fehler); Gegeben sei / Und / Wenn / Dann / Und
- Akzeptanzkriterien sind prüfbare Aussagen, keine Wiederholung der Szenarien
- Akzeptanzkriterien und Szenarien nie ins Issue schreiben, nur ins Delta
- Sprache des Projekts (laut `CLAUDE.md`)
