# Notizen US-15

- Milestone als eigenes Aggregate mit eigener Tabelle `milestone` (ADR 0005), nicht als Liste am PI wie die Links — Grund: ADR nennt ihn ausdrücklich als eigenes Aggregate
- Migration V8 legt nur die Tabelle an, ändert keine Daten; FK mit `ON DELETE CASCADE`, damit das Leeren der PIs in den Cucumber-Szenarien weiter geht
- Den PI-Zeitraum prüfen `Milestone.create`/`update`, nicht der Compact Constructor: der Milestone kennt sein PI nur über die ID
- Spalte `milestone_date` statt `date`, weil `date` in SQL ein Schlüsselwort ist
- Meldung «Den Milestone gibt es nicht.» (Akkusativ wie «Den Link gibt es nicht.»); das Gerüst hatte «Der …», Test nachgezogen
- Delta um das Szenario «Unbekannter Milestone wird nicht gefunden» ergänzt (nur «Neu»): ADR 0003 verlangt für jede neue Fehlerart ein CucumberIT-Szenario
- Reihenfolge der Milestones: Datum, bei gleichem Datum Name — sortiert der Repository-Adapter; das Frontend reiht nach Erfassen/Ändern gleich ein, statt neu zu laden
- REST: `POST` → 201 mit `Location`, `PUT` → 200, `DELETE` → 204; das Board liefert `milestones` mit `iterationNumber`, damit die Oberfläche nicht selbst Daten den Iterationen zuordnet
- Vitest Detailseite: der eingebettete Abschnitt lädt erst, wenn das PI da ist; der Test wartet eine zweite Runde (`settle()`), sonst ist die Liste noch leer
- Das Board-JSON hat ein zusätzliches Feld `milestones`; das Delta auf `main` führt unter «Geändert» nichts für US-12, die Anzeige ohne Milestones bleibt gleich
