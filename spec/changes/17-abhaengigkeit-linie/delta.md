# Delta US-17 — Abhängigkeiten als Linie auf dem Board (Issue #17)

Story: `spec/user-stories/17-abhaengigkeit-linie.md` (neu) · Status: entwurf

## Warum

Der «Red String» an der Wand zeigt auf einen Blick, welche Teams voneinander abhängen. Das Board braucht dieselbe
Sicht.

## Entscheide

- **Nur Frontend:** Das Board liefert die Abhängigkeiten mit (Erweiterung des Board-Endpunkts aus #12, im Plan
  zu klären); die Linie ist reine Darstellung
- **Linie** von der liefernden zur brauchenden Karte, mit Pfeil zur brauchenden Karte, als SVG über dem Raster
- **Backlog:** Liegt eines der Features im Backlog, gibt es keine Linie, aber die Karte trägt einen Zähler der
  Abhängigkeiten
- **Hervorheben:** Fokus oder Hover auf einer Karte hebt ihre Linien hervor und blendet die übrigen ab; so bleibt das
  Board bei vielen Linien lesbar
- **Testbarkeit:** Die Berechnung der Linien aus Zellen und Abhängigkeiten ist eine reine Funktion mit Vitest-Tests;
  Browser-E2E ist noch nicht eingerichtet

## Neu

### AK: Sind beide Features einer Abhängigkeit eingeplant, verbindet eine Linie mit Pfeil die liefernde mit der brauchenden Karte

### AK: Liegt eines der beiden Features im Backlog, gibt es keine Linie; die Karte zeigt die Anzahl ihrer Abhängigkeiten

### AK: Fokus oder Hover auf einer Karte hebt deren Linien hervor und blendet die übrigen ab

### AK: Nach dem Einplanen, Verschieben oder Zurücknehmen folgen die Linien ohne Neuladen

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

### Szenario: Hover hebt die Linien einer Karte hervor
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 3", "Login" bei "Zebra" in "Iteration 2" und "Suche" bei "Zebra" in "Iteration 1"
- **Und** "Portal" braucht "Login"
- **Und** "Suche" braucht "Login"
- **Wenn** ich mit der Maus auf "Suche" zeige
- **Dann** ist die Linie von "Login" zu "Suche" hervorgehoben
- **Und** ist die Linie von "Login" zu "Portal" abgeblendet

## Geändert

keine

## Entfernt

keine
