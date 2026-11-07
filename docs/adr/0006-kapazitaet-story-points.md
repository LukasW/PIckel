# ADR 0006: Kapazität und Schätzung in Story Points, Iterationen aus fester Länge

**Status:** Angenommen
**Datum:** 2026-11-07

## Kontext

Das Program Board vergleicht pro Zelle Team × Iteration die geplante Last mit der verfügbaren Kapazität. Offen
waren die Einheit (Story Points oder Personentage, fest oder wählbar) und woher die Last kommt; die Spezifikation
nannte für Features nur Titel, Beschreibung, WSJF-Rang und Owner.

## Entscheid

- **Einheit fest Story Points** (normalisiert nach SAFe), ganzzahlig und nicht negativ, für Kapazität und Schätzung
- **Ein Feature hat eine optionale Schätzung in SP** und liegt auf dem Board in höchstens einer Zelle. Die Last einer
  Zelle ist die Summe der Schätzungen ihrer Features; ein Feature ohne Schätzung zählt 0
- **Iterationen entstehen aus einer festen Länge in Wochen:** PIkel teilt den Zeitraum des PI auf, die letzte
  Iteration ist die IP-Iteration. In der IP-Iteration wird kein Feature eingeplant

## Konsequenzen

- Teams, die in Personentagen planen, rechnen um; eine wählbare Einheit braucht eine neue ADR
- Features, an denen mehrere Teams arbeiten, lassen sich in M1 nicht auf Teams aufteilen; der Bedarf wird nach M1
  geprüft
- Unregelmässige Iterationen (Feiertage, verkürzte Sprints) sind nicht abbildbar, bis eine Story sie verlangt
