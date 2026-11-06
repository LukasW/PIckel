---
# Schalter für die Hooks des Plugins flow (Tools/claude-flow)
spelling: swiss
guard-env: true
test-guard: true
base-branch: main
---

# Ablauf für PIkel

Projektangaben für `/flow:task`, `/flow:plan`, `/flow:implement`, `/flow:ship`, `/flow:fast`, `/flow:grill-me`,
`/flow:fix-build` und die Agenten `flow:test-author` und `flow:test-reviewer`. Architektur und Code-Konventionen
stehen in `CLAUDE.md`, der Ablauf selbst im Plugin.

## Repository

- Issues und PRs: GitHub (`github.com/LukasW/PIkel`) über `gh` bzw. `mcp__github__*`. Workflows unter
  `.github/workflows/`
- Labels: `enhancement` (User Story), `bug`, `maintenance` (Task); bei Bedarf `refactor`, `security`, `test`,
  `documentation`
- Nutzer: Lukas

## Spezifikation

- Fachlich: `spec/fachliche-spezifikation.md`, Stories unter `spec/user-stories/`
- Technisch: `spec/technische-spezifikation.md`
- Entscheide: `docs/adr/NNNN-<slug>.md`
- Schnittstelle: JAX-RS-Resources unter `adapter/in/rest/`, OpenAPI zur Laufzeit unter `/q/openapi`
- Pläne: `plan/` (in `.gitignore`, bleibt lokal)

## User Stories

**Die Story-Nummer ist die Issue-Nummer.** US-1 (Walking Skeleton) stammt aus der Zeit vor GitHub.

- Titel `US-<issue-nr>: …`. Issue mit Platzhalter `US: …` anlegen, Nummer lesen, Titel auf `US-<nr>: …` ändern.
  Nie vorab raten
- Datei `spec/user-stories/<nr>-<slug>.md` mit H1 `US-<nr>: …`, `**ID:**`, `**Status:**`, `**Priorität:**`,
  `**Schätzung:**`, danach Beschreibung, Akzeptanzkriterien, BDD-Szenarien, technische Notizen, Aufgaben
- BDD-Szenarien gleichlautend in `src/test/resources/features/<bereich>/<nr>-<slug>.feature` (`# language: de`,
  Titel `US-<nr> <Kurztitel>`); die Story verweist darauf
- Vitest-Testnamen tragen das Suffix `(US-<nr>)`; Java-Methoden kein Suffix, der Bezug steht im Feature als
  `# JUnit: <Pfad>/<Klasse> → <methode>`
- Betroffene Schichten im Issue: Domain / Application / Adapter / Frontend

## Build

Ein Maven-Modul mit Backend und Frontend (Quinoa). Ausgabe immer in eine Datei, z. B.
`> "${TMPDIR:-/tmp}/pikel-build.log" 2>&1; echo "exit $?"`.

**Voraussetzung Frontend:** Node aus `.nvmrc` und npm 11 (`packageManager` in `package.json`). Nach einem frischen
Klon einmal `cd src/main/webapp && npm ci`.

- Formatieren: `cd src/main/webapp && npm run format`; `npm run format:check` ist CI-Gate im PR
- Im Loop, nur Backend: `./mvnw test -B -DskipFrontendTests=true`; eine Klasse mit `-Dtest='<Klasse>'`
- Im Loop, nur Frontend: `cd src/main/webapp && npm test`
- Schnell (wie CI im PR): `./mvnw test -B` — JUnit und Vitest
- Abschluss: `./mvnw verify -B` — zusätzlich `CucumberIT` und `SmokeIT` gegen Dev-Services-PostgreSQL. Braucht
  Podman oder Docker
- Ausgabe auswerten:
  `grep -E '\[ERROR\]|FAIL|Tests run:.*(Failures|Errors): [1-9]|Test Files|BUILD' "${TMPDIR:-/tmp}/pikel-build.log" | head -60`

## Tests

