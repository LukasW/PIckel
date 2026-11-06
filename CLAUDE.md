# CLAUDE.md

## Project Overview

PIkel — ein Werkzeug für das **PI Planning** (SAFe): Program Increments, Teams, Features, Abhängigkeiten und
PI-Ziele planen. Aktuell ein **Walking Skeleton**: ein Durchstich «Program Increment anlegen und auflisten» durch
alle Schichten (Angular → REST → Use Case → Domain → Panache → PostgreSQL/Liquibase), mit Tests auf jeder Ebene und
CI. Fachliche Breite kommt Story für Story.

Ein Maven-Modul: Quarkus-Backend und Angular-SPA (über Quinoa) in einem deploybaren JAR.

## Tech Stack

- **Backend**: Java 25, Quarkus 3.40, Hibernate ORM mit Panache, PostgreSQL, Liquibase, SmallRye Health/OpenAPI
- **Frontend**: Angular 22 (standalone, Signals, zoneless), TypeScript 6, Tailwind CSS 4 mit Design-Tokens
- **Build**: Maven (`./mvnw`), npm 11, Quinoa
- **Tests**: JUnit 5 + Mockito, Cucumber JVM + REST-assured (`CucumberIT`), `@QuarkusIntegrationTest` (`SmokeIT`),
  Vitest (über `ng test`)
- **Auth**: noch keine — kommt als eigene Story (OIDC/Keycloak wie in Brainy, Session-Cookies, kein JWT im Browser)

## Hexagonal Architecture

### Backend (Java)

```
ch.grudligstrasse.pikel/
├── domain/                    # Innerer Hexagon — reine Fachlogik, keine Framework-Imports
│   ├── model/                 # Records + fachliche Exceptions (Validierung im Compact Constructor)
│   └── port/
│       ├── in/                # Driving Ports (Use-Case-Interfaces)
│       └── out/               # Driven Ports (Repository-Interfaces)
├── application/
│   └── service/               # Implementiert Driving Ports, ruft Driven Ports
└── adapter/
    ├── in/rest/               # JAX-RS-Resources, DTOs, ExceptionMapper
    └── out/persistence/       # JPA-Entities, Panache-Repository-Adapter, Mapping
```

**Regeln:**
- `domain/` hat **keine** Framework-Abhängigkeiten (kein Quarkus, JPA, Jackson, Jakarta)
- JPA-Entities nur in `adapter/out/persistence/`; Mapping Entity ↔ Domain im Adapter
- REST-Resources delegieren an Use-Case-Ports, keine Repository-Zugriffe, keine Fachlogik
- Domain-Exceptions → HTTP-Status über `ExceptionMapper` im REST-Adapter
- Abhängigkeiten zeigen nur nach innen

### Frontend (Angular)

```
src/main/webapp/src/app/
├── core/
│   ├── models/                # Domain-Typen (program-increment.model.ts)
│   ├── ports/                 # Abstrakte Repositories (Driven Ports)
│   ├── api/                   # HTTP-Adapter, implementieren die Ports (einziger Ort für HttpClient)
│   └── services/              # Use Cases, halten State als Signals
├── features/                  # UI pro Route (Driving Adapter)
└── shared/                    # Wiederverwendbare UI ohne Fachlogik
```

Port ↔ Adapter werden in `app.config.ts` verdrahtet (`{ provide: XRepository, useExisting: XHttpAdapter }`);
Tests ersetzen den Port durch ein Mock-Objekt.

## Build & Run

```bash
./mvnw quarkus:dev          # Dev-Modus mit Hot-Reload (Java + Angular), http://localhost:8080
./mvnw package              # Produktiv-Build → target/quarkus-app/quarkus-run.jar
```

Dev Services starten PostgreSQL automatisch (Podman oder Docker nötig). Swagger UI: `/q/swagger-ui`,
Health: `/q/health`.

## Testing

```bash
./mvnw test -B                              # JUnit + Vitest
./mvnw verify -B                            # + CucumberIT und SmokeIT (Dev-Services-PostgreSQL)
./mvnw test -B -DskipFrontendTests=true     # nur Backend
cd src/main/webapp && npm test              # nur Frontend (Vitest)
```

### Testpyramide (verbindlich)

