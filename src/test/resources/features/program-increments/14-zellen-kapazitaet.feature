# language: de
Funktionalität: US-14 Kapazität pro Zelle mit Überlast anzeigen
  Als Team-Mitglied möchte ich in jeder Zelle die geplanten gegenüber den verfügbaren Story Points sehen
  und Überlast sofort erkennen, damit wir uns nicht überplanen.

  # JUnit: application/service/BoardServiceTest → boardCarriesCapacitiesOfProgramIncrementAsDeliveredByUseCase
  Szenario: Board liefert die Kapazität pro Zelle
    Gegeben sei das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26" mit Iterationen zu 2 Wochen
    Und das Team "Adler" mit der Kapazität 20 in "Iteration 1" von "PI 27.1"
    Wenn ich das Board von "PI 27.1" abrufe
    Dann liefert das Board für "Adler" in "Iteration 1" die Kapazität 20
    Und liefert das Board für "Adler" in "Iteration 2" die Kapazität 0
    Und liefert das Board für die IP-Iteration keine Kapazität

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → zeigt geplant gegenüber verfügbar: 13 von 20, nicht überlastet (US-14)
  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → hat in der IP-Iteration keine Last (US-14)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zeigt in der Zelle «geplant / verfügbar SP» und keine Überlast (US-14)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zeigt in der IP-Spalte keine Kapazität (US-14)
  @Unit
  Szenario: Zelle zeigt geplant gegenüber verfügbar
    Gegeben sei das Team "Adler" mit der Kapazität 20 in "Iteration 1" auf dem Board
    Und das Feature "Portal" mit Schätzung 13 bei "Adler" in "Iteration 1"
    Wenn ich das Board öffne
    Dann zeigt die Zelle "Adler" × "Iteration 1" «13 / 20 SP»
    Und ist die Zelle "Adler" × "Iteration 1" nicht als Überlast markiert
    Und zeigt die IP-Iteration keine Kapazität

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → ist überlastet, wenn geplant grösser als verfügbar ist: 21 > 20 (US-14)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → hebt eine Zelle über der Kapazität rot hervor und beschriftet sie mit «Überlast» (US-14)
  @Unit
  Szenario: Zelle über der Kapazität ist überlastet
    Gegeben sei das Team "Adler" mit der Kapazität 20 in "Iteration 1" auf dem Board
    Und die Features "Portal" mit Schätzung 13 und "Suche" mit Schätzung 8 bei "Adler" in "Iteration 1"
    Wenn ich das Board öffne
    Dann zeigt die Zelle "Adler" × "Iteration 1" «21 / 20 SP»
    Und ist die Zelle rot hervorgehoben und mit «Überlast» beschriftet

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → ist bei genau ausgelasteter Zelle nicht überlastet: 13 = 13 (US-14)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → markiert eine genau ausgelastete Zelle nicht als Überlast (US-14)
  @Unit
  Szenario: Genau ausgelastete Zelle ist nicht überlastet
    Gegeben sei das Team "Adler" mit der Kapazität 13 in "Iteration 1" auf dem Board
    Und das Feature "Portal" mit Schätzung 13 bei "Adler" in "Iteration 1"
    Wenn ich das Board öffne
    Dann ist die Zelle "Adler" × "Iteration 1" nicht als Überlast markiert

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → zählt ein Feature ohne Schätzung als 0: «0 / 0» bei einem Team ohne Kapazität (US-14)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zählt ein Feature ohne Schätzung als 0 und zeigt auf der Karte «ohne Schätzung» (US-14)
  # Vitest: src/main/webapp/src/app/features/program-board/board-card.component.spec.ts → trägt auf einer Karte ohne Schätzung den Hinweis «ohne Schätzung» (US-14)
  @Unit
  Szenario: Feature ohne Schätzung zählt 0
    Gegeben sei das Team "Adler" ohne Kapazität auf dem Board
    Und das Feature "Login" ohne Schätzung bei "Adler" in "Iteration 1"
    Wenn ich das Board öffne
    Dann zeigt die Zelle "Adler" × "Iteration 1" «0 / 0 SP»
    Und trägt die Karte "Login" den Hinweis «ohne Schätzung»

  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → folgt dem Verschieben sofort, ohne neu zu laden (US-14)
  # Vitest: src/main/webapp/src/app/core/services/board.service.spec.ts → zeigt in beiden Zellen wieder die Werte von vorher, wenn das Verschieben scheitert (US-14)
  # Vitest: src/main/webapp/src/app/features/program-board/program-board.component.spec.ts → zeigt nach dem Verschieben die neuen Werte ohne Neuladen, nach einem Fehler wieder die alten (US-14)
  @Unit
  Szenario: Werte folgen dem Einplanen ohne Neuladen
    Gegeben sei das Team "Adler" mit der Kapazität 10 in "Iteration 1" und in "Iteration 2" auf dem Board
    Und das Feature "Portal" mit Schätzung 8 bei "Adler" in "Iteration 1"
    Wenn ich "Portal" nach "Adler" × "Iteration 2" verschiebe
    Dann zeigt die Zelle "Adler" × "Iteration 1" «0 / 10 SP» und die Zelle "Adler" × "Iteration 2" «8 / 10 SP»
    Und schlägt das Speichern fehl, zeigen beide Zellen wieder die Werte von vorher
