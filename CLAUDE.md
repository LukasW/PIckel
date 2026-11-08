# CLAUDE.md

## Project Overview

PIckel — ein Werkzeug für das **PI Planning** (SAFe): Program Increments, Teams, Features, Abhängigkeiten und
PI-Ziele planen. Aktuell ein **Walking Skeleton**: ein Durchstich «Program Increment anlegen und auflisten» durch
alle Schichten (Angular → REST → Use Case → Domain → Panache → PostgreSQL/Liquibase), mit Tests auf jeder Ebene und
CI. Fachliche Breite kommt Story für Story.

Ein Maven-Modul: Quarkus-Backend und Angular-SPA (über Quinoa) in einem deploybaren JAR.

## Wichtigste Regeln

1. **Abhängigkeiten zeigen nur nach innen, `domain/` ist framework-frei** — sonst ist die Fachlogik nicht ohne
   Quarkus testbar. `ArchitectureTest` (ArchUnit) bricht den Build bei einem Verstoss
2. **Tests vor dem Code, aus der Spezifikation** — ein Test, der aus dem Code abgeleitet ist, findet dessen Fehler
   nicht. Einen bestehenden Test abschwächen, abschalten oder löschen nur, wenn sich die Spezifikation geändert hat
3. **Schema nur über eine neue Liquibase-Migration**, bestehende Migrationen nie ändern — Liquibase prüft
   Checksummen, eine geänderte Migration bricht jede bestehende Datenbank
4. **Vor jedem Push `scripts/gate.sh` grün** — dieselben Prüfungen wie CI; CI ist kein Debugger
5. **Oberfläche Deutsch, Schweizer Schreibweise (`ss` statt Eszett), Du-Form; Bezeichner Englisch** — Zielgruppe sind
   Schweizer Teams; SAFe-Begriffe (Program Increment, ART, Feature, PI Objective) bleiben im Code englisch

Sprachspezifische Konventionen laden bei Bedarf aus `.claude/rules/java.md` und `.claude/rules/angular.md`.

## Tech Stack

- **Backend**: Java 25, Quarkus 3.40, Hibernate ORM mit Panache, PostgreSQL, Liquibase, SmallRye Health/OpenAPI
- **Frontend**: Angular 22 (standalone, Signals, zoneless), TypeScript 6, Tailwind CSS 4 mit Design-Tokens
- **Build**: Maven (`./mvnw`), npm 11, Quinoa; Spotless (Palantir) und Prettier als Formatierer
- **Tests**: JUnit 5 + Mockito, ArchUnit, PIT (Mutation), Cucumber JVM + REST-assured (`CucumberIT`),
  `@QuarkusIntegrationTest` (`SmokeIT`), Vitest (über `ng test`)
- **Auth**: noch keine, die API ist ungeschützt — Plan in `docs/arc42.md` → «Sicherheit»

## Hexagonal Architecture

### Backend (Java)

```
ch.grudligstrasse.pickel/
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

- JPA-Entities nur in `adapter/out/persistence/`, Mapping Entity ↔ Domain im Adapter — damit ein Schemawechsel die
  Domain nicht berührt
- REST-Resources delegieren an Use-Case-Ports, ohne Repository-Zugriff und ohne Fachlogik — sonst gibt es zwei Orte
  für dieselbe Regel; Domain-Exceptions → HTTP-Status über `ExceptionMapper`

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
scripts/gate.sh [--fast]    # alle Prüfungen wie CI; --fast ohne Integration
```

Dev Services starten PostgreSQL automatisch (Podman oder Docker nötig). Swagger UI: `/q/swagger-ui`,
Health: `/q/health`. Weitere Befehle (Loop, Formatieren, Mutation): `.claude/flow.md` → «Build».

## Testpyramide (verbindlich)

| Ebene | Werkzeug | Testet |
|-------|----------|--------|
| Unit | JUnit 5 + Mockito, Vitest | Domänenregeln, Use Cases mit gemockten Ports, Komponenten mit Mock-Repositories |
| Architektur | ArchUnit (`ArchitectureTest`) | Schichtregeln, läuft mit den Unit-Tests |
| Mutation | PIT (`-Pmutation`) | ob die Unit-Tests in `domain/` und `application/` Fehler finden (≥ 80 %) |
| Integration | `CucumberIT` (REST-assured gegen Dev-Services-PostgreSQL) | REST-Verträge, Persistenz |
| Smoke | `SmokeIT` | gepacktes JAR bootet, Liquibase + Hibernate-Validate, UI wird ausgeliefert |
| E2E | — (noch nicht eingerichtet) | erst, wenn eine Story einen echten Browser zwingend braucht |

- **Unit ist der Default.** Nach oben geht nur, was darunter nicht beweisbar ist — höhere Ebenen sind langsamer und
  bremsen den Loop des Agenten
- Gherkin unter `src/test/resources/features/` ist die Spezifikation. Tags: ohne Tag → `CucumberIT`; `@Unit` →
  belegt durch JUnit/Vitest mit Kommentar `# JUnit: <Pfad>/<Klasse> → <methode>` bzw.
  `# Vitest: <spec> → <testname>`; `@E2E` → Browser; `@Pending` → nicht umgesetzt
- Neue Logik → Unit-Test; neuer/geänderter Endpunkt oder Persistenz → `CucumberIT`-Szenario

## Allgemeine Code-Konventionen

- KISS, keine spekulativen Abstraktionen; DRY erst ab echter Duplikation — jede Abstraktion kostet Lesezeit
- Fail fast an den Systemgrenzen (REST, Formular), intern vertrauen — doppelte Prüfungen widersprechen sich irgendwann
- Nur öffentliche APIs dokumentieren (Ports, Services, Resources); Kommentare erklären **warum**
- Kein toter oder auskommentierter Code, keine TODOs ohne Issue-Referenz, keine Magic Numbers
- Keine `null`-Rückgaben — `Optional` (Java) bzw. `undefined` (TypeScript), damit der Typ das Fehlen ausdrückt

## Arbeitsablauf mit Claude

Agenten und Hooks liegen im Repository unter `.claude/`, die Skills nach [agentskills.io](https://agentskills.io)
unter `.agents/skills/` (ADR 0002), Projektangaben in
[`.claude/flow.md`](.claude/flow.md), Überblick in [`claude-guide.md`](claude-guide.md):
`/flow-task <beschreibung>` → `/flow-plan <nr>` → `/clear` → `/flow-implement <nr>` → `/flow-ship`.
