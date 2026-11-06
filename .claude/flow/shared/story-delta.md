# Fragment: Change-Ordner und Delta-Stories

Gilt, wenn der Kopf von `flow.md` `deltas: <ordner>` nennt. Ohne den Schalter ändern die Skills eine Story direkt in
ihrer Datei.

Die Story-Datei beschreibt, was das System **heute** tut. Ein Delta beschreibt, was ein Issue daran **ändert**: neue,
geänderte und entfernte Akzeptanzkriterien und Szenarien, mit Grund. Das Delta wird vor dem Code geschrieben und
geprüft, dann im selben PR wie der Code **in die Story eingepflegt**: Danach steht der neue Stand in der Story, nicht
nur im Delta. Das Delta bleibt als Begründung im Repository liegen. So ist die Story immer aktuell, und die
Geschichte jeder Änderung steht in Git, nicht im Issue (`project.md` → «Das Repository führt»).

## Ort: ein Change-Ordner je Issue

`<deltas>/<issue-nr>-<slug>/` (Slug wie beim Branch: aus dem Issue-Titel, kebab-case, höchstens 30 Zeichen),
gefunden über die Issue-Nummer (`<deltas>/<issue-nr>-*/`). Alles, was zu einer Änderung gehört, liegt hier und geht
mit dem Code in denselben PR:

| Datei | Wer schreibt | Inhalt |
|---|---|---|
| `delta.md` | `/flow-task` (Entwurf), `/flow-plan`, `/flow-implement` | Änderung an der Story, nur wenn das Issue Verhalten einer Story betrifft (→ «Wann») |
| `entscheide.md` | `/flow-grill-me` | Geklärte Entscheidungen, Non-Goals, Annahmen aus dem Interview |
| `plan.md` | `/flow-plan`, sonst `/flow-implement` | Aufgaben, Architektur-Entscheide, Test-Strategie |
| `notes.md` | `/flow-implement` | Überraschungen, ungeplante Entscheidungen, Annahmen während der Umsetzung |

**Ohne Issue** (Chore direkt als `chore/<slug>`-PR, etwa Änderungen am Ablauf): `<deltas>/chore-<slug>/`.

**Nur Spezifikation vorab** (Change-Ordner per `chore/`-PR auf den Hauptzweig, etwa für den Nachtlauf): Ein PR ohne
Code darf ein Delta mit `Status: entwurf` oder `bestätigt` enthalten; eingepflegt wird es im PR mit dem Code.

Ein Delta gehört zu einer Story; ändert ein Issue mehrere Stories, je Story ein Abschnitt `# Delta US-<nr>` in
derselben `delta.md`, jeder mit eigener Zeile `Status:`. Der Ordner wird nach dem Merge nicht verschoben: Die
Issue-Nummer im Namen und `git log` geben die Reihenfolge.

Bis `/flow-implement` den Branch anlegt, liegen die Dateien ungetrackt im Arbeitsverzeichnis; `git checkout -b`
nimmt sie mit, für einen Worktree werden sie kopiert.

**Kein Delta:** Dann steht in `plan.md` eine Zeile `Delta: keines — <Grund>` (Refactoring, Build, Bugfix bei
korrekter Story …). `check-story-drift.sh` akzeptiert damit Code ohne Story-Änderung; ohne diese Zeile meldet es Drift.

## Wann

- Das Issue ist eine Story (neu oder bestehend), oder es ändert Verhalten, das eine bestehende Story beschreibt
  (auch ein Bugfix, wenn die Story das falsche Verhalten beschreibt)
- Kein Delta: Bugfix, bei dem der Code von der Story abweicht (die Story stimmt schon), Refactoring, Build, Doku,
  technische Tasks ohne fachliches Verhalten

## Neu, geändert oder entfernt

Massgebend ist das Verhalten, nicht der Wortlaut der Story:

- **Geändert:** Für eine Eingabe oder Lage, die die Story schon heute abdeckt, kommt ein anderes Ergebnis heraus,
  auch wenn es nur mehr ist (mehr Treffer, ein zusätzliches Feld, eine Meldung statt Stille). Gilt das bisherige
  Kriterium im Wortlaut weiter, ändert sich trotzdem, worauf sich Nutzer und Tests verlassen: Es gehört unter
  «Geändert», mit dem Kriterium oder Szenario, das es betrifft
- **Neu:** Etwas, das die Story bisher gar nicht beschreibt (eine neue Funktion, eine neue Eingabe, ein neuer
  Fehlerfall), und kein bestehender Fall ändert dabei sein Ergebnis
- **Entfernt:** Ein Kriterium oder Szenario gilt nicht mehr, ohne Ersatz oder durch ein anderes ersetzt

