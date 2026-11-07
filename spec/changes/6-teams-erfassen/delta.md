# Delta US-6 — Teams des ART erfassen (Issue #6)

Story: `spec/user-stories/6-teams-erfassen.md` (neu) · Status: entwurf

## Warum

Ohne Teams gibt es keine Zeilen auf dem Program Board und keine Kapazität. Teams gehören zum ART und werden einmal
erfasst, nicht in jedem PI neu (ADR 0005).

## Entscheide

- **Teams am ART:** ein impliziter ART, ein Team gilt für alle PIs (ADR 0005)
- **Name eindeutig** ohne Rücksicht auf Gross- und Kleinschreibung, damit auf dem Board keine zwei gleich lautenden
  Zeilen entstehen
- **Entfernen** fragt nach einer Bestätigung in der Oberfläche; was mit eingeplanten Features geschieht, regelt die
  Story «Feature auf dem Board einplanen» (#13)
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

### Szenario: Doppelter Teamname wird abgelehnt
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "rakete" erfasse
- **Dann** wird das Erfassen mit der Meldung "Ein Team mit diesem Namen gibt es schon." abgelehnt

### Szenario: Team wird umbenannt
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "Rakete" in "Komet" umbenenne
- **Dann** erscheint "Komet" in der Teamliste
- **Und** erscheint "Rakete" nicht mehr

### Szenario: Team wird entfernt
- **Gegeben sei** das Team "Rakete"
- **Wenn** ich das Team "Rakete" entferne
- **Dann** ist die Teamliste leer

### Szenario: Leerzustand ohne Teams
- **Gegeben sei** eine leere Teamliste
- **Wenn** ich die Teamseite öffne
- **Dann** sehe ich den Hinweis "Noch keine Teams. Erfasse das erste Team."

## Geändert

keine

## Entfernt

keine