| Ebene | Werkzeug | Testet |
|-------|----------|--------|
| Unit | JUnit 5 + Mockito, Vitest | Domänenregeln, Use Cases mit gemockten Ports, Komponenten mit Mock-Repositories |
| Integration | `CucumberIT` (REST-assured gegen Dev-Services-PostgreSQL) | REST-Verträge, Persistenz |
| Smoke | `SmokeIT` | gepacktes JAR bootet, Liquibase + Hibernate-Validate, UI wird ausgeliefert |
| E2E | — (noch nicht eingerichtet) | erst, wenn eine Story einen echten Browser zwingend braucht |

- **Unit ist der Default.** Nach oben geht nur, was darunter nicht beweisbar ist
- Gherkin unter `src/test/resources/features/` ist die Spezifikation. Tags: ohne Tag → `CucumberIT`; `@Unit` →
  belegt durch JUnit/Vitest mit Kommentar `# JUnit: <Pfad>/<Klasse> → <methode>` bzw.
  `# Vitest: <spec> → <testname>`; `@E2E` → Browser; `@Pending` → nicht umgesetzt
- Vitest-Testnamen mit Suffix `(US-<nr>)`
- **Definition of Done:** neue Logik → Unit-Test; neuer/geänderter Endpunkt oder Persistenz → `CucumberIT`-Szenario

## Code Conventions

### Allgemein
- KISS, keine spekulativen Abstraktionen; DRY erst ab echter Duplikation
- Fail fast an den Systemgrenzen (REST, Formular), intern vertrauen
- Nur öffentliche APIs dokumentieren (Ports, Services, Resources); Kommentare erklären **warum**
- Kein toter oder auskommentierter Code, keine TODOs ohne Issue-Referenz, keine Magic Numbers
- Keine `null`-Rückgaben — `Optional` (Java) bzw. `undefined` (TypeScript)

### Java
- Records für Domain-Modelle und DTOs; Constructor Injection
- `@ApplicationScoped` für Services und Adapter; `@Transactional` im Application-Service
- DB-Spalten snake_case; Schema nur über neue Liquibase-Migration (`db/migration/V<n>__<slug>.sql` + `changeLog.xml`),
  bestehende Migrationen nie ändern
- 4 Leerzeichen Einzug

### Angular
- Standalone (nie `standalone: true` setzen), `ChangeDetectionStrategy.OnPush`, Selector-Prefix `app-`
- Signals für State (`signal`, `computed`, `linkedSignal`, `effect`), `inject()` statt Constructor-Injection
- `input()`/`output()` statt Decorators; `@if`/`@for`/`@switch` statt `*ngIf`/`*ngFor`
- Reactive Forms; strict TypeScript, kein `any`
- **Design-Tokens statt Tailwind-Paletten:** `bg-surface`, `bg-background`, `text-foreground`, `text-muted`,
  `border-border`, `bg-accent`, `text-danger` (definiert in `src/styles.css`). Verboten: `bg-blue-600` usw.
- Prettier (Breite 100, Single Quotes), `npm run format` vor dem Commit; `format:check` ist CI-Gate

### Sprache
- Oberfläche Deutsch mit echten Umlauten, Schweizer Schreibweise (`ss` statt `ß`), Du-Form
- Bezeichner Englisch; Fachbegriffe aus SAFe (Program Increment, ART, Feature, PI Objective) im Code englisch

## Arbeitsablauf mit Claude

- Der Ablauf kommt aus dem Plugin `flow` (`Tools/claude-flow`, in `.claude/settings.json` eingetragen und auf ein
  Tag gepinnt); was PIkel daran anders macht, steht in [`.claude/flow.md`](.claude/flow.md). Issues und PRs über
  GitHub: `/flow:task <beschreibung>` → `/flow:plan <nr>` → `/clear` → `/flow:implement <nr>` → `/flow:ship`.
  Dazu `/flow:grill-me <nr>`, `/flow:fast <beschreibung>`, `/flow:fix-build`. Einzelheiten in
  [`claude-guide.md`](claude-guide.md)
- **Tests sind die Spezifikation:** `/flow:implement` lässt sie vor dem Code von `flow:test-author` schreiben.
  Einen bestehenden Test abschwächen, abschalten oder löschen nur mit Begründung
- `.claude/hooks/reviewer-reminder.sh` schlägt nach einem Edit die passenden Reviewer vor (`hexagonal-reviewer`,
  `angular-signals-reviewer`, `bdd-cucumber-author`, `flow:test-reviewer`). Vor dem Commit laufen lassen
