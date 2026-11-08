# Delta US-14 — Kapazität pro Zelle mit Überlast anzeigen (Issue #14)

Story: `spec/user-stories/14-zellen-kapazitaet.md` (neu) · Status: eingepflegt

## Warum

An der Wand rechnet jedes Team im Kopf, ob es sich überplant. Das Board zeigt es pro Zelle sofort.

## Entscheide

- **Last einer Zelle** = Summe der Schätzungen ihrer Features; ein Feature ohne Schätzung zählt 0 (ADR 0006)
- **Überlast** heisst geplant grösser als verfügbar; gleich viel ist keine Überlast
- **Ort der Berechnung:** Das Board liefert pro Zelle die verfügbare Kapazität (ohne Eintrag 0). Das Frontend leitet
  geplant und Überlast aus den Karten auf dem Board ab, damit die Werte nach dem optimistischen Einplanen aus #13
  sofort stimmen und bei einem Fehler mit der Karte zurückkehren (Quelle: `entscheide.md`)
- **Nicht nur Farbe:** Überlast zeigt die Zelle rot und mit Text, damit sie auch ohne Farbsehen erkennbar ist
- **IP-Iteration** zeigt keine Kapazität (dort wird nicht geplant)
- **Nur anzeigen:** Einplanen über die Kapazität bleibt erlaubt (Quelle: `spec/changes/13-feature-einplanen/entscheide.md`)

## Neu

### AK: Jede Zelle ausser in der IP-Iteration zeigt «geplant / verfügbar SP»

### AK: Geplant ist die Summe der Schätzungen der Features in der Zelle; Features ohne Schätzung zählen 0

### AK: Ist geplant grösser als verfügbar, ist die Zelle rot hervorgehoben und mit «Überlast» beschriftet

### AK: Nach dem Einplanen, Verschieben oder Zurücknehmen eines Features stimmen die Werte ohne Neuladen

### AK: Feature-Karten ohne Schätzung tragen den Hinweis «ohne Schätzung»

### Szenario: Board liefert die Kapazität pro Zelle
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler" mit der Kapazität 20 in "Iteration 1" von "PI 27.1"
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** liefert das Board für "Adler" in "Iteration 1" die Kapazität 20
- **Und** liefert das Board für "Adler" in "Iteration 2" die Kapazität 0
- **Und** liefert das Board für die IP-Iteration keine Kapazität

### Szenario: Zelle zeigt geplant gegenüber verfügbar
`@Unit` (Vitest)
- **Gegeben sei** das Team "Adler" mit der Kapazität 20 in "Iteration 1" auf dem Board
- **Und** das Feature "Portal" mit Schätzung 13 bei "Adler" in "Iteration 1"
- **Wenn** ich das Board öffne
- **Dann** zeigt die Zelle "Adler" × "Iteration 1" «13 / 20 SP»
- **Und** ist die Zelle "Adler" × "Iteration 1" nicht als Überlast markiert
- **Und** zeigt die IP-Iteration keine Kapazität

### Szenario: Zelle über der Kapazität ist überlastet
`@Unit` (Vitest)
- **Gegeben sei** das Team "Adler" mit der Kapazität 20 in "Iteration 1" auf dem Board
- **Und** die Features "Portal" mit Schätzung 13 und "Suche" mit Schätzung 8 bei "Adler" in "Iteration 1"
- **Wenn** ich das Board öffne
- **Dann** zeigt die Zelle "Adler" × "Iteration 1" «21 / 20 SP»
- **Und** ist die Zelle rot hervorgehoben und mit «Überlast» beschriftet

### Szenario: Genau ausgelastete Zelle ist nicht überlastet
`@Unit` (Vitest)
- **Gegeben sei** das Team "Adler" mit der Kapazität 13 in "Iteration 1" auf dem Board
- **Und** das Feature "Portal" mit Schätzung 13 bei "Adler" in "Iteration 1"
- **Wenn** ich das Board öffne
- **Dann** ist die Zelle "Adler" × "Iteration 1" nicht als Überlast markiert

### Szenario: Feature ohne Schätzung zählt 0
`@Unit` (Vitest)
- **Gegeben sei** das Team "Adler" ohne Kapazität auf dem Board
- **Und** das Feature "Login" ohne Schätzung bei "Adler" in "Iteration 1"
- **Wenn** ich das Board öffne
- **Dann** zeigt die Zelle "Adler" × "Iteration 1" «0 / 0 SP»
- **Und** trägt die Karte "Login" den Hinweis «ohne Schätzung»

### Szenario: Werte folgen dem Einplanen ohne Neuladen
`@Unit` (Vitest)
- **Gegeben sei** das Team "Adler" mit der Kapazität 10 in "Iteration 1" und in "Iteration 2" auf dem Board
- **Und** das Feature "Portal" mit Schätzung 8 bei "Adler" in "Iteration 1"
- **Wenn** ich "Portal" nach "Adler" × "Iteration 2" verschiebe
- **Dann** zeigt die Zelle "Adler" × "Iteration 1" «0 / 10 SP» und die Zelle "Adler" × "Iteration 2" «8 / 10 SP»
- **Und** schlägt das Speichern fehl, zeigen beide Zellen wieder die Werte von vorher

## Geändert

keine

## Entfernt

keine
