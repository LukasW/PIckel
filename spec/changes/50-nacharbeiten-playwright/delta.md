# Delta US-13 — Feature auf dem Board einplanen (Issue #50)

Story: `spec/user-stories/13-feature-einplanen.md` (bestehend) · Status: eingepflegt

## Warum

Im Playwright-Test landete eine Karte, die über der IP-Iteration losgelassen wurde, kommentarlos in Iteration 4. AK 3
verlangt eine Ablehnung mit Meldung. Die Ursache betrifft jedes Loslassen ausserhalb einer Ablage: Das Board legt die
Karte dann in der zuletzt überfahrenen Zelle ab.

## Entscheide

- **Ausserhalb einer Ablage losgelassen:** Die Karte bleibt an ihrem bisherigen Ort, ohne Meldung. Das ist das übliche
  Verhalten beim Ziehen und bisher nicht beschrieben (Plan)
- **Über der IP-Iteration losgelassen:** Die Karte bleibt am bisherigen Ort, und über dem Board erscheint die Meldung
  aus AK 3. Die IP-Zelle bleibt keine Ablage; es gibt also keine Vorschau, die ein Einplanen vortäuscht (Plan)

## Neu

### AK: Wird eine Karte ausserhalb einer Ablage losgelassen, bleibt sie an ihrem bisherigen Ort; über der IP-Iteration erscheint dazu die Meldung aus AK 3

### Szenario: Karte über der IP-Iteration losgelassen bleibt an ihrem Ort
- **Gegeben sei** das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich "Portal" über die Zellen von "Adler" ziehe und über "IP" loslasse
- **Dann** liegt "Portal" weiter bei "Adler" in "Iteration 2"
- **Und** über dem Board steht "In der IP-Iteration wird keine Feature-Arbeit geplant."

### Szenario: Karte ausserhalb einer Ablage losgelassen bleibt an ihrem Ort
- **Gegeben sei** das Feature "Portal" im Backlog des Boards
- **Wenn** ich "Portal" über die Zelle von "Adler" in "Iteration 1" ziehe und neben dem Raster loslasse
- **Dann** liegt "Portal" weiter im Backlog des Boards
- **Und** über dem Board steht keine Meldung

## Geändert

keine

## Entfernt

keine

