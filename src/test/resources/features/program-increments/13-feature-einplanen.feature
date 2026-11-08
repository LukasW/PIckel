# language: de
Funktionalität: US-13 Feature einplanen

  Als Team-Mitglied
  möchte ich ein Feature per Drag & Drop in eine Zelle Team × Iteration legen, verschieben und zurück ins Backlog nehmen
  damit die Planung so schnell geht wie mit Klebezetteln

  # Teams gelten für alle PIs; ohne Leeren scheiterte «das Team "Adler"» am Namen aus einem anderen Szenario.
  Grundlage:
    Gegeben sei eine leere Teamliste

  Szenario: Feature wird in eine Zelle eingeplant
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Wenn ich "Portal" bei "Adler" in "Iteration 2" einplane
    Dann liegt "Portal" auf dem Board bei "Adler" in "Iteration 2"
    Und liegt "Portal" nicht mehr im Backlog des Boards

  Szenario: Eingeplantes Feature wird verschoben
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und das Team "Zebra"
    Und im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
    Wenn ich "Portal" bei "Zebra" in "Iteration 3" einplane
    Dann liegt "Portal" auf dem Board bei "Zebra" in "Iteration 3"

  Szenario: Eingeplantes Feature geht zurück ins Backlog
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
    Wenn ich "Portal" zurück ins Backlog nehme
    Dann liegt "Portal" im Backlog des Boards

  Szenario: Einplanen in die IP-Iteration wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Wenn ich "Portal" bei "Adler" in "IP" einplane
    Dann wird das Einplanen mit der Meldung "In der IP-Iteration wird keine Feature-Arbeit geplant." abgelehnt

  Szenario: Einplanen in eine Iteration, die es im PI nicht gibt, wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Wenn ich "Portal" bei "Adler" in die Iteration Nummer 7 einplane
    Dann wird das Einplanen mit der Meldung "Die Iteration gehört nicht zu diesem PI." abgelehnt

  Szenario: Einplanen ohne Team und Iteration wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Wenn ich "Portal" ohne Team und Iteration einplane
    Dann wird das Einplanen mit der Meldung "Team und Iteration sind Pflicht." abgelehnt

  Szenario: Einplanen bei einem unbekannten Team wird abgelehnt
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Wenn ich "Portal" bei einem unbekannten Team in "Iteration 2" einplane
    Dann antwortet der Server mit 404 und der Meldung "Das Team gibt es nicht."

  Szenario: Bearbeiten eines eingeplanten Features lässt die Einplanung stehen
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
    Wenn ich "Portal" in "Portal neu" umbenenne
    Dann liegt "Portal neu" auf dem Board bei "Adler" in "Iteration 2"

  Szenario: Entferntes Team gibt seine Features ins Backlog zurück
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
    Wenn ich das Team "Adler" entferne
    Dann liegt "Portal" im Backlog des Boards

  Szenario: Entferntes Team gibt seine Features in allen PIs zurück
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das PI "PI 27.2" vom "2027-04-05" bis "2027-06-25" mit Iterationen zu 2 Wochen
    Und das Team "Adler"
    Und im PI "PI 27.1" das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
    Und im PI "PI 27.2" das Feature "Suche" bei "Adler" in "Iteration 3" eingeplant
    Wenn ich das Team "Adler" entferne
    Dann liegt "Portal" im Backlog des Boards
    Und liegt "Suche" im Backlog des Boards

  # Vitest: src/main/webapp/src/app/features/program-board/board-card.component.spec.ts → meldet beim Einplanen das gewählte Team und die Nummer der Iteration (US-13)
  # Vitest: src/main/webapp/src/app/features/program-board/board-card.component.spec.ts → bietet nur die Entwicklungs-Iterationen an, nicht die IP-Iteration (US-13)
  # Vitest: src/main/webapp/src/app/features/program-board/board-card.component.spec.ts → bietet «Zurück ins Backlog» bei einer eingeplanten Karte an und meldet es (US-13)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → plant über die Aktion «Einplanen …» an der Karte ohne Maus ein (US-13)
  @Unit
  Szenario: Karte wird ohne Maus eingeplant und zurückgenommen
    Gegeben sei das Feature "Portal" im Backlog des Boards
    Wenn ich bei "Portal" die Aktion "Einplanen …" öffne
    Und ich das Team "Adler" und "Iteration 2" wähle und "Einplanen" auslöse
    Dann wird "Portal" bei "Adler" in "Iteration 2" eingeplant
    Und die IP-Iteration steht nicht zur Auswahl
    Und bei "Portal" gibt es die Aktion "Zurück ins Backlog"

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → nimmt das Einplanen bei einem Fehler zurück und zeigt die Meldung des Servers (US-13)
  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → springt beim Verschieben bei einem Fehler in die alte Zelle zurück (US-13)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zeigt die Meldung einer gescheiterten Aktion als Alarm über dem Board (US-13)
  @Unit
  Szenario: Gescheitertes Speichern nimmt die Karte zurück
    Gegeben sei das Feature "Portal" bei "Adler" in "Iteration 2" eingeplant
    Wenn ich "Portal" bei "Zebra" in "Iteration 3" einplane und das Speichern fehlschlägt
    Dann liegt "Portal" wieder bei "Adler" in "Iteration 2"
    Und eine Meldung erscheint über dem Board

  # Vitest: src/main/webapp/src/app/features/teams/teams.component.spec.ts → weist in der Bestätigung darauf hin, dass die eingeplanten Features in allen PIs ins Backlog zurückgehen (US-6)
  @Unit
  Szenario: Bestätigung beim Entfernen eines Teams weist auf zurückgehende Features hin
    Gegeben sei das Team "Adler"
    Wenn ich das Team "Adler" entfernen will
    Dann weist die Bestätigung darauf hin, dass seine eingeplanten Features in allen PIs zurück ins Backlog gehen
