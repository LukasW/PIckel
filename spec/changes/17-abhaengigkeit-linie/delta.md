# Delta US-17 — Abhängigkeiten als Linie auf dem Board (Issue #17)

Story: `spec/user-stories/17-abhaengigkeit-linie.md` (neu) · Status: bestätigt

## Warum

Der «Red String» an der Wand zeigt auf einen Blick, welche Teams voneinander abhängen. Das Board braucht dieselbe
Sicht.

## Entscheide

- **Daten:** Der Board-Endpunkt aus #12 liefert die Abhängigkeiten des PI mit, damit die Oberfläche ein konsistentes
  Bild in einem Aufruf erhält (Rückfrage im Plan; ersetzt den Entwurf «Nur Frontend»). Die Linie selbst ist reine
  Darstellung
- **Linie** von der liefernden zur brauchenden Karte, mit Pfeil zur brauchenden Karte, als SVG über dem Raster
- **Backlog:** Liegt eines der Features im Backlog, gibt es keine Linie. Beide Karten, im Raster wie im Backlog,
  zeigen die Zahl ihrer Abhängigkeiten (beide Richtungen), für die keine Linie gezeichnet ist; bei 0 nichts
  (Rückfrage im Plan)
- **Hervorheben:** Fokus oder Hover auf einer Karte hebt ihre Linien hervor und blendet die übrigen ab; so bleibt das
  Board bei vielen Linien lesbar
- **Ohne Neuladen:** Welche Abhängigkeit eine Linie hat, leitet das Frontend aus dem Board-Signal ab, das das
  optimistische Einplanen aus #13 ändert (wie die Kapazität in #14)
- **Testbarkeit:** Welche Abhängigkeiten eine Linie bekommen und die Geometrie der Linien sind reine Funktionen mit
  Vitest-Tests; Browser-E2E ist noch nicht eingerichtet. Lage und Aussehen der Linien prüft ein Screenshot im PR

## Neu

### AK: Das Board liefert die Abhängigkeiten des PI mit

### AK: Sind beide Features einer Abhängigkeit eingeplant, verbindet eine Linie mit Pfeil die liefernde mit der brauchenden Karte

### AK: Liegt eines der beiden Features im Backlog, gibt es keine Linie; beide Karten zeigen die Anzahl ihrer Abhängigkeiten ohne Linie

### AK: Fokus oder Hover auf einer Karte hebt deren Linien hervor und blendet die übrigen ab

### AK: Nach dem Einplanen, Verschieben oder Zurücknehmen folgen die Linien ohne Neuladen

### Szenario: Board liefert die Abhängigkeiten
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** liefert das Board die Abhängigkeit "Portal" braucht "Login"

### Szenario: Linie zwischen zwei eingeplanten Features
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 3" und "Login" bei "Zebra" in "Iteration 2"
- **Und** "Portal" braucht "Login"
- **Wenn** das Board die Linien berechnet
- **Dann** führt eine Linie von "Login" zu "Portal"

### Szenario: Keine Linie, wenn ein Feature im Backlog liegt
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 3" und "Login" im Backlog
- **Und** "Portal" braucht "Login"
- **Wenn** das Board die Linien berechnet
- **Dann** gibt es keine Linie
- **Und** zeigt die Karte "Portal" 1 Abhängigkeit
- **Und** zeigt die Karte "Login" 1 Abhängigkeit

### Szenario: Hover hebt die Linien einer Karte hervor
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 3", "Login" bei "Zebra" in "Iteration 2" und "Suche" bei "Zebra" in "Iteration 1"
- **Und** "Portal" braucht "Login"
- **Und** "Suche" braucht "Login"
- **Wenn** ich mit der Maus auf "Suche" zeige
- **Dann** ist die Linie von "Login" zu "Suche" hervorgehoben
- **Und** ist die Linie von "Login" zu "Portal" abgeblendet

### Szenario: Linie folgt dem Einplanen ohne Neuladen
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 3" und "Login" im Backlog
- **Und** "Portal" braucht "Login"
- **Wenn** ich "Login" bei "Zebra" in "Iteration 2" einplane
- **Dann** führt eine Linie von "Login" zu "Portal"
- **Und** zeigt keine Karte eine Anzahl Abhängigkeiten

## Geändert

keine — das Board-JSON bekommt ein zusätzliches Feld wie bei #14 und #15; die Szenarien von #12 behalten ihr
Ergebnis

## Entfernt

keine
