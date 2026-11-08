# Notizen — Issue #14

- **US-12-Test angepasst:** `program-board.component.spec.ts` → «lässt auf der Karte fehlenden Rang und fehlende
  Schätzung weg (US-12)» erwartete den Kartentext genau «Suche». Die Erwartung stammte aus einer Nachtlauf-Annahme
  (`spec/changes/12-program-board-anzeigen/notes.md`), nicht aus einem AK von US-12. Das bestätigte Delta US-14 (AK 5,
  `entscheide.md`: «auf jeder Karte ohne Schätzung, im Backlog wie in den Zellen») verlangt den Hinweis auf der Karte.
  Der Test erwartet jetzt «Suche ohne Schätzung», der fehlende Rang bleibt weggelassen
- **Überlast-Klassen über `[class]`:** `[class.bg-danger/10]` als Einzelbindung ist wegen des Schrägstrichs im
  Attributnamen heikel; deshalb ein `[class]`-Ausdruck, den Angular mit den statischen Klassen zusammenführt.
  Überlast trägt zusätzlich `border-2`, damit der rote Rand neben den 1-px-Rasterlinien sichtbar ist. Die
  Rahmenfarbe (`border-border` oder `border-danger`) steht nur im `[class]`-Ausdruck, damit nie beide Farben am
  selben Element hängen und die CSS-Reihenfolge entscheidet (Angular-Review)
- **Fehlender Kapazitätseintrag:** Der Server liefert jede Entwicklungs-Zelle (ohne Eintrag 0). `loadOf` gibt nur
  für Zellen mit Eintrag einen Wert; die IP-Iteration hat keinen, daher `undefined` ohne eigene IP-Prüfung im
  Frontend (Plan → «IP-Iteration»)
- **Schätzung `null`:** Der Server kann eine fehlende Schätzung als `null` liefern; `loadOf` und die Karte behandeln
  `null` wie «fehlt» (Test `zählt eine Schätzung, die der Server als null liefert, als 0 (US-14)`)
- **Reihenfolge der Kapazitäten im Board:** wie `CapacityUseCase.list` sie liefert; der Server sortiert nicht nach,
  das Frontend sucht über Team und Iteration
- **Cucumber-Step «das Team … mit der Kapazität … in … von …»:** legt das Team nur an, wenn es noch fehlt, und nimmt
  das PI aus dem Szenario-Kontext statt über den Namen (PI-Namen sind nicht eindeutig)
- **Tooling im Nachtlauf:** Die Test-Agenten durften Maven und npm nicht starten; Formatieren, Kompilieren und die
  Rot-Nachweise liefen in der Haupt-Session. Mutationsprobe `BoardService` (Kapazitäten → `List.of()`) macht
  `boardCarriesCapacitiesOfProgramIncrementAsDeliveredByUseCase` rot
