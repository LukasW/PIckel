# Delta US-18 — Hinweis bei Konflikt einer Abhängigkeit (Issue #18)

Story: `spec/user-stories/18-konflikt-hinweis.md` (neu) · Status: bestätigt

## Warum

Eine Abhängigkeit ist nur dann ein Problem, wenn die Lieferung zu spät kommt. Das soll im Planning auffallen, nicht
erst in der Iteration.

## Entscheide

- **Konflikt** (Begriff aus der Spezifikation): Die Lieferung liegt in einer späteren Iteration als der Bedarf.
  Dieselbe Iteration ist kein Konflikt
- **Lieferung nicht eingeplant**, Bedarf eingeplant: kein Konflikt im Sinn der Spezifikation, aber ein eigener
  Hinweis «Lieferung nicht eingeplant», weil sonst genau die gefährlichste Lage unsichtbar bliebe. Ist der Bedarf
  nicht eingeplant, gibt es weder Konflikt noch Hinweis
- **Ort der Berechnung: Frontend** (Rückfrage im Plan; ersetzt den Entwurf «Berechnung in der Domain»). Das Board
  aus #13 plant optimistisch ein und lädt danach nichts neu; nur aus dem Board-Signal abgeleitete Konflikte stimmen
  sofort nach dem Einplanen und kehren bei einem Fehler mit der Karte zurück (AK 5). Gleicher Grund wie bei der
  Kapazität in #14. Die Regel steht an einem Ort (TypeScript) und wird mit Vitest geprüft; die REST-API liefert keine
  Konflikte. Grundlage sind die Abhängigkeiten, die das Board seit #17 mitliefert
- **Darstellung:** Die Linie eines Konflikts ist rot und gestrichelt (nicht nur Farbe); eine Liste «Konflikte» nennt
  jeden Konflikt und jede nicht eingeplante Lieferung im Klartext. Den Hinweis «Lieferung nicht eingeplant» trägt
  zusätzlich die Karte des Bedarfs (Plan)
- **Testebenen:** Alle Szenarien sind `@Unit` mit Vitest-Belegen; es gibt keinen neuen Endpunkt und keine Persistenz

## Neu

### AK: Eine Abhängigkeit ist ein Konflikt, wenn die Lieferung in einer späteren Iteration eingeplant ist als der Bedarf

### AK: Ist der Bedarf eingeplant und die Lieferung nicht, zeigt die Karte des Bedarfs den Hinweis «Lieferung nicht eingeplant»

### AK: Die Linie eines Konflikts ist rot und gestrichelt

### AK: Eine Liste neben dem Board nennt jeden Konflikt und jede nicht eingeplante Lieferung im Klartext; ohne solche zeigt sie «Keine Konflikte»

### AK: Nach dem Einplanen, Verschieben oder Zurücknehmen stimmen Konflikte und Hinweise ohne Neuladen; scheitert das Speichern, gilt wieder der alte Stand

### Szenario: Lieferung nach dem Bedarf ist ein Konflikt
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 4"
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Board ansehe
- **Dann** nennt die Liste «Konflikte» "Login wird in Iteration 4 geliefert, Portal braucht es in Iteration 2."
- **Und** ist die Linie von "Login" zu "Portal" als Konflikt markiert

### Szenario: Lieferung in derselben Iteration ist kein Konflikt
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 2"
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Board ansehe
- **Dann** zeigt die Liste «Konflikte» "Keine Konflikte"
- **Und** ist die Linie von "Login" zu "Portal" nicht als Konflikt markiert

### Szenario: Nicht eingeplante Lieferung wird gemeldet
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" im Backlog
- **Und** "Portal" braucht "Login"
- **Wenn** ich das Board ansehe
- **Dann** nennt die Liste «Konflikte» "Login ist nicht eingeplant, Portal braucht es in Iteration 2."
- **Und** trägt die Karte "Portal" den Hinweis "Lieferung nicht eingeplant"

### Szenario: Verschieben löst den Konflikt
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 4"
- **Und** "Portal" braucht "Login"
- **Wenn** ich "Login" bei "Zebra" in "Iteration 1" einplane
- **Dann** zeigt die Liste «Konflikte» "Keine Konflikte"

### Szenario: Gescheitertes Speichern stellt den Konflikt wieder her
- **Gegeben sei** ein Board mit "Portal" bei "Adler" in "Iteration 2" und "Login" bei "Zebra" in "Iteration 4"
- **Und** "Portal" braucht "Login"
- **Und** das Speichern scheitert
- **Wenn** ich "Login" bei "Zebra" in "Iteration 1" einplane
- **Dann** nennt die Liste «Konflikte» "Login wird in Iteration 4 geliefert, Portal braucht es in Iteration 2."

## Geändert

keine

## Entfernt

keine
