---
# Schalter für die Hooks unter .claude/flow/scripts/
spelling: swiss
guard-env: true
test-guard: true
base-branch: main
# Story-Drift: check-story-drift.sh meldet Code-Änderungen ohne Story oder Delta
stories: spec/user-stories
deltas: spec/changes
---

# Ablauf für PIkel

Projektangaben für `/flow-task`, `/flow-plan`, `/flow-implement`, `/flow-ship`, `/flow-fast`, `/flow-grill-me`,
`/flow-fix-build`, `/flow-night-batch` und die Agenten. Architektur, Testpyramide und Konventionen stehen in
`CLAUDE.md` und `.claude/rules/`, der Ablauf selbst in `.claude/skills/flow-*` und `.claude/flow/`. Jede Regel steht
an genau einem Ort; hier nur, was der Ablauf zusätzlich braucht.

## Repository

- Issues und PRs: GitHub (`github.com/LukasW/PIkel`) über `gh`. Workflows unter `.github/workflows/`
- Label `spec-unchanged` setzt nur ein Mensch: übersteuert den CI-Job «Spec-Drift» (der Agent nie)
- Label `needs-human-review` setzt CI bei Risiko-Pfaden (Migrationen, `.github/`, `.claude/`, `CLAUDE.md`,
  Abhängigkeiten); solche PRs merged `/flow-ship` nur nach ausdrücklicher Freigabe, der Nachtlauf nie
- Labels: `enhancement` (User Story), `bug`, `maintenance` (Task); bei Bedarf `refactor`, `security`, `test`,
  `documentation`
- Nutzer: Lukas

## Spezifikation

- Fachlich: `spec/fachliche-spezifikation.md`, Stories unter `spec/user-stories/`
- Technisch: `spec/technische-spezifikation.md`
- Entscheide: `docs/adr/NNNN-<slug>.md`
- Schnittstelle: JAX-RS-Resources unter `adapter/in/rest/`, OpenAPI zur Laufzeit unter `/q/openapi`
- **Change-Ordner je Issue:** `spec/changes/<nr>-<slug>/` mit `delta.md`, `entscheide.md`, `plan.md`, `notes.md`
  (`.claude/flow/shared/story-delta.md` → «Ort»). Alles davon kommt mit dem Code in denselben PR
- **Das Repository führt, das Issue verweist** (`.claude/flow/shared/project.md`): Akzeptanzkriterien, Szenarien,
  Entscheide und Plan stehen nur im Change-Ordner und in der Story, nie im Issue

## User Stories

**Die Story-Nummer ist die Issue-Nummer.** US-1 (Walking Skeleton) stammt aus der Zeit vor GitHub.

- Titel `US-<issue-nr>: …`. Issue mit Platzhalter `US: …` anlegen, Nummer lesen, Titel auf `US-<nr>: …` ändern.
  Nie vorab raten
- Datei `spec/user-stories/<nr>-<slug>.md` mit H1 `US-<nr>: …`, `**ID:**`, `**Issue:**`, `**Status:**`
  (`Entwurf` → `In Arbeit` → `Umgesetzt`), `**Priorität:**`, `**Schätzung:**`, danach Beschreibung,
  Akzeptanzkriterien, BDD-Szenarien (Verweis auf die `.feature`-Datei), technische Notizen, Aufgaben, Änderungen
- BDD-Szenarien gleichlautend in `src/test/resources/features/<bereich>/<nr>-<slug>.feature` (`# language: de`,
  Titel `US-<nr> <Kurztitel>`); die Story verweist darauf
- Vitest-Testnamen tragen das Suffix `(US-<nr>)`; Java-Methoden kein Suffix, der Bezug steht im Feature als
  `# JUnit: <Pfad>/<Klasse> → <methode>`
- Betroffene Schichten im Issue: Domain / Application / Adapter / Frontend

## Build

Ein Maven-Modul mit Backend und Frontend (Quinoa). Ausgabe immer in eine Datei, z. B.
`> "${TMPDIR:-/tmp}/pikel-build.log" 2>&1; echo "exit $?"` — sonst füllt das Maven-Log den Kontext.

**Voraussetzung Frontend:** Node aus `.nvmrc` und npm 11 (`packageManager` in `package.json`). Nach einem frischen
Klon einmal `cd src/main/webapp && npm ci`.

- **Formatieren:** `./mvnw -B -q spotless:apply` und `cd src/main/webapp && npm run format`
- **Im Loop** (Backend): `./mvnw test -B -DskipFrontendTests=true`, eine Klasse mit `-Dtest='<Klasse>'`;
  (Frontend): `cd src/main/webapp && npm test`
- **Schnell** (wie der Unit-Job in CI): `scripts/gate.sh --fast` — Prettier, Spotless, JUnit inkl. ArchUnit, Vitest,
  PIT
- **Integration:** `./mvnw verify -B -DskipFrontendTests=true` — `CucumberIT` und `SmokeIT` gegen
  Dev-Services-PostgreSQL. Braucht Podman oder Docker
- **Abschluss ohne Fail-Fast:** `scripts/gate.sh` — alles oben in einer Kette; muss vor jedem Push grün sein
- **Mutation allein:** `./mvnw test-compile -B -Pmutation`, Bericht unter `target/pit-reports/index.html`.
  Überlebende Mutanten heissen: Ein Test fehlt, nicht die Schwelle senken
