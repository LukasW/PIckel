# Delta US-5 — Iterationen eines PI mit IP-Iteration festlegen (Issue #5)

Story: `spec/user-stories/5-iterationen-festlegen.md` (neu) · Status: eingepflegt

## Warum

Das Program Board braucht die Iterationen eines PI als Spalten. Heute hat ein PI nur einen Zeitraum. Wird der
Zeitraum aus einer festen Länge aufgeteilt, entstehen weder Lücken noch Überlappungen.

## Entscheide

- **Iterationen aus fester Länge:** Der RTE wählt beim Anlegen die Iterationslänge in Wochen, PIckel teilt den
  Zeitraum ab dem Startdatum auf, die letzte Iteration ist die IP-Iteration (ADR 0006)
- **Standard 2 Wochen**, erlaubt 1 bis 4 Wochen. Fehlt die Länge im Request, gilt der Standard; so bleibt der
  bestehende Vertrag von US-1 gültig
- **Rest:** Die letzte Iteration endet immer am Enddatum des PI und ist deshalb kürzer, wenn der Zeitraum nicht
  aufgeht
- **Nicht in dieser Story:** Iterationslänge eines bestehenden PI ändern, Iterationen einzeln anpassen (Feiertage)
- **Ort:** neue Detailseite des PI, aus der Liste erreichbar; die Board-Stories bauen darauf auf
- **Lese-Endpunkt für ein PI:** `GET /api/v1/program-increments/{id}` liefert das PI mit Iterationen; eine unbekannte
  ID ergibt 404 mit `{ "message": … }` (ADR 0003) (Rückfrage im Plan, 7. November 2026)
- **Berechnet, nicht gespeichert:** Gespeichert wird nur die Iterationslänge, die Iterationen berechnet die Domain.
  Spätere Stories (#7, #13) verweisen auf eine Iteration über PI und Nummer; das bleibt stabil, weil Länge und
  Zeitraum eines PI in M1 nicht änderbar sind (Rückfrage im Plan, 7. November 2026)

## Neu

### AK: Beim Anlegen eines PI ist die Iterationslänge in Wochen wählbar, Standard 2, erlaubt 1 bis 4

### AK: Die Iterationen folgen lückenlos ab dem Startdatum; jede endet am Tag vor dem Beginn der nächsten, die letzte am Enddatum des PI

### AK: Die letzte Iteration ist die IP-Iteration und heisst «IP», die übrigen heissen «Iteration 1», «Iteration 2» usw.

### AK: Ein Zeitraum, der weniger als zwei Iterationen ergibt, wird mit einer verständlichen Meldung abgelehnt

### AK: Die Detailseite eines PI zeigt seine Iterationen mit Name, Start- und Enddatum (TT.MM.JJJJ); die IP-Iteration ist sichtbar markiert

### AK: Ein unbekanntes PI wird mit einer verständlichen Meldung abgelehnt

### Szenario: Iterationen werden aus der Iterationslänge erzeugt
- **Gegeben sei** eine leere Liste der PIs
- **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen anlege
- **Dann** hat das PI 6 Iterationen
- **Und** beginnt "Iteration 1" am "2027-01-04" und endet am "2027-01-17"
- **Und** beginnt "IP" am "2027-03-15" und endet am "2027-03-26"

### Szenario: Iterationen folgen der gewählten Länge
- **Gegeben sei** eine leere Liste der PIs
- **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 3 Wochen anlege
- **Dann** hat das PI 4 Iterationen
- **Und** beginnt "IP" am "2027-03-08" und endet am "2027-03-26"

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

### Szenario: Unbekanntes PI wird nicht gefunden
- **Wenn** ich ein PI mit einer unbekannten ID abfrage
- **Dann** wird die Abfrage mit dem Status 404 und der Meldung "Das PI gibt es nicht." abgelehnt

### Szenario: PI mit ungültiger ID wird nicht gefunden
- **Wenn** ich das PI mit der ID "keine-uuid" abfrage
- **Dann** wird die Abfrage mit dem Status 404 und der Meldung "Das PI gibt es nicht." abgelehnt

### Szenario: Detailseite zeigt die Iterationen mit markierter IP-Iteration
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Wenn** ich die Detailseite von "PI 27.1" öffne
- **Dann** sehe ich 6 Iterationen mit Start- und Enddatum im Format TT.MM.JJJJ
- **Und** ist "IP" als IP-Iteration markiert

## Geändert

keine (die Änderungen an US-1 stehen unten unter «Delta US-1»)

## Entfernt

keine

# Delta US-1 — PI anlegen: Antwort mit Iterationen und `Location` (Issue #5)

Story: `spec/user-stories/1-pi-anlegen.md` (bestehend) · Status: eingepflegt

## Warum

Mit US-5 trägt jedes PI eine Iterationslänge und Iterationen, und es gibt einen Endpunkt für ein einzelnes PI. Damit
ändert sich, was `POST` und `GET` von US-1 zurückgeben, auch wenn jede bisher gültige Anfrage gültig bleibt.

## Entscheide

- **Iterationslänge optional** im Request (Standard 2 Wochen, Delta US-5): Die Szenarien von US-1 behalten Wortlaut
  und Ergebnis
- **`Location`-Header bei 201:** Der Grund für das Weglassen («es gibt keinen Endpunkt für ein einzelnes PI») fällt
  mit US-5 weg (Rückfrage im Plan, 7. November 2026)

## Neu

keine

## Geändert

### Szenario: PI wird angelegt und erscheint in der Liste
- **Vorher:**
  - **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" anlege
  - **Dann** wird das PI mit einer ID angelegt
  - **Und** erscheint "PI 27.1" in der Liste der PIs
- **Nachher:**
  - **Wenn** ich das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" anlege
  - **Dann** wird das PI mit einer ID angelegt
  - **Und** verweist die Antwort im Header `Location` auf das neue PI
  - **Und** erscheint "PI 27.1" in der Liste der PIs
- **Grund:** `POST` antwortet mit `Location`; `POST` und `GET` liefern zusätzlich `iterationLengthWeeks` und
  `iterations` (AK 1 bleibt im Wortlaut, die Antwort enthält mehr)
- **Tests:** `ProgramIncrementSteps` (neuer Schritt für den Header), Kommentar und Antwort in
  `ProgramIncrementResource`; Testdaten in `program-increments.service.spec.ts` und
  `program-increments.component.spec.ts` um die neuen Felder ergänzt

## Entfernt

keine