Im Zweifel «Geändert»: Ein Eintrag zu viel dort kostet eine Freigabe, einer zu wenig eine unbemerkte Änderung
bestehenden Verhaltens.

## Format

```markdown
# Delta US-<us-nr> — <Titel> (Issue #<issue-nr>)

Story: `<pfad der story-datei>` (neu | bestehend) · Status: entwurf | bestätigt | eingepflegt

## Warum

<ein, zwei Sätze: Anlass und Nutzen>

## Entscheide

- **<Aspekt>:** <Entscheid> (Quelle: `entscheide.md`, ADR, Plan)

## Neu

### AK: <prüfbare Aussage>

### Szenario: <Titel>
- **Gegeben sei** …
- **Wenn** …
- **Dann** …

## Geändert

### Szenario: <Titel, wie in der Story>
- **Vorher:** <bisheriger Wortlaut oder das Wesentliche>
- **Nachher:**
  - **Gegeben sei** …
  - **Wenn** …
  - **Dann** …
- **Grund:** <warum>
- **Tests:** <bestehende Tests, die sich deshalb ändern>

## Entfernt

### AK: <bisheriger Wortlaut>
- **Grund:** <warum>
- **Ersatz:** <was stattdessen gilt, oder «keiner»>
- **Tests:** <bestehende Tests, die deshalb wegfallen>
```

Leere Abschnitte bleiben mit «keine» stehen; ein Leser sieht so, dass nichts entfernt wurde. Bei einer neuen Story
steht alles unter «Neu». Unter «Entscheide» stehen die Entscheide aus `entscheide.md`, die das Delta prägen.

**Szenario-Titel sind Schlüssel:** Der Titel nach `### Szenario:` lautet wörtlich wie der Titel in der
`.feature`-Datei (`Szenario: <Titel>`). `check-story-drift.sh` prüft nach dem Einpflegen, dass jedes Szenario unter
«Neu» und «Geändert» in einer Feature-Datei steht und keines unter «Entfernt» mehr.

## Status

| Status | Gesetzt von | Bedeutung |
|---|---|---|
| `entwurf` | `/flow-plan`, `/flow-implement` | geschrieben, noch nicht vorgelegt |
| `bestätigt` | `/flow-plan` nach der Freigabe des Plans, `/flow-implement` nach der Freigabe des Deltas | Der Nutzer hat das Delta gesehen und freigegeben |
| `eingepflegt` | `/flow-implement` vor dem Commit | Die Story-Datei enthält den neuen Stand |

**Geändert** und **Entfernt** ändern Verhalten, auf das sich jemand verlässt. Ein Delta mit Einträgen dort braucht
`bestätigt`, bevor Tests geschrieben werden. Ein Delta nur mit «Neu» darf mit der Freigabe vor dem Commit
(`/flow-implement` Schritt 12) bestätigt werden.

## Einpflegen

1. Je Eintrag die Story-Datei nachführen: «Neu» anhängen, «Geändert» ersetzen, «Entfernt» streichen. Die Story trägt
   danach nur den neuen Stand, keine Vorher-Nachher-Angaben und keine Delta-Marker
2. In der Story unter «Änderungen» eine Zeile `- #<issue-nr>: <Titel> — <pfad des deltas>` anfügen (Abschnitt
   anlegen, wenn er fehlt)
3. Im Delta `Status: eingepflegt`
4. Story, Delta und Code gehen in denselben Commit. Mit Gherkin (`test-requirements.md` → «BDD mit Gherkin») auch
   die Feature-Datei: Ihre Szenarien lauten danach wie die der Story

Zeigt sich beim Umsetzen oder im Review etwas, das nicht im Delta steht: zuerst das Delta nachführen, dann Story,
Test und Code. Das Delta ist die Begründung für jede Änderung an bestehenden Tests; `check-test-edit.sh` meldet sie
trotzdem, die Begründung im Handoff verweist dann auf den Eintrag im Delta.

## Drift prüfen

`.claude/flow/scripts/check-story-drift.sh` vergleicht den Branch mit der Abzweigung vom Hauptzweig und meldet:

- Code oder Tests geändert, aber weder Story noch Delta, und kein `plan.md` mit `Delta: keines — <Grund>`
- Story geändert ohne Delta
- Delta geändert, aber die Story nicht (nicht eingepflegt)
- Delta nicht im Status `eingepflegt`
- Szenario aus «Neu» oder «Geändert» fehlt in den Feature-Dateien, oder eines aus «Entfernt» steht noch drin
- Story verweist auf eine Feature-Datei, die es nicht gibt

**Drift ist ein Gate, keine Empfehlung:** Der CI-Job «Spec-Drift» lässt den PR rot, bis die Meldung behoben ist.
Ein Mensch kann ihn mit dem Label `spec-unchanged` übersteuern; der Agent setzt dieses Label nie selbst.
