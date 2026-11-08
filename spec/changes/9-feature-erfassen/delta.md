# Delta US-9 — Feature im PI-Backlog erfassen (Issue #9)

Story: `spec/user-stories/9-feature-erfassen.md` (neu) · Status: eingepflegt

## Warum

Features sind das, was auf dem Program Board eingeplant wird. Ohne erfasste Features gibt es nichts zu planen.

## Entscheide

- **Feature gehört zu einem PI** (ADR 0005); kein PI-übergreifendes Backlog in M1
- **WSJF-Rang** ist eine ganze Zahl ab 1 (1 = zuerst), optional und nicht eindeutig: Er stammt meist aus dem
  Issue-Tracker und wird nicht von PIckel berechnet
- **Owner** ist Freitext (Name), weil es noch keine Benutzer gibt
- **Schätzung** in Story Points, ganzzahlig ab 0, optional (ADR 0006)
- **Unbekanntes PI → 404** mit `{ "message": … }`: erste 404 der API; erweitert den Fehlervertrag aus ADR 0003 um
  einen Mapper für «nicht gefunden», gilt danach für alle Stories mit einer PI-ID im Pfad. Umgesetzt mit dem bestehenden
  `ProgramIncrementNotFoundExceptionMapper` (seit US-5), ein neuer Mapper war nicht nötig (`notes.md`)
- **Nicht in dieser Story:** Liste bearbeiten und löschen (#10), Import (#11)

## Neu

### AK: Ein Feature hat einen Titel; Pflicht, getrimmt, höchstens 200 Zeichen

### AK: Beschreibung (höchstens 5000 Zeichen), WSJF-Rang (ganze Zahl ab 1), Owner (höchstens 100 Zeichen) und Schätzung (ganze Zahl ab 0, Story Points) sind optional

### AK: Ein neues Feature gehört zum PI, in dem es erfasst wird, und ist noch nicht eingeplant

### AK: Das Formular zeigt fehlende oder ungültige Angaben am Feld an und legt dann nichts an

### AK: Das Backlog ist nach WSJF-Rang aufsteigend sortiert, Features ohne Rang am Ende, bei gleichem Rang nach Titel ohne Gross-/Kleinschreibung

Nachgetragen im Nachtlauf (Annahme, `notes.md`): Das Delta legte keine Reihenfolge fest.

### Szenario: Feature wird erfasst und erscheint im Backlog
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" das Feature "Self-Service-Portal" mit WSJF-Rang 1, Owner "Anna" und Schätzung 21 erfasse
- **Dann** erscheint "Self-Service-Portal" im Backlog von "PI 27.1"
- **Und** ist "Self-Service-Portal" nicht eingeplant

### Szenario: Feature nur mit Titel wird erfasst
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" das Feature "Login" ohne weitere Angaben erfasse
- **Dann** erscheint "Login" im Backlog von "PI 27.1"

### Szenario: Feature ohne Titel wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" das Feature " " erfasse
- **Dann** wird das Erfassen mit der Meldung "Der Titel darf nicht leer sein." abgelehnt

### Szenario: WSJF-Rang 0 wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" das Feature "Login" mit WSJF-Rang 0 erfasse
- **Dann** wird das Erfassen mit der Meldung "Der WSJF-Rang muss eine ganze Zahl ab 1 sein." abgelehnt

### Szenario: Negative Schätzung wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" das Feature "Login" mit Schätzung -1 erfasse
- **Dann** wird das Erfassen mit der Meldung "Die Schätzung muss eine ganze Zahl ab 0 sein." abgelehnt

### Szenario: Feature in einem unbekannten PI wird abgelehnt
- **Wenn** ich in einem unbekannten PI das Feature "Login" erfasse
- **Dann** antwortet der Server mit 404 und der Meldung "Das PI gibt es nicht."

### Szenario: Backlog ist nach WSJF-Rang und Titel sortiert
Nachgetragen im Nachtlauf (Annahme, `notes.md`).
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich im PI "PI 27.1" die Features "Suche" und "archiv" ohne Rang, "Bericht" mit Rang 10, "Zahlung" und
  "login" mit Rang 2 und "Export" mit Rang 1 erfasse
- **Dann** zeigt das Backlog von "PI 27.1" die Features "Export, login, Zahlung, Bericht, archiv, Suche"

### Szenario: Formular zeigt ungültige Angaben am Feld und legt nichts an
Nachgetragen im Nachtlauf, belegt AK 4 im Frontend (`@Unit`, Vitest).
- **Gegeben sei** das Backlog eines PI
- **Wenn** ich das Formular mit leerem Titel, WSJF-Rang 0 oder Schätzung -1 absende
- **Dann** steht die Meldung am betroffenen Feld
- **Und** es wird nichts angelegt

## Geändert

keine

## Entfernt

keine
