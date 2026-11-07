# Delta US-10 — Backlog nach WSJF-Rang bearbeiten (Issue #10)

Story: `spec/user-stories/10-backlog-bearbeiten.md` (neu) · Status: entwurf

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

## Geändert

keine

## Entfernt

keine
