# Delta US-12 — Program Board eines PI anzeigen (Issue #12)

Story: `spec/user-stories/12-program-board-anzeigen.md` (neu) · Status: eingepflegt

## Warum

Das Program Board ersetzt die Klebezettel-Wand. Diese Story liefert das Raster als Anzeige; Einplanen,
Kapazität und Abhängigkeiten bauen darauf auf.

## Entscheide

- **Raster:** Zeilen sind die Teams (alphabetisch), Spalten die Iterationen des PI in zeitlicher Reihenfolge, die
  IP-Iteration als letzte Spalte sichtbar abgesetzt
- **Backlog-Spalte** links neben dem Raster mit den nicht eingeplanten Features, sortiert wie das Backlog (#10)
- **Karte** zeigt Titel, WSJF-Rang und Schätzung
- **Nur Anzeige:** Einplanen per Drag & Drop folgt mit #13
- **Ein Lese-Endpunkt** für das ganze Board (`GET /api/v1/program-increments/{id}/board`), damit die Oberfläche ein
  konsistentes Bild in einem Aufruf erhält; Schnitt im Plan
- **Breite:** Das Raster scrollt waagrecht in seinem eigenen Container; Mobile ist für das Board kein Ziel in M1

## Neu

### AK: Das Board eines PI zeigt ein Raster mit einer Zeile pro Team (alphabetisch) und einer Spalte pro Iteration (zeitlich), die IP-Iteration sichtbar abgesetzt

### AK: Spaltenköpfe zeigen Name und Zeitraum der Iteration (TT.MM.)

### AK: Eine Backlog-Spalte zeigt die nicht eingeplanten Features des PI als Karten mit Titel, WSJF-Rang und Schätzung, sortiert wie das Backlog

### AK: Ohne Teams zeigt das Board einen Hinweis mit Link zur Teamseite statt eines leeren Rasters

### AK: Das Board ist von der Detailseite des PI aus erreichbar

### Szenario: Board liefert Teams, Iterationen und Backlog
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Zebra"
- **Und** das Team "Adler"
- **Und** im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
- **Wenn** ich das Board von "PI 27.1" abrufe
- **Dann** hat das Board die Zeilen "Adler", "Zebra"
- **Und** hat das Board 6 Spalten, die letzte ist "IP"
- **Und** liegt "Portal" im Backlog des Boards

### Szenario: Board eines unbekannten PI
- **Wenn** ich das Board eines unbekannten PI abrufe
- **Dann** antwortet der Server mit 404 und der Meldung "Das PI gibt es nicht."

### Szenario: Board ohne Teams zeigt einen Hinweis
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** eine leere Teamliste
- **Wenn** ich das Board von "PI 27.1" öffne
- **Dann** sehe ich den Hinweis "Noch keine Teams. Erfasse zuerst die Teams des ART."

### Szenario: IP-Iteration ist auf dem Board abgesetzt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
- **Und** das Team "Adler"
- **Wenn** ich das Board von "PI 27.1" öffne
- **Dann** ist die Spalte "IP" als IP-Iteration markiert

## Geändert

keine

## Entfernt

keine
