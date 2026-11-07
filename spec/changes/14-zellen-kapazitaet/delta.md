# Delta US-14 — Kapazität pro Zelle mit Überlast anzeigen (Issue #14)

Story: `spec/user-stories/14-zellen-kapazitaet.md` (neu) · Status: entwurf

## Warum

An der Wand rechnet jedes Team im Kopf, ob es sich überplant. Das Board zeigt es pro Zelle sofort.

## Entscheide

- **Last einer Zelle** = Summe der Schätzungen ihrer Features; ein Feature ohne Schätzung zählt 0 (ADR 0006)
- **Überlast** heisst geplant grösser als verfügbar; gleich viel ist keine Überlast
- **Berechnung in der Domain**, das Board liefert pro Zelle geplant und verfügbar mit, damit die Regel an einem Ort
  steht und ohne Browser testbar ist
- **Nicht nur Farbe:** Überlast zeigt die Zelle rot und mit Text, damit sie auch ohne Farbsehen erkennbar ist
- **IP-Iteration** zeigt keine Kapazität (dort wird nicht geplant)

## Neu

### AK: Jede Zelle ausser in der IP-Iteration zeigt «geplant / verfügbar SP»

### AK: Geplant ist die Summe der Schätzungen der Features in der Zelle; Features ohne Schätzung zählen 0

### AK: Ist geplant grösser als verfügbar, ist die Zelle rot hervorgehoben und mit «Überlast» beschriftet

### AK: Nach dem Einplanen, Verschieben oder Zurücknehmen eines Features stimmen die Werte ohne Neuladen

### AK: Feature-Karten ohne Schätzung tragen den Hinweis «ohne Schätzung»

### Szenario: Zelle zeigt geplant gegenüber verfügbar
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler" mit der Kapazität 20 in "Iteration 1" von "PI 27.1"
- **Und** im PI "PI 27.1" das Feature "Portal" mit Schätzung 13 bei "Adler" in "Iteration 1" eingeplant
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** zeigt die Zelle "Adler" × "Iteration 1" geplant 13 und verfügbar 20
- **Und** ist die Zelle "Adler" × "Iteration 1" nicht überlastet

### Szenario: Zelle über der Kapazität ist überlastet
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler" mit der Kapazität 20 in "Iteration 1" von "PI 27.1"
- **Und** im PI "PI 27.1" das Feature "Portal" mit Schätzung 13 bei "Adler" in "Iteration 1" eingeplant
- **Und** im PI "PI 27.1" das Feature "Suche" mit Schätzung 8 bei "Adler" in "Iteration 1" eingeplant
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** zeigt die Zelle "Adler" × "Iteration 1" geplant 21 und verfügbar 20
- **Und** ist die Zelle "Adler" × "Iteration 1" überlastet

### Szenario: Genau ausgelastete Zelle ist nicht überlastet
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler" mit der Kapazität 13 in "Iteration 1" von "PI 27.1"
- **Und** im PI "PI 27.1" das Feature "Portal" mit Schätzung 13 bei "Adler" in "Iteration 1" eingeplant
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** ist die Zelle "Adler" × "Iteration 1" nicht überlastet

### Szenario: Feature ohne Schätzung zählt 0
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Login" ohne Schätzung bei "Adler" in "Iteration 1" eingeplant
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** zeigt die Zelle "Adler" × "Iteration 1" geplant 0 und verfügbar 0

## Geändert

keine

## Entfernt

keine
