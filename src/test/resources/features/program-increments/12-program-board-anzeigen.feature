# language: de
Funktionalität: US-12 Program Board anzeigen

  Als Teilnehmerin oder Teilnehmer des PI Planning
  möchte ich das Program Board eines PI als Raster sehen
  damit die Klebezettel-Wand durch eine gemeinsame Ansicht ersetzt wird

  # Teams gelten für alle PIs; ohne Leeren stünden Teams anderer Szenarien als Zeilen auf dem Board.
  Grundlage:
    Gegeben sei eine leere Teamliste

  Szenario: Board liefert Teams, Iterationen und Backlog
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Zebra"
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Wenn ich das Board von "PI 27.1" abrufe
    Dann hat das Board die Zeilen "Adler", "Zebra"
    Und hat das Board 6 Spalten, die letzte ist "IP"
    Und liegt "Portal" im Backlog des Boards

  Szenario: Board eines unbekannten PI
    Wenn ich das Board eines unbekannten PI abrufe
    Dann antwortet der Server mit 404 und der Meldung "Das PI gibt es nicht."

  # Vitest: program-board.component.spec.ts → zeigt ohne Teams einen Hinweis mit Link zur Teamseite statt eines Rasters (US-12)
  @Unit
  Szenario: Board ohne Teams zeigt einen Hinweis
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und eine leere Teamliste
    Wenn ich das Board von "PI 27.1" öffne
    Dann sehe ich den Hinweis "Noch keine Teams. Erfasse zuerst die Teams des ART."

  # Vitest: program-board.component.spec.ts → markiert die Spalte der IP-Iteration als IP-Iteration (US-12)
  @Unit
  Szenario: IP-Iteration ist auf dem Board abgesetzt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Wenn ich das Board von "PI 27.1" öffne
    Dann ist die Spalte "IP" als IP-Iteration markiert
