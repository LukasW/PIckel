# Delta US-10 — Backlog nach WSJF-Rang bearbeiten (Issue #10)

Story: `spec/user-stories/10-backlog-bearbeiten.md` (neu) · Status: eingepflegt

## Warum

Das Planning startet mit einer priorisierten Liste. Sie muss sich bis zuletzt korrigieren lassen, ohne den Weg über
den Issue-Tracker.

## Entscheide

- **Sortierung:** WSJF-Rang aufsteigend, Features ohne Rang am Ende; bei gleichem Rang alphabetisch nach Titel,
  damit die Reihenfolge stabil ist
- **Bearbeiten** mit denselben Regeln wie beim Erfassen (#9)
- **Löschen** nach Bestätigung in der Oberfläche; was mit Abhängigkeiten eines gelöschten Features geschieht, regelt
  #16
- **Nicht in dieser Story:** Rang per Drag & Drop in der Liste ändern, Filter und Suche

## Neu

### AK: Das Backlog eines PI listet seine Features mit Titel, WSJF-Rang, Owner und Schätzung, aufsteigend nach WSJF-Rang

### AK: Features ohne WSJF-Rang stehen am Ende; bei gleichem Rang entscheidet der Titel alphabetisch

### AK: Ein Feature lässt sich bearbeiten, mit denselben Regeln wie beim Erfassen

### AK: Ein Feature lässt sich nach Bestätigung löschen

### AK: Ohne Features zeigt das Backlog einen Leerzustand mit dem Hinweis, das erste Feature zu erfassen

### Szenario: Backlog ist nach WSJF-Rang sortiert, ohne Rang am Ende
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Und** im PI "PI 27.1" das Feature "Suche" mit WSJF-Rang 2
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Dann** zeigt das Backlog von "PI 27.1" die Features "Portal", "Suche", "Login" in dieser Reihenfolge

### Szenario: Gleicher Rang wird nach Titel sortiert
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Suche" mit WSJF-Rang 1
- **Und** im PI "PI 27.1" das Feature "Export" mit WSJF-Rang 1
- **Dann** zeigt das Backlog von "PI 27.1" die Features "Export", "Suche" in dieser Reihenfolge

### Szenario: Feature wird bearbeitet
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Wenn** ich beim Feature "Login" den WSJF-Rang auf 3 und die Schätzung auf 8 setze
- **Dann** hat "Login" den WSJF-Rang 3 und die Schätzung 8

### Szenario: Bearbeiten mit leerem Titel wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Wenn** ich beim Feature "Login" den Titel auf " " setze
- **Dann** wird das Bearbeiten mit der Meldung "Der Titel darf nicht leer sein." abgelehnt

### Szenario: Feature wird gelöscht
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Und** im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
- **Wenn** ich das Feature "Login" lösche
- **Dann** ist das Backlog von "PI 27.1" leer

### Szenario: Leerzustand ohne Features
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich das Backlog von "PI 27.1" öffne
- **Dann** sehe ich den Hinweis "Noch keine Features. Erfasse das erste Feature oder importiere eine CSV-Datei."

### AK: Ein unbekanntes Feature oder eines aus einem anderen PI lässt sich unter diesem PI weder bearbeiten noch löschen (404 mit Meldung)

Nachgetragen im Nachtlauf (Annahme, `notes.md`): Das Delta nannte die Fehlerfälle nicht; Muster wie US-6
«Unbekanntes Team».

### Szenario: Unbekanntes Feature lässt sich nicht bearbeiten
Nachgetragen im Nachtlauf.
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" das Feature mit einer unbekannten oder ungültigen ID bearbeite
- **Dann** antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."

### Szenario: Unbekanntes Feature lässt sich nicht löschen
Nachgetragen im Nachtlauf.
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" das Feature mit einer unbekannten oder ungültigen ID lösche
- **Dann** antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."

### Szenario: Feature eines anderen PI lässt sich dort nicht bearbeiten
Nachgetragen im Nachtlauf.
- **Gegeben sei** die PIs "PI 27.1" und "PI 27.2" und im PI "PI 27.1" das Feature "Login"
- **Wenn** ich das Feature "Login" im PI "PI 27.2" bearbeite
- **Dann** antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."
- **Und** erscheint "Login" weiterhin im Backlog von "PI 27.1"

### Szenario: Feature eines anderen PI lässt sich dort nicht löschen
Nachgetragen im Nachtlauf.
- **Gegeben sei** die PIs "PI 27.1" und "PI 27.2" und im PI "PI 27.1" das Feature "Login"
- **Wenn** ich das Feature "Login" im PI "PI 27.2" lösche
- **Dann** antwortet der Server mit 404 und der Meldung "Das Feature gibt es nicht."
- **Und** erscheint "Login" weiterhin im Backlog von "PI 27.1"

### Szenario: Löschen verlangt eine Bestätigung
Nachgetragen im Nachtlauf, belegt AK 4 im Frontend (`@Unit`, Vitest).
- **Gegeben sei** das Backlog eines PI mit dem Feature "Login"
- **Wenn** ich beim Feature "Login" auf «Löschen» klicke
- **Dann** wird noch nichts gelöscht, erst «Wirklich löschen» löscht das Feature; «Abbrechen» behält es

## Geändert

keine

## Entfernt

keine
