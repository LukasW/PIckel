# Delta US-15 — Milestones auf dem Board (Issue #15)

Story: `spec/user-stories/15-milestones.md` (neu) · Status: eingepflegt

## Warum

Releases, Messen oder Audits legen Termine fest, um die herum die Teams planen. An der Wand hängen sie als Marker
über den Iterationen; auf dem Board gehören sie an dieselbe Stelle.

## Entscheide

- **Milestone gehört zum PI**, mit Name und Datum; das Datum liegt im Zeitraum des PI (ADR 0005)
- **Position:** Der Marker steht über der Iteration, in deren Zeitraum das Datum liegt
- **Erfassen, ändern, löschen** auf der Detailseite des PI, Anzeige auf dem Board
- **Nicht in dieser Story:** Milestones einem Team oder Feature zuordnen, wiederkehrende Termine

## Neu

### AK: Ein Milestone hat einen Namen (Pflicht, getrimmt, höchstens 100 Zeichen) und ein Datum im Zeitraum des PI

### AK: Ein Datum ausserhalb des PI wird mit einer verständlichen Meldung abgelehnt

### AK: Milestones lassen sich ändern und nach Bestätigung löschen

### AK: Das Board zeigt jeden Milestone als Marker mit Name und Datum (TT.MM.) über der Iteration, in die sein Datum fällt

### Szenario: Milestone wird erfasst und über der passenden Iteration angezeigt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Wenn** ich im PI "PI 27.1" den Milestone "Release 4.0" am "2027-02-10" erfasse
- **Dann** liegt der Milestone "Release 4.0" auf dem Board über "Iteration 3"

### Szenario: Milestone ausserhalb des PI wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Wenn** ich im PI "PI 27.1" den Milestone "Release 4.0" am "2027-04-01" erfasse
- **Dann** wird das Erfassen mit der Meldung "Das Datum muss im Zeitraum des PI liegen." abgelehnt

### Szenario: Milestone ohne Namen wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Wenn** ich im PI "PI 27.1" den Milestone " " am "2027-02-10" erfasse
- **Dann** wird das Erfassen mit der Meldung "Der Name darf nicht leer sein." abgelehnt

### Szenario: Milestone wird verschoben
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** im PI "PI 27.1" der Milestone "Release 4.0" am "2027-02-10"
- **Wenn** ich beim Milestone "Release 4.0" das Datum auf "2027-01-05" setze
- **Dann** liegt der Milestone "Release 4.0" auf dem Board über "Iteration 1"

### Szenario: Milestone wird gelöscht
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** im PI "PI 27.1" der Milestone "Release 4.0" am "2027-02-10"
- **Wenn** ich den Milestone "Release 4.0" lösche
- **Dann** hat das Board von "PI 27.1" keine Milestones

### Szenario: Unbekannter Milestone wird nicht gefunden
Im Nachtlauf ergänzt: ADR 0003 verlangt für jede neue Fehlerart ein `CucumberIT`-Szenario mit der Meldung.
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Wenn** ich im PI "PI 27.1" einen unbekannten Milestone lösche
- **Dann** antwortet der Server mit 404 und der Meldung "Den Milestone gibt es nicht."

## Geändert

keine

## Entfernt

keine
