# Delta US-7 — Kapazität pro Team und Iteration erfassen (Issue #7)

Story: `spec/user-stories/7-kapazitaet-erfassen.md` (neu) · Status: entwurf

## Warum

Das Board zeigt Überlast nur, wenn es die verfügbare Kapazität jedes Teams pro Iteration kennt. Die Kapazität
gehört zum PI, die Teams zum ART (ADR 0005).

## Entscheide

- **Einheit Story Points**, ganzzahlig, ab 0 (ADR 0006)
- **Nur Entwicklungs-Iterationen:** Für die IP-Iteration wird keine Kapazität erfasst, weil dort keine
  Feature-Arbeit geplant wird
- **Standard 0:** Ein Team ohne erfasste Kapazität hat im PI die Kapazität 0 (ADR 0005)
- **Erfassung als Tabelle** Teams × Iterationen auf der Detailseite des PI, gespeichert pro Zelle
- **Nicht in dieser Story:** Kapazität aus Anwesenheiten berechnen, Kapazität ins nächste PI kopieren

## Neu

### AK: Auf der Detailseite eines PI lässt sich für jedes Team und jede Iteration ausser der IP-Iteration eine Kapazität in Story Points erfassen

### AK: Die Kapazität ist eine ganze Zahl ab 0; andere Werte werden mit einer verständlichen Meldung abgelehnt

### AK: Ohne Eintrag gilt die Kapazität 0

### AK: Die Kapazität eines Teams ist pro PI getrennt; ein Eintrag in einem PI ändert kein anderes PI

### AK: Ohne Teams zeigt die Tabelle einen Hinweis mit Link zur Teamseite

### Szenario: Kapazität wird erfasst
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Rakete"
- **Wenn** ich für "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität 40 erfasse
- **Dann** hat "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität 40

### Szenario: Ohne Eintrag ist die Kapazität 0
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Rakete"
- **Dann** hat "Rakete" in "Iteration 2" von "PI 27.1" die Kapazität 0

### Szenario: Negative Kapazität wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Rakete"
- **Wenn** ich für "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität -5 erfasse
- **Dann** wird das Erfassen mit der Meldung "Die Kapazität muss eine ganze Zahl ab 0 sein." abgelehnt

### Szenario: Kapazität für die IP-Iteration wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Rakete"
- **Wenn** ich für "Rakete" in "IP" von "PI 27.1" die Kapazität 10 erfasse
- **Dann** wird das Erfassen mit der Meldung "In der IP-Iteration wird keine Kapazität geplant." abgelehnt

### Szenario: Kapazität ist pro PI getrennt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25" mit Iterationen zu 2 Wochen
- **Und** das Team "Rakete"
- **Wenn** ich für "Rakete" in "Iteration 1" von "PI 27.1" die Kapazität 40 erfasse
- **Dann** hat "Rakete" in "Iteration 1" von "PI 27.2" die Kapazität 0

## Geändert

keine

## Entfernt

keine