- **Ausgabe auswerten:**
  `grep -E '\[ERROR\]|FAIL|Tests run:.*(Failures|Errors): [1-9]|Test Files|below threshold|Violation|BUILD' "${TMPDIR:-/tmp}/pikel-build.log" | head -60`

## Tests

Ebenen und Tag-Routing: `CLAUDE.md` → «Testpyramide». Orte:

- Unit Backend: `src/test/java/**/*Test.java`, Ports mit Mockito gemockt
- Unit Frontend: `src/main/webapp/src/app/**/*.spec.ts`, Mock-Repositories
- Integration: Steps unter `src/test/java/**/cucumber/steps/`, Features unter `src/test/resources/features/`
- `@Pending` ist **kein Abschlusszustand**; bei Tag-Routing und Gherkin hilft der Agent `bdd-cucumber-author`

## Regeln beim Implementieren

`CLAUDE.md` → «Wichtigste Regeln» und die passende Datei unter `.claude/rules/`. Zusätzlich für den Ablauf:

- Überraschungen, ungeplante Entscheidungen und Annahmen laufend in die Notizen-Datei zum Plan schreiben, nicht erst
  im Handoff — sie überleben `/clear` und sind das Futter für «Lernen» in `/flow-ship`

## Reviewer

- Java unter `src/main/java/ch/grudligstrasse/pikel/**` → `hexagonal-reviewer`
- Angular unter `src/main/webapp/src/app/**` → `angular-signals-reviewer`
- `.feature`-Dateien → `bdd-cucumber-author`
- Tests unter `src/test/**` oder `*.spec.ts` → `test-reviewer`
- Jede Story und jeder Bugfix → `spec-reviewer` (Diff gegen die Akzeptanzkriterien, frischer Kontext)

Reviewer-Befunde nicht blind anwenden: Jeder Befund wird geprüft, übernommen oder mit Grund verworfen. Nach zwei
Runden ohne Einigung entscheidet der Nutzer.

Der Hook `.claude/hooks/reviewer-reminder.sh` nennt die Agenten nach jedem Edit.

## Definition of Done

- [ ] `.feature`-Datei vorhanden, gleichlautend mit der Story, jedes Szenario grün oder als `@Unit` belegt,
  kein `@Pending`
- [ ] Story: Verweis auf die `.feature`-Datei, Aufgaben abgehakt, Status nachgeführt
- [ ] Delta eingepflegt, `check-story-drift.sh` ohne Meldung (CI-Gate, eine Begründung ersetzt es nicht)
- [ ] `scripts/gate.sh` grün (Format, Unit, ArchUnit, Mutation, Integration, Smoke)

## Rückfragen

Plan auch in `/flow-fast` vorlegen bei: mehr als 3 Akzeptanzkriterien, mehr als einer Schicht, Liquibase-Migration,
Auth/Security oder Breaking Changes der REST-API.

Typische offene Äste für `/flow-grill-me` und `/flow-task`:

- **Fachlich (SAFe):** ART, Teams, PI, Iterationen, Features, Stories, Abhängigkeiten, PI-Ziele, Kapazität —
  welche Begriffe braucht die Story wirklich?
- **Schichten:** Frontend, Backend, beides? Welche Hexagon-Schicht?
- **Datenmodell:** neue Entität oder Feld? Migration?
- **Berechtigung:** wer darf was (RTE, Product Owner, Team)? Auth ist noch nicht eingebaut
- **UX:** Leerzustände, Fehlermeldungen, Mobile

## Anhalten

Zusätzlich zu den Fällen aus `.claude/flow/shared/stop-conditions.md` immer fragen, bevor es weitergeht, wenn die Änderung:

- eine Liquibase-Migration bringt, die bestehende Daten umschreibt oder löscht
- Anmeldung, Sessions oder Rollen einführt oder ändert

## Nachtlauf

Angaben für `/flow-night-batch` und `.claude/flow/scripts/night-batch.sh`. Nacht-PRs sind Vorschläge, keine Merges.

- Vorbedingung: `podman info >/dev/null 2>&1 || docker info >/dev/null 2>&1`
- Warm-up: `./mvnw -B -q dependency:go-offline && (cd src/main/webapp && npm ci)`
- Pool: 1 (Issues einer Welle nacheinander; mehr nur ohne gemeinsame Migrationen und Konfiguration)
- Merge: nein (die PRs bleiben zum Review am Morgen offen)
- Nie im Nachtlauf: Liquibase-Migrationen, die Daten ändern; Auth, Sessions, Rollen; `.github/`, `.claude/`,
  `pom.xml`, `package.json`; Issues, die nicht du selbst angelegt hast
- Rechte: nur `.claude/settings.json` (`--permission-mode acceptEdits`), Kostendeckel `FLOW_MAX_BUDGET_USD` je Phase

## Abschluss

Ein Merge auf `main` lässt `ci-main.yml` laufen (Unit, Integration, Smoke). Ein Deployment gibt es noch nicht.

## Geheimnisse

- `.env` nie lesen, nie durchsuchen (`grep -r --exclude='.env*'`)
