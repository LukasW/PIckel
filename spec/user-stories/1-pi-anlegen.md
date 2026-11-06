# US-1: PI anlegen (Walking Skeleton)

**ID:** US-1
**Status:** Umgesetzt
**Priorität:** 1
**Schätzung:** 3

## Beschreibung

Als RTE möchte ich ein Program Increment mit Name und Zeitraum anlegen und alle PIs sehen, damit das Team dafür
planen kann.

## Hintergrund

Durchstich durch alle Schichten (Walking Skeleton): Angular → REST → Use Case → Domain → Panache → PostgreSQL,
mit Tests auf jeder Ebene und CI. Siehe [ADR 0001](../../docs/adr/0001-walking-skeleton.md).

## Akzeptanzkriterien

1. Ein PI hat Name, Start- und Enddatum; es erscheint nach dem Anlegen in der Liste
2. Die Liste ist aufsteigend nach Startdatum sortiert
3. Ein Enddatum, das nicht nach dem Startdatum liegt, wird mit einer verständlichen Meldung abgelehnt
4. Ein leerer Name wird abgelehnt
5. Ohne PIs zeigt die Seite einen Leerzustand

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/1-pi-anlegen.feature`](../../src/test/resources/features/program-increments/1-pi-anlegen.feature).

## Technische Notizen

- Validierung im Domain-Record `ProgramIncrement`; `InvalidProgramIncrementException` → 400 im REST-Adapter
- Migration `V1__create_program_increments.sql` mit Check-Constraint auf die Daten

## Aufgaben

- [x] Domain, Use Case, Persistenz, REST
- [x] Angular-Seite mit Formular und Liste
- [x] Tests: JUnit, Vitest, CucumberIT, SmokeIT
- [x] CI (PR und main)
