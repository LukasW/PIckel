# ADR 0005: Ein impliziter ART, Teams am ART, Kapazität und Board am PI

**Status:** Angenommen
**Datum:** 2026-11-07

## Kontext

Vor den ersten Stories von M1 war offen, ob der ART der Wurzel-Container ist und ob Teams zum ART oder zum PI
gehören (`spec/fachliche-spezifikation.md` → «Offene Fragen»). Mehrere ARTs und Solution Trains sind laut
Spezifikation nicht im MVP.

## Entscheid

- **Ein impliziter ART:** Es gibt keine Entität `ART`. Alles in einer Installation gehört zum selben ART
- **Teams gehören zum ART**, nicht zum PI: Sie werden einmal erfasst und stehen in jedem PI zur Verfügung
- **Am PI hängen** die Iterationen, die Kapazität pro Team und Iteration, die Features samt Einplanung, die
  Milestones und die Abhängigkeiten. Ein Team ohne Kapazität in einem PI hat dort die Kapazität 0
- Feature, Team und Milestone sind eigene Aggregate mit eigener ID; die Einplanung eines Features (Team ×
  Iteration) ist ein Wert am Feature

## Konsequenzen

- Teams werden nicht pro PI neu erfasst; wer ein Team entfernt, entfernt es für alle PIs (Regel in der Team-Story)
- Kommt ein zweiter ART, braucht es eine Migration, die eine ART-Entität einführt und die bestehenden Daten einem
  Standard-ART zuordnet; dafür eine neue ADR, die diese ersetzt
- Anmeldung und Rollen kommen erst in M2 mit dem Bereich «Zusammenarbeit»; bis dahin darf PIckel nicht öffentlich
  betrieben werden (`docs/arc42.md` → «Sicherheit»)
