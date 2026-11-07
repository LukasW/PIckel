# Delta US-13 — Feature auf dem Board einplanen (Issue #13)

Story: `spec/user-stories/13-feature-einplanen.md` (neu) · Status: entwurf

## Warum

Der Kern des Plannings: Features in die Zellen Team × Iteration legen und umhängen, so schnell wie Klebezettel.

## Entscheide

- **Einplanung als Wert am Feature** (Team und Iteration), ein Feature liegt in höchstens einer Zelle (ADR 0005,
  ADR 0006)
- **IP-Iteration gesperrt:** Dort wird keine Feature-Arbeit geplant; der Server lehnt ab, die Oberfläche lässt das
  Ablegen gar nicht zu
- **Bedienung ohne Maus:** Jede Karte hat zusätzlich eine Aktion «Einplanen …» mit Auswahl von Team und Iteration
  (Tastatur, Barrierefreiheit). Angular CDK Drag & Drop als Kandidat; Entscheid im Plan
- **Optimistisch mit Rücknahme:** Die Karte springt sofort, bei einem Fehler zurück, mit Meldung
- **Team entfernen:** Seine eingeplanten Features kommen in allen PIs zurück ins Backlog (ergänzt die Team-Story
  #6 um einen Fall, den es dort noch nicht gab)
- **Nicht in dieser Story:** gleichzeitiges Arbeiten mehrerer Personen (M2), Reihenfolge innerhalb einer Zelle

## Neu

### AK: Ein Feature lässt sich aus dem Backlog in eine Zelle Team × Iteration ziehen und ist danach dort eingeplant

### AK: Ein eingeplantes Feature lässt sich in eine andere Zelle verschieben oder zurück ins Backlog ziehen

### AK: In die IP-Iteration lässt sich kein Feature einplanen; der Versuch wird mit einer verständlichen Meldung abgelehnt

### AK: Jede Karte lässt sich auch ohne Maus über die Aktion «Einplanen …» einplanen, verschieben und zurücknehmen

### AK: Schlägt das Speichern fehl, springt die Karte an ihren alten Ort zurück und eine Meldung erscheint

### AK: Wird ein Team entfernt, kommen seine eingeplanten Features in allen PIs zurück ins Backlog

### Szenario: Feature wird in eine Zelle eingeplant
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich "Portal" bei "Adler" in "Iteration 2" einplane
- **Dann** liegt "Portal" auf dem Board bei "Adler" in "Iteration 2"
- **Und** liegt "Portal" nicht mehr im Backlog des Boards

### Szenario: Eingeplantes Feature wird verschoben
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** das Team "Zebra"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich "Portal" bei "Zebra" in "Iteration 3" einplane
- **Dann** liegt "Portal" auf dem Board bei "Zebra" in "Iteration 3"

### Szenario: Eingeplantes Feature geht zurück ins Backlog
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich "Portal" zurück ins Backlog nehme
- **Dann** liegt "Portal" im Backlog des Boards

### Szenario: Einplanen in die IP-Iteration wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich "Portal" bei "Adler" in "IP" einplane
- **Dann** wird das Einplanen mit der Meldung "In der IP-Iteration wird keine Feature-Arbeit geplant." abgelehnt

### Szenario: Einplanen in eine Iteration eines anderen PI wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich "Portal" bei "Adler" in "Iteration 1" von "PI 27.2" einplane
- **Dann** wird das Einplanen mit der Meldung "Die Iteration gehört nicht zu diesem PI." abgelehnt

### Szenario: Entferntes Team gibt seine Features ins Backlog zurück
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich das Team "Adler" entferne
- **Dann** liegt "Portal" im Backlog des Boards

## Geändert

keine — das Entfernen eines Teams mit eingeplanten Features ist ein neuer Fall; die Szenarien der Team-Story
(#6) behalten ihr Ergebnis

## Entfernt

keine
