# Entscheide — Issue #14

## Geklärte Entscheidungen
- **Ort der Berechnung:** Der Server liefert mit dem Board pro Zelle nur die verfügbare Kapazität. Das Frontend
  leitet «geplant» und «Überlast» als `computed` aus den Karten im Board-Signal ab (Entscheid des Nutzers im
  Verhör; ersetzt den Entwurf «Berechnung in der Domain»). Begründung: Das Board aus #13 plant optimistisch ein
  und lädt nach einem Drop nichts neu. Nur aus dem Board-Signal abgeleitete Werte stimmen sofort nach dem
  Einplanen, Verschieben und Zurücknehmen (AK 4) und kehren bei einem Fehler mit der Karte zurück. Die Regel steht
  an einem Ort (TypeScript) und wird mit Vitest geprüft
- **Last einer Zelle:** Summe der Schätzungen ihrer Features; ein Feature ohne Schätzung zählt 0 (Quelle: ADR 0006)
- **Überlast:** geplant grösser als verfügbar, gleich viel ist keine Überlast (Quelle: Delta)
- **Verfügbar ohne Eintrag:** 0 (Quelle: ADR 0005, US-7 AK 3; `CapacityUseCase.list` füllt schon jede
  Entwicklungs-Iteration mit 0 auf)
- **IP-Iteration:** zeigt keine Kapazität (Quelle: ADR 0006, `Capacity.plan` lehnt sie ab)
- **Überlast nur anzeigen, nicht verhindern:** Einplanen über die Kapazität bleibt erlaubt (Quelle:
  `spec/changes/13-feature-einplanen/entscheide.md`)
- **Nicht nur Farbe:** rot und mit Text «Überlast» (Quelle: Delta)
- **Keine Migration:** Schätzung und Kapazität sind schon gespeichert
- **Testebenen:** CucumberIT prüft nur, dass das Board die Kapazität pro Zelle liefert (REST-Vertrag). Geplant,
  Überlast, «ohne Schätzung» und das Nachführen ohne Neuladen sind `@Unit` mit Vitest-Belegen (CLAUDE.md →
  «Unit ist der Default»)

## Bewusst draussen (Non-Goals)
- Überlast in der REST-API: Die API liefert kein «geplant» und kein «überlastet»; braucht ein anderer Konsument
  (Bericht, Export) das, kommt die Regel mit dieser Story in die Domain
- Summen pro Team über das PI oder pro Iteration über alle Teams
- Warnung oder Sperre beim Einplanen in eine volle Zelle
- Kapazität direkt auf dem Board bearbeiten (bleibt auf der Detailseite, US-7)
- Gleichzeitiges Arbeiten mehrerer Personen (M2)

## Verbleibende Annahmen
- Bei der Umsetzung festzulegen, im Plan zu bestätigen: `BoardService` holt die Kapazitäten über `CapacityUseCase`,
  `ProgramBoard` und `BoardDto` bekommen eine Liste `capacities` (Team, Iterationsnummer, Punkte; Form wie
  `CapacityDto`), das Frontend-Modell `Board` dieselbe
- Die Anzeige «geplant / verfügbar SP» steht oben in der Zelle, auch wenn die Zelle leer ist («0 / 0 SP»)
- Der Hinweis «ohne Schätzung» steht auf jeder Karte ohne Schätzung, im Backlog wie in den Zellen
- Eine Änderung der Kapazität auf der Detailseite zeigt das Board beim nächsten Öffnen (das Board lädt beim Aufruf)

## Auswirkung auf die Spezifikation
- Delta US-14: Entscheid «Berechnung in der Domain» ersetzt; Szenarien neu geschnitten (ein CucumberIT-Szenario
  für die Kapazität pro Zelle, die übrigen als `@Unit`), ein Szenario für das Nachführen ohne Neuladen ergänzt
- ADR 0006 bleibt gültig (die Regel ist dieselbe, nur der Ort der Berechnung ist ein Umsetzungsentscheid)
- Keine neue ADR
