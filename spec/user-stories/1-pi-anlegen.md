# US-1: PI anlegen (Walking Skeleton)

**ID:** US-1
**Issue:** keines (vor GitHub)
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
6. Eine unvollständige oder ungültige Anfrage wird mit einer verständlichen Meldung abgelehnt
7. Das Formular zeigt fehlende oder zu lange Angaben am Feld an und legt dann nichts an
8. Start- und Enddatum erscheinen im Format TT.MM.JJJJ
9. Die Kopfzeile verlinkt auf jeder Seite die PI-Übersicht («Program Increments») und die Teams

## BDD-Szenarien

Siehe [`src/test/resources/features/program-increments/1-pi-anlegen.feature`](../../src/test/resources/features/program-increments/1-pi-anlegen.feature).

## Tests

- AK 9 (Kopfzeile): `@Unit` «Kopfzeile führt zur PI-Übersicht»; Vitest `app.spec.ts` → «führt über «Program
  Increments» in der Kopfzeile zur Liste der PIs (US-1)», «zeigt den Link «Program Increments» auf jeder Seite, auch
  auf Detailseite und Board (US-1)», «verlinkt in der Kopfzeile «Program Increments» auf die PI-Übersicht, neben
  «Teams» (US-1)»
- Übrige Kriterien: siehe Feature-Datei

## Änderungen

- Chore Projekt-Review: Fehlervertrag, fehlende Szenarien, Formular — `spec/changes/chore-review-massnahmen/delta.md`
- #5: PI anlegen: Antwort mit Iterationen und `Location` — `spec/changes/5-iterationen-festlegen/delta.md`
- #50: Nacharbeiten aus dem Playwright-Test — `spec/changes/50-nacharbeiten-playwright/delta.md`
