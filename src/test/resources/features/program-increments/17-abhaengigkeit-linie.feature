# language: de
Funktionalität: US-17 Abhängigkeiten als Linie auf dem Board

  Als Teilnehmerin oder Teilnehmer des Plannings
  möchte ich Abhängigkeiten als Linie zwischen den Features auf dem Board sehen
  damit ich die Verflechtung der Teams auf einen Blick erkenne

  # JUnit: application/service/BoardServiceTest → boardCarriesDependenciesOfProgramIncrement
  Szenario: Board liefert die Abhängigkeiten
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und im PI "PI 27.1" das Feature "Portal" mit WSJF-Rang 1
    Und im PI "PI 27.1" das Feature "Login" ohne WSJF-Rang
    Und "Portal" braucht "Login"
    Wenn ich das Board von "PI 27.1" abrufe
    Dann liefert das Board die Abhängigkeit "Portal" braucht "Login"

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → zieht eine Linie von Login zu Portal, wenn beide eingeplant sind (US-17)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zieht eine Linie von Login zu Portal, wenn beide eingeplant sind (US-17)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zeichnet die Linie mit einem Pfeil und verbirgt die Linien vor Screenreadern (US-17)
  # Vitest: src/main/webapp/src/app/features/program-board/dependency-lines.spec.ts → führt von der rechten Kante der Lieferung zur linken Kante des Bedarfs, wenn der Bedarf rechts liegt (US-17)
  @Unit
  Szenario: Linie zwischen zwei eingeplanten Features
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 3" und "Login" bei "Zebra" in "Iteration 2"
    Und "Portal" braucht "Login"
    Wenn das Board die Linien berechnet
    Dann führt eine Linie von "Login" zu "Portal"

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → zählt an beiden Karten die Abhängigkeit ohne Linie, wenn ein Feature im Backlog liegt (US-17)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zieht keine Linie und zeigt an beiden Karten 1 Abhängigkeit, wenn Login im Backlog liegt (US-17)
  # Vitest: src/main/webapp/src/app/features/program-board/board-card.component.spec.ts → zeigt «1 Abhängigkeit» bei einer Abhängigkeit ohne Linie (US-17)
  @Unit
  Szenario: Keine Linie, wenn ein Feature im Backlog liegt
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 3" und "Login" im Backlog
    Und "Portal" braucht "Login"
    Wenn das Board die Linien berechnet
    Dann gibt es keine Linie
    Und zeigt die Karte "Portal" 1 Abhängigkeit
    Und zeigt die Karte "Login" 1 Abhängigkeit

  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → hebt bei Hover auf Suche die Linie Login → Suche hervor und blendet Login → Portal ab (US-17)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → hebt bei Tastaturfokus auf Suche dieselben Linien hervor wie Hover (US-17)
  # Vitest: src/main/webapp/src/app/features/program-board/dependency-lines.spec.ts → hebt bei Fokus auf «Suche» die Linie Login → Suche hervor und blendet Login → Portal ab (US-17)
  @Unit
  Szenario: Hover hebt die Linien einer Karte hervor
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 3", "Login" bei "Zebra" in "Iteration 2" und "Suche" bei "Zebra" in "Iteration 1"
    Und "Portal" braucht "Login"
    Und "Suche" braucht "Login"
    Wenn ich mit der Maus auf "Suche" zeige
    Dann ist die Linie von "Login" zu "Suche" hervorgehoben
    Und ist die Linie von "Login" zu "Portal" abgeblendet

  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zieht nach dem Einplanen per Aktion die Linie, ohne das Board neu zu laden (US-17)
  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → zieht nach dem Einplanen die Linie und zählt nichts mehr, ohne das Board neu zu laden (US-17)
  @Unit
  Szenario: Linie folgt dem Einplanen ohne Neuladen
    Gegeben sei ein Board mit "Portal" bei "Adler" in "Iteration 3" und "Login" im Backlog
    Und "Portal" braucht "Login"
    Wenn ich "Login" bei "Zebra" in "Iteration 2" einplane
    Dann führt eine Linie von "Login" zu "Portal"
    Und zeigt keine Karte eine Anzahl Abhängigkeiten
