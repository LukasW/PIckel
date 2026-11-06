# ADR 0001: Walking Skeleton mit dem Stack von Brainy

**Status:** Angenommen
**Datum:** 2026-11-06

## Kontext

PIkel soll ein PI-Planning-Werkzeug werden. Bevor Fachlichkeit entsteht, soll ein lauffähiger Durchstich durch alle
Schichten stehen — inklusive Build, Tests, CI und Claude-Code-Setup —, damit jede weitere Story nur noch Fachlogik
hinzufügt.

## Entscheid

- Stack und Struktur wie Brainy: Quarkus + Angular in einem Maven-Modul über Quinoa, hexagonal in Backend und
  Frontend, Liquibase, Testpyramide mit Gherkin als Spezifikation
- Claude-Code-Ablauf über das Plugin `flow`, projektspezifisch in `.claude/flow.md`; Issues und PRs auf GitHub
- Bewusst **noch nicht**: Auth, Offline/IndexedDB-Sync, Browser-E2E, OpenAPI-Codegenerierung, Container-Release.
  Jedes davon kommt mit der ersten Story, die es braucht

## Konsequenzen

- Erster Durchstich: «Program Increment anlegen und auflisten» (US-1)
- Der REST-Endpunkt ist bis zur Auth-Story ungeschützt — PIkel darf bis dahin nicht öffentlich betrieben werden
