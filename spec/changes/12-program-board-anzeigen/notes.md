# Notizen #12 — Program Board eines PI anzeigen

Nachtlauf, ohne `/flow-plan` und ohne `entscheide.md`; Annahmen hier, im PR unter «Annahmen im Nachtlauf».

- **Schnitt:** `BoardService` setzt das Board aus den Driving Ports `ProgramIncrementUseCase`, `TeamUseCase` und
  `FeatureUseCase` zusammen statt aus den Repositories, damit die Reihenfolgen (Teams alphabetisch, Backlog nach #10)
  und die 404-Prüfung genau an einem Ort bleiben. Kein `@Transactional`: nur lesend
- **Annahme Feature-Datei:** Grundlage «Gegeben sei eine leere Teamliste», weil Teams für alle PIs gelten und sonst
  Teams anderer Szenarien als Zeilen auf dem Board stünden. Die Szenarien selbst lauten wie im Delta
- **Annahme Karte:** Rang als «WSJF <n>», Schätzung als «<n> SP»; was fehlt, wird weggelassen
- **Annahme Fehler:** Unbekanntes PI oder Serverfehler zeigt die Meldung mit `role="alert"` wie auf der Detailseite
- **Annahme leeres Backlog / ohne Teams:** Die Backlog-Spalte bleibt sichtbar (auch leer, ohne eigenen Hinweis);
  ohne Teams steht der Hinweis an der Stelle des Rasters
- **Annahme Navigation:** Link «Program Board öffnen» unter dem Zeitraum auf der Detailseite; auf dem Board ein Link
  «← Zum PI» zurück
- **Annahme Rolle:** Das Delta nennt keine; die Story sagt «Teilnehmerin oder Teilnehmer des PI Planning», weil das
  Board die gemeinsame Ansicht aller ist
- **Reviewer (Angular):** Späte Antwort auf ein früher gewähltes PI überschrieb das Board; `BoardService` zählt die
  Ladeaufträge und übernimmt nur die Antwort auf den letzten, mit eigenem Vitest-Test
- **Reviewer (Tests):** Kartentest prüft jetzt «WSJF n» und «n SP» statt einzelner Ziffern, dazu ein Test für fehlenden
  Rang und fehlende Schätzung
- **Testumbau:** Der gemeinsame Step «antwortet der Server mit {int} und der Meldung {string}» in `FeatureSteps`
  prüft jetzt `context.lastResponse()`, damit Szenario 2 ihn wörtlich nutzen kann; jeder Request in `FeatureSteps`
  setzt die Antwort. Keine Prüfung entfällt
- **Nicht automatisch prüfbar:** «sichtbar abgesetzt» (Hintergrund der IP-Spalte) und waagrechtes Scrollen; jsdom
  rendert kein Layout. Getestet ist die Markierung «IP-Iteration» im Spaltenkopf
- Im Worktree fehlte `node_modules`; `test-author` hat einmal `npm ci` ausgeführt (Voraussetzung laut `flow.md`)
