# Delta US-5 — Iterationen eines PI mit IP-Iteration festlegen (Issue #5)

Story: `spec/user-stories/5-iterationen-festlegen.md` (neu) · Status: entwurf

## Warum

Das Program Board braucht die Iterationen eines PI als Spalten. Heute hat ein PI nur einen Zeitraum. Wird der
Zeitraum aus einer festen Länge aufgeteilt, entstehen weder Lücken noch Überlappungen.

## Entscheide

- **Iterationen aus fester Länge:** Der RTE wählt beim Anlegen die Iterationslänge in Wochen, PIkel teilt den
  Zeitraum ab dem Startdatum auf, die letzte Iteration ist die IP-Iteration (ADR 0006)
- **Standard 2 Wochen**, erlaubt 1 bis 4 Wochen. Fehlt die Länge im Request, gilt der Standard; so bleibt der
  bestehende Vertrag von US-1 gültig
- **Rest:** Die letzte Iteration endet immer am Enddatum des PI und ist deshalb kürzer, wenn der Zeitraum nicht
  aufgeht
- **Nicht in dieser Story:** Iterationslänge eines bestehenden PI ändern, Iterationen einzeln anpassen (Feiertage)
- **Ort:** neue Detailseite des PI, aus der Liste erreichbar; die Board-Stories bauen darauf auf

## Neu

### AK: Beim Anlegen eines PI ist die Iterationslänge in Wochen wählbar, Standard 2, erlaubt 1 bis 4

### AK: Die Iterationen folgen lückenlos ab dem Startdatum; jede endet am Tag vor dem Beginn der nächsten, die letzte am Enddatum des PI

### AK: Die letzte Iteration ist die IP-Iteration und heisst «IP», die übrigen heissen «Iteration 1», «Iteration 2» usw.

### AK: Ein Zeitraum, der weniger als zwei Iterationen ergibt, wird mit einer verständlichen Meldung abgelehnt

### AK: Die Detailseite eines PI zeigt seine Iterationen mit Name, Start- und Enddatum (TT.MM.JJJJ); die IP-Iteration ist sichtbar markiert

### Szenario: Iterationen werden aus der Iterationslänge erzeugt
- **Gegeben sei** eine leere Liste der PIs
- **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen anlege
- **Dann** hat das PI 6 Iterationen
- **Und** beginnt "Iteration 1" am "2027-01-04" und endet am "2027-01-17"
- **Und** beginnt "IP" am "2027-03-15" und endet am "2027-03-26"

### Szenario: Ohne Iterationslänge gelten 2 Wochen
- **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" ohne Iterationslänge anlege
- **Dann** hat das PI 6 Iterationen

### Szenario: Letzte Iteration ist bei einem Rest kürzer
- **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-02-07" mit Iterationen zu 2 Wochen anlege
- **Dann** hat das PI 3 Iterationen
- **Und** beginnt "IP" am "2027-02-01" und endet am "2027-02-07"

### Szenario: Iterationslänge ausserhalb von 1 bis 4 Wochen wird abgelehnt
- **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 5 Wochen anlege
- **Dann** wird das Anlegen mit der Meldung "Die Iterationslänge muss zwischen 1 und 4 Wochen liegen." abgelehnt

### Szenario: Zeitraum für weniger als zwei Iterationen wird abgelehnt
- **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-01-10" mit Iterationen zu 2 Wochen anlege
- **Dann** wird das Anlegen mit der Meldung "Der Zeitraum reicht nicht für mindestens zwei Iterationen." abgelehnt

### Szenario: Detailseite zeigt die Iterationen mit markierter IP-Iteration
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Wenn** ich die Detailseite von "PI 27.1" öffne
- **Dann** sehe ich 6 Iterationen mit Start- und Enddatum im Format TT.MM.JJJJ
- **Und** ist "IP" als IP-Iteration markiert

## Geändert

keine — die Iterationslänge ist im Request optional, die bestehenden Szenarien von US-1 behalten ihr Ergebnis.
`/flow-plan` prüft, ob die zusätzlichen Felder in der Antwort von `POST /api/v1/program-increments` ein Eintrag
unter «Geändert» für US-1 sind (`story-delta.md` → «Neu, geändert oder entfernt»).

## Entfernt

keine
