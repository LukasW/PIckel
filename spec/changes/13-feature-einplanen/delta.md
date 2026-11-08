# Delta US-13 — Feature auf dem Board einplanen (Issue #13)

Story: `spec/user-stories/13-feature-einplanen.md` (neu) · Status: eingepflegt

## Warum

Der Kern des Plannings: Features in die Zellen Team × Iteration legen und umhängen, so schnell wie Klebezettel.

## Entscheide

- **Einplanung als Wert am Feature** (Team und Iteration), ein Feature liegt in höchstens einer Zelle (ADR 0005,
  ADR 0006)
- **IP-Iteration gesperrt:** Dort wird keine Feature-Arbeit geplant; der Server lehnt ab, die Oberfläche lässt das
  Ablegen gar nicht zu
- **Drag & Drop mit Angular CDK:** `@angular/cdk` kommt in diesem PR mit (Entscheid des Nutzers im
  interaktiven Lauf; `package.json` gehört nie in einen Nachtlauf, CI setzt `needs-human-review`)
- **Bedienung ohne Maus:** Jede Karte hat zusätzlich eine Aktion «Einplanen …», die ein Inline-Formular an der Karte
  aufklappt (Auswahl Team und Iteration ohne IP-Iteration, «Einplanen», «Abbrechen», bei eingeplanten Karten «Zurück
  ins Backlog»); native Auswahlfelder, kein Dialog
- **Optimistisch mit Rücknahme:** Die Karte springt sofort, bei einem Fehler zurück, mit Meldung
- **Team entfernen:** Seine eingeplanten Features kommen in allen PIs zurück ins Backlog (ergänzt die Team-Story
  #6 um einen Fall, den es dort noch nicht gab). Die Bestätigung beim Entfernen weist mit festem Text darauf hin;
  eine Zählung der betroffenen Features ist bewusst nicht Teil dieser Story
- **Iteration über ihre Nummer im PI:** Iterationen werden nicht gespeichert, sondern aus dem PI berechnet
  (`Iteration`); die Einplanung nennt Team und Nummer. Eine Iteration «eines anderen PI» ist darum eine Nummer, die
  es im PI nicht gibt (Plan)
- **Reihenfolge in der Zelle:** wie im Backlog nach WSJF-Rang (`entscheide.md`)
- **Nicht in dieser Story:** gleichzeitiges Arbeiten mehrerer Personen (M2), Reihenfolge innerhalb einer Zelle

## Neu

### AK: Ein Feature lässt sich aus dem Backlog in eine Zelle Team × Iteration ziehen und ist danach dort eingeplant

### AK: Ein eingeplantes Feature lässt sich in eine andere Zelle verschieben oder zurück ins Backlog ziehen

### AK: In die IP-Iteration lässt sich kein Feature einplanen; der Versuch wird mit einer verständlichen Meldung abgelehnt

### AK: Jede Karte lässt sich auch ohne Maus über die Aktion «Einplanen …» einplanen, verschieben und zurücknehmen

### AK: Schlägt das Speichern fehl, springt die Karte an ihren alten Ort zurück und eine Meldung erscheint

### AK: Wird ein Team entfernt, kommen seine eingeplanten Features in allen PIs zurück ins Backlog

### AK: Die Bestätigung beim Entfernen eines Teams weist darauf hin, dass seine eingeplanten Features in allen PIs zurück ins Backlog gehen

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

### Szenario: Einplanen in eine Iteration, die es im PI nicht gibt, wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich "Portal" bei "Adler" in die Iteration Nummer 7 einplane
- **Dann** wird das Einplanen mit der Meldung "Die Iteration gehört nicht zu diesem PI." abgelehnt

### Szenario: Einplanen ohne Team und Iteration wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich "Portal" ohne Team und Iteration einplane
- **Dann** wird das Einplanen mit der Meldung "Team und Iteration sind Pflicht." abgelehnt

### Szenario: Einplanen bei einem unbekannten Team wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich "Portal" bei einem unbekannten Team in "Iteration 2" einplane
- **Dann** antwortet der Server mit 404 und der Meldung "Das Team gibt es nicht."

### Szenario: Bearbeiten eines eingeplanten Features lässt die Einplanung stehen
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich "Portal" in "Portal neu" umbenenne
- **Dann** liegt "Portal neu" auf dem Board bei "Adler" in "Iteration 2"

### Szenario: Entferntes Team gibt seine Features ins Backlog zurück
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich das Team "Adler" entferne
- **Dann** liegt "Portal" im Backlog des Boards

### Szenario: Entferntes Team gibt seine Features in allen PIs zurück
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Und** im PI "PI 27.2" das Feature "Suche" bei "Adler" in "Iteration 3" eingeplant
- **Wenn** ich das Team "Adler" entferne
- **Dann** liegt "Portal" im Backlog des Boards
- **Und** liegt "Suche" im Backlog des Boards

### Szenario: Karte wird ohne Maus eingeplant und zurückgenommen
- **Gegeben sei** das Feature "Portal" im Backlog des Boards
- **Wenn** ich bei "Portal" die Aktion "Einplanen …" öffne
- **Und** ich das Team "Adler" und "Iteration 2" wähle und "Einplanen" auslöse
- **Dann** wird "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Und** die IP-Iteration steht nicht zur Auswahl
- **Und** bei "Portal" gibt es die Aktion "Zurück ins Backlog"

### Szenario: Gescheitertes Speichern nimmt die Karte zurück
- **Gegeben sei** das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Wenn** ich "Portal" bei "Zebra" in "Iteration 3" einplane und das Speichern fehlschlägt
- **Dann** liegt "Portal" wieder bei "Adler" in "Iteration 2"
- **Und** eine Meldung erscheint über dem Board

### Szenario: Bestätigung beim Entfernen eines Teams weist auf zurückgehende Features hin
- **Gegeben sei** das Team "Adler"
- **Wenn** ich das Team "Adler" entfernen will
- **Dann** weist die Bestätigung darauf hin, dass seine eingeplanten Features in allen PIs zurück ins Backlog gehen

## Geändert

keine — das Entfernen eines Teams mit eingeplanten Features ist ein neuer Fall; die Szenarien der Team-Story
(#6) behalten ihr Ergebnis. Die geänderte Bestätigung steht im Delta US-6 unten

## Entfernt

keine

# Delta US-6 — Teams des ART erfassen (Issue #13)

Story: `spec/user-stories/6-teams-erfassen.md` (bestehend) · Status: eingepflegt

## Warum

Mit #13 hängt am Team Planung: Wer ein Team entfernt, nimmt dessen Features vom Board. Die Bestätigung sagt das,
damit Planung nicht unbemerkt verloren geht.

## Entscheide

- **Fester Hinweistext in der Bestätigung, keine Zählung** der betroffenen Features (`entscheide.md`)

## Neu

keine

## Geändert

### AK: Ein Team lässt sich nach Bestätigung entfernen und erscheint danach in keinem PI mehr
- **Vorher:** 5. Ein Team lässt sich nach Bestätigung entfernen und erscheint danach in keinem PI mehr
- **Nachher:** 5. Ein Team lässt sich nach Bestätigung entfernen und erscheint danach in keinem PI mehr; die
  Bestätigung weist darauf hin, dass seine eingeplanten Features in allen PIs zurück ins Backlog gehen
- **Grund:** Die Bestätigung zeigt mehr als «Wirklich entfernen?»; das Zurückgeben selbst belegt US-13
- **Tests:** `teams.component.spec.ts` bekommt einen Test für den Hinweistext; die bestehenden Prüfungen auf
  «Wirklich entfernen?» bleiben gültig

## Entfernt

keine