| Ebene | Werkzeug | Wann | Ort |
|-------|----------|------|-----|
| Unit Backend | JUnit 5 + Mockito (Ports gemockt) | Domänenregeln, Use-Case-Services | `src/test/java/**/*Test.java` |
| Unit Frontend | Vitest (`ng test`), Mock-Repositories | Services (Signals), Komponenten (DOM) | `src/main/webapp/src/app/**/*.spec.ts` |
| Integration | `CucumberIT` (Cucumber JVM + REST-assured, Dev Services) | REST-Verträge, Persistenz | Steps unter `cucumber/steps/` |
| Smoke | `SmokeIT` (`@QuarkusIntegrationTest`) | gepacktes JAR bootet, UI wird ausgeliefert | `src/test/java/**/SmokeIT.java` |

- Unit ist der Default; nach oben nur, was darunter nicht beweisbar ist
- Tag-Routing: ohne Tag → `CucumberIT`; `@Unit` → JUnit/Vitest mit Verweis-Kommentar; `@E2E` → Browser-Runner
  (noch nicht eingerichtet, erst mit der ersten Story, die ihn braucht); `@Pending` → nicht umgesetzt, **kein
  Abschlusszustand**
- Neuer oder geänderter REST-Endpunkt oder Persistenz: `CucumberIT`-Szenario
- Bei Tag-Routing und Gherkin hilft der Agent `bdd-cucumber-author`

## Regeln beim Implementieren

- `CLAUDE.md` einhalten (hexagonal, Angular mit Signals, OnPush, `inject()`, `@if`/`@for`, Design-Tokens)
- Schemaänderung nur über eine neue Liquibase-Migration unter `src/main/resources/db/migration/` samt Eintrag in
  `db/changeLog.xml`; bestehende Migrationen nie ändern
- Texte der Oberfläche Deutsch mit echten Umlauten, Schweizer Schreibweise (`ss` statt `ß`), Du-Form;
  Bezeichner Englisch
- Frontend vor dem Commit formatieren (`npm run format`)

## Reviewer

- Java unter `src/main/java/ch/grudligstrasse/pikel/**` → `hexagonal-reviewer`
- Angular unter `src/main/webapp/src/app/**` → `angular-signals-reviewer`
- `.feature`-Dateien → `bdd-cucumber-author`
- Tests unter `src/test/**` oder `*.spec.ts` → `flow:test-reviewer`

Der Hook `.claude/hooks/reviewer-reminder.sh` nennt die Agenten nach jedem Edit.

## Definition of Done

- [ ] `.feature`-Datei vorhanden, gleichlautend mit der Story, jedes Szenario grün oder als `@Unit` belegt,
  kein `@Pending`
- [ ] Story: Verweis auf die `.feature`-Datei, Aufgaben abgehakt, Status nachgeführt
- [ ] `npm run format:check` grün
- [ ] `./mvnw verify -B` grün

## Rückfragen

Plan auch in `/flow:fast` vorlegen bei: mehr als 3 Akzeptanzkriterien, mehr als einer Schicht, Liquibase-Migration,
Auth/Security oder Breaking Changes der REST-API.

Typische offene Äste für `/flow:grill-me` und `/flow:task`:

- **Fachlich (SAFe):** ART, Teams, PI, Iterationen, Features, Stories, Abhängigkeiten, PI-Ziele, Kapazität —
  welche Begriffe braucht die Story wirklich?
- **Schichten:** Frontend, Backend, beides? Welche Hexagon-Schicht?
- **Datenmodell:** neue Entität oder Feld? Migration?
- **Berechtigung:** wer darf was (RTE, Product Owner, Team)? Auth ist noch nicht eingebaut
- **UX:** Leerzustände, Fehlermeldungen, Mobile

## Anhalten

Zusätzlich zu den Fällen des Plugins immer fragen, bevor es weitergeht, wenn die Änderung:

- eine Liquibase-Migration bringt, die bestehende Daten umschreibt oder löscht
- Anmeldung, Sessions oder Rollen einführt oder ändert

## Abschluss

Ein Merge auf `main` lässt `ci-main.yml` laufen (Unit, Integration, Smoke). Ein Deployment gibt es noch nicht.

## Geheimnisse

- `.env` nie lesen, nie durchsuchen (`grep -r --exclude='.env*'`)
