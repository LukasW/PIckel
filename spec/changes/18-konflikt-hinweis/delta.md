# Delta US-18 — Hinweis bei Konflikt einer Abhängigkeit (Issue #18)

Story: `spec/user-stories/18-konflikt-hinweis.md` (neu) · Status: entwurf

## Warum

Eine Abhängigkeit ist nur dann ein Problem, wenn die Lieferung zu spät kommt. Das soll im Planning auffallen, nicht
erst in der Iteration.

## Entscheide

- **Konflikt** (Begriff aus der Spezifikation): Die Lieferung liegt in einer späteren Iteration als der Bedarf.
  Dieselbe Iteration ist kein Konflikt
- **Lieferung nicht eingeplant**, Bedarf eingeplant: kein Konflikt im Sinn der Spezifikation, aber ein eigener
  Hinweis «Lieferung nicht eingeplant», weil sonst genau die gefährlichste Lage unsichtbar bliebe
- **Berechnung in der Domain**, das Board liefert die Konflikte mit; die Regel steht an einem Ort und ist ohne
  Browser testbar
- **Darstellung:** Die Linie eines Konflikts ist rot und gestrichelt (nicht nur Farbe); eine Liste «Konflikte» neben
  dem Board nennt jeden Konflikt im Klartext

## Neu

### AK: Eine Abhängigkeit ist ein Konflikt, wenn die Lieferung in einer späteren Iteration eingeplant ist als der Bedarf

### AK: Ist der Bedarf eingeplant und die Lieferung nicht, zeigt das Board den Hinweis «Lieferung nicht eingeplant»

### AK: Die Linie eines Konflikts ist rot und gestrichelt

### AK: Eine Liste neben dem Board nennt jeden Konflikt und jede nicht eingeplante Lieferung im Klartext; ohne solche zeigt sie «Keine Konflikte»

### AK: Nach dem Einplanen, Verschieben oder Zurücknehmen stimmen Konflikte und Hinweise ohne Neuladen

### Szenario: Lieferung nach dem Bedarf ist ein Konflikt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** das Team "Zebra"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Und** im PI "PI 27.1" das Feature "Login" bei "Zebra" in "Iteration 4" eingeplant
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** meldet das Board den Konflikt "Login wird in Iteration 4 geliefert, Portal braucht es in Iteration 2."

### Szenario: Lieferung in derselben Iteration ist kein Konflikt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** das Team "Zebra"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Und** im PI "PI 27.1" das Feature "Login" bei "Zebra" in "Iteration 2" eingeplant
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** meldet das Board keine Konflikte

### Szenario: Nicht eingeplante Lieferung wird gemeldet
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** meldet das Board den Hinweis "Login ist nicht eingeplant, Portal braucht es in Iteration 2."

### Szenario: Verschieben löst den Konflikt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Und** das Team "Zebra"
- **Und** im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
- **Und** im PI "PI 27.1" das Feature "Login" bei "Zebra" in "Iteration 4" eingeplant
- **Und** "Portal" braucht "Login"
- **Wenn** ich "Login" bei "Zebra" in "Iteration 1" einplane
- **Dann** meldet das Board keine Konflikte

## Geändert

keine

## Entfernt

keine
