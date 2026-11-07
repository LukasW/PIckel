# Delta US-6 — Teams des ART erfassen (Issue #6)

Story: `spec/user-stories/6-teams-erfassen.md` (neu) · Status: eingepflegt

## Warum

Ohne Teams gibt es keine Zeilen auf dem Program Board und keine Kapazität. Teams gehören zum ART und werden einmal
erfasst, nicht in jedem PI neu (ADR 0005).

## Entscheide

- **Teams am ART:** ein impliziter ART, ein Team gilt für alle PIs (ADR 0005)
- **Name eindeutig** ohne Rücksicht auf Gross- und Kleinschreibung, damit auf dem Board keine zwei gleich lautenden
  Zeilen entstehen
- **Entfernen** fragt nach einer Bestätigung in der Oberfläche; was mit eingeplanten Features geschieht, regelt die
  Story «Feature auf dem Board einplanen» (#13)
- **Alphabetisch** wie im Deutschen: ohne Rücksicht auf Gross- und Kleinschreibung, Umlaute beim Grundbuchstaben
  (Nachtlauf, `notes.md`)
- **Umbenennen** auf den eigenen Namen in anderer Schreibweise ist erlaubt; zuerst gilt «Team gibt es nicht», dann die
  Namensregeln (Nachtlauf, `notes.md`)
- **«In keinem PI mehr»:** Teams hängen am ART (ADR 0005), einen Bezug Team–PI gibt es noch nicht; belegt ist das
  Verschwinden aus der einzigen Teamliste, den Bezug zu eingeplanten Features belegt #13
- **REST:** `/api/v1/teams` mit GET, POST (201), PUT `/{id}` (200), DELETE `/{id}` (204); 400 und 404 mit Meldung
  wie bei PIs. Erreichbar über den Link «Teams» im Kopf, Route `/teams`
- **Feature-Datei:** Szenarien mit Daten beginnen dort zusätzlich mit «Gegeben sei eine leere Teamliste», weil sich
  die Szenarien die Datenbank teilen
- **Nicht in dieser Story:** Team-Mitglieder, Rollen, Reihenfolge der Teams (alphabetisch)

## Neu

### AK: Ein Team hat einen Namen; Pflicht, getrimmt, höchstens 50 Zeichen

### AK: Zwei Teams können nicht gleich heissen, auch nicht in anderer Gross- und Kleinschreibung

### AK: Die Teamliste ist alphabetisch nach Name sortiert

### AK: Ein Team lässt sich umbenennen, mit denselben Regeln wie beim Anlegen

### AK: Ein Team lässt sich nach Bestätigung entfernen und erscheint danach in keinem PI mehr

### AK: Ohne Teams zeigt die Seite einen Leerzustand mit dem Hinweis, das erste Team zu erfassen

### Szenario: Team wird erfasst und erscheint in der Liste
- **Gegeben sei** eine leere Teamliste
- **Wenn** ich das Team "Rakete" erfasse
- **Dann** erscheint "Rakete" in der Teamliste

### Szenario: Teamliste ist alphabetisch sortiert
- **Gegeben sei** das Team "Zebra"
- **Und** das Team "Adler"
- **Dann** zeigt die Teamliste zuerst "Adler" und dann "Zebra"

### Szenario: Leerer Teamname wird abgelehnt
- **Wenn** ich das Team " " erfasse
- **Dann** wird das Erfassen mit der Meldung "Der Teamname darf nicht leer sein." abgelehnt

### Szenario: Zu langer Teamname wird abgelehnt
- **Wenn** ich ein Team mit 51 Zeichen im Namen erfasse
- **Dann** wird das Erfassen mit der Meldung "Der Teamname darf höchstens 50 Zeichen lang sein." abgelehnt

### Szenario: Doppelter Teamname wird abgelehnt
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "rakete" erfasse
- **Dann** wird das Erfassen mit der Meldung "Ein Team mit diesem Namen gibt es schon." abgelehnt

### Szenario: Team wird umbenannt
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "Rakete" in "Komet" umbenenne
- **Dann** erscheint "Komet" in der Teamliste
- **Und** erscheint "Rakete" nicht mehr in der Teamliste

### Szenario: Team wird entfernt
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "Rakete" entferne
- **Dann** ist die Teamliste leer

### Szenario: Umbenennen lässt die anderen Teams unverändert
- **Gegeben sei** das Team "Rakete"
- **Und** das Team "Zebra"
- **Wenn** ich das Team "Rakete" in "Komet" umbenenne
- **Dann** zeigt die Teamliste zuerst "Komet" und dann "Zebra"

### Szenario: Entfernen lässt die anderen Teams stehen
- **Gegeben sei** das Team "Rakete"
- **Und** das Team "Zebra"
- **Wenn** ich das Team "Rakete" entferne
- **Dann** erscheint "Zebra" in der Teamliste
- **Und** erscheint "Rakete" nicht mehr in der Teamliste

### Szenario: Umbenennen antwortet mit dem umbenannten Team
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "Rakete" in " Komet " umbenenne
- **Dann** antwortet das Umbenennen mit dem Team "Komet"

### Szenario: Umbenennen auf den Namen eines anderen Teams wird abgelehnt
- **Gegeben sei** das Team "Rakete"
- **Und** das Team "Komet"
- **Wenn** ich das Team "Rakete" in "KOMET" umbenenne
- **Dann** wird die Änderung mit dem Status 400 und der Meldung "Ein Team mit diesem Namen gibt es schon." abgelehnt

### Szenario: Entfernen antwortet ohne Inhalt
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "Rakete" entferne
- **Dann** antwortet das Entfernen ohne Inhalt

### Szenario: Unbekanntes Team lässt sich nicht umbenennen
- **Wenn** ich ein Team mit unbekannter oder ungültiger ID umbenenne
- **Dann** wird die Änderung mit dem Status 404 und der Meldung "Das Team gibt es nicht." abgelehnt

### Szenario: Unbekanntes Team lässt sich nicht entfernen
- **Wenn** ich ein Team mit unbekannter oder ungültiger ID entferne
- **Dann** wird die Änderung mit dem Status 404 und der Meldung "Das Team gibt es nicht." abgelehnt

### Szenario: Entfernen verlangt eine Bestätigung
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "Rakete" entfernen will
- **Dann** bleibt "Rakete" in der Teamliste, bis ich das Entfernen bestätige

### Szenario: Leerzustand ohne Teams
- **Gegeben sei** eine leere Teamliste
- **Wenn** ich die Teamseite öffne
- **Dann** sehe ich den Hinweis "Noch keine Teams. Erfasse das erste Team."

## Geändert

keine

## Entfernt

keine
