# Nacharbeiten aus dem Playwright-Test (Issue #50)

## Übersicht

Ein explorativer Playwright-Test hat einen Fehler auf dem Program Board und mehrere Mängel in der Bedienung gezeigt.
Die Umsetzung kommt in **zwei PRs aus diesem Change-Ordner**:

- **PR 1 – Fehler beim Ablegen (US-13):** Wird eine Karte ausserhalb einer Ablage losgelassen, legt das CDK sie in
  der zuletzt überfahrenen Liste ab, weil `drop()` `isPointerOverContainer` nicht prüft. Über der IP-Iteration (keine
  Ablage) landet sie deshalb in der Nachbarzelle. Der Fix: Karte bleibt am Ort, über IP mit der Meldung aus AK 3
- **PR 2 – Bedienung (US-1, US-8, US-10, US-11, US-12, US-16):** Kopfzeile, Board-Kopf und leeres Backlog,
  CSV-Hinweis, Auswahl und Meldungen bei Abhängigkeiten, Bearbeiten in der Zeile, Business-Kontext mit Knopf

Befund 5 aus dem Issue (Gliederung der Detailseite) ist **nicht** Teil dieses Issues (Rückfrage im Plan,
2026-11-08): Nach PR 2 ist die Seite deutlich kürzer; eine Gliederung bekäme bei Bedarf eine eigene Story.

Nur Frontend: keine Änderung an Domain, REST-API oder Schema.

## Zu erstellende Dateien

- [ ] keine (die Beispieldatei für den CSV-Import entsteht im Browser, siehe Architektur-Entscheide)
- [x] PR 2, ungeplant: `src/main/webapp/src/app/features/feature-backlog/feature-fields.component.{ts,html}` —
  Felder von Erfassen und Bearbeiten (`notes.md`)

## Zu ändernde Dateien

PR 1:
- [ ] `src/main/webapp/src/app/features/program-board/program-board.component.ts` — `drop()`: ohne
  `isPointerOverContainer` nichts speichern; liegt der Ablagepunkt (`event.dropPoint`) in einer IP-Zelle, Meldung
  über den `BoardService` setzen
- [ ] `src/main/webapp/src/app/features/program-board/program-board.component.html` — IP-Zellen mit einem
  Daten-Attribut kennzeichnen, damit `drop()` sie am Ablagepunkt erkennt
- [ ] `src/main/webapp/src/app/core/services/board.service.ts` — öffentliche Methode, die eine Meldung als
  `actionError` setzt (heute nur intern bei gescheitertem Speichern); Name nach Gerüst
- [ ] `src/main/webapp/src/app/features/program-board/program-board.component.spec.ts`,
  `src/main/webapp/src/app/core/services/board.service.spec.ts` — neue Tests (test-author)
- [ ] `src/test/resources/features/program-increments/13-feature-einplanen.feature` — zwei `@Unit`-Szenarien
- [ ] `spec/user-stories/13-feature-einplanen.md` — AK einpflegen, Tests-Tabelle, «Änderungen»

PR 2:
- [ ] `src/main/webapp/src/app/app.ts`, `app.spec.ts` — Link «Program Increments» in der Kopfzeile
- [ ] `src/main/webapp/src/app/features/program-board/program-board.component.html` (+ spec) — sichtbare Überschrift
  «Team», Trennlinie unter dem Kopf, Hinweise im leeren Backlog
- [ ] `src/main/webapp/src/app/features/feature-backlog/feature-backlog.component.{ts,html,spec.ts}` —
  CSV-Hinweis mit Beispieldatei; Auswahl ohne verbundene Features mit Platzhalter, Knopf erst mit Wahl aktiv;
  Meldung je Zeile; Bearbeiten in der Zeile
- [ ] `src/main/webapp/src/app/core/services/feature-dependencies.service.ts` (+ spec) — nur falls die Meldung je
  Zeile das Feature braucht, dessen Aktion scheiterte (sonst bleibt das in der Komponente)
- [ ] `src/main/webapp/src/app/features/program-increment-detail/program-increment-detail.component.{ts,html,spec.ts}`
  — Formulare für Business-Kontext und Links hinter Knöpfen
- [ ] Feature-Dateien `1-pi-anlegen`, `8-business-kontext`, `10-backlog-bearbeiten`, `11-csv-import`,
  `12-program-board-anzeigen`, `16-abhaengigkeit-erfassen` — neue `@Unit`-Szenarien
- [ ] Stories US-1, US-8, US-10, US-11, US-12, US-16 — Delta einpflegen

## Delta

`spec/changes/50-nacharbeiten-playwright/delta.md` — je Story ein Abschnitt:

- US-13 Neu: Karte ausserhalb einer Ablage bleibt am Ort, über IP mit Meldung (2 Szenarien)
- US-1 Neu: Kopfzeile verlinkt die PI-Übersicht
- US-12 Neu: Überschrift «Team» und Trennlinie; Hinweise im leeren Backlog (3 Szenarien)
- US-11 Neu: Spalten genannt, Beispieldatei
- US-16 Geändert: Auswahl nur noch nicht verbundener Features mit Platzhalter; Meldung in der Zeile. Neu: 2 Szenarien
- US-10 Geändert: AK 3 Bearbeiten in der Zeile. Neu: 1 Szenario
- US-8 Geändert: AK 3 Formulare hinter Knöpfen. Neu: 1 Szenario
- Entfernt: keine

**Zwei PRs, ein Delta:** `check-story-drift.sh` verlangt, dass jeder Abschnitt einer geänderten `delta.md`
eingepflegt ist. PR 1 bringt deshalb `delta.md` nur mit dem Abschnitt US-13 (eingepflegt); PR 2 fügt die übrigen
Abschnitte hinzu und pflegt sie ein. Der Entwurf hier enthält alle Abschnitte für die Prüfung vorab.

## Architektur-Entscheide

- **Schicht:** nur Frontend, `features/` (Driving Adapter) und `core/services/`. Kein `HttpClient` ausserhalb von
  `core/api/`, keine neuen Ports
- **IP-Drop – zwei Wege:**
  - *Gewählt:* IP-Zelle bleibt keine Ablage; `drop()` ignoriert ein Loslassen ohne `isPointerOverContainer` und
    erkennt die IP über `document.elementFromPoint(event.dropPoint)` und ein Daten-Attribut. Keine Vorschau, die ein
    Einplanen in die IP vortäuscht; der bestehende Test «macht das Backlog und die Zellen der
    Entwicklungs-Iterationen zu Ablagen, die IP nicht (US-13)» gilt weiter
  - *Verworfen:* IP-Zelle als Ablage mit Ablehnung im `drop()`. Einfacher zu erkennen, aber die Karte springt
    während des Ziehens sichtbar in die IP-Zelle, und der bestehende Test müsste sich ändern
  - Die Meldung entsteht im Frontend, ohne Aufruf an den Server (fail fast an der Systemgrenze); der Server lehnt IP
    weiter selbst ab (CucumberIT «Einplanen in die IP-Iteration wird abgelehnt»). Der Text ist derselbe wie der des
    Servers; eine Konstante im Frontend, da ein gemeinsamer Ort über Java und TypeScript fehlt
- **Bearbeiten in der Zeile:** eigenes `FormGroup` für die Zeile in `feature-backlog`, Validatoren mit dem
  Erfassungsformular geteilt (eine Funktion, die das Formular baut). Keine neue Komponente, solange das Template
  lesbar bleibt; wird es zu lang, eine Zeilen-Komponente unter `features/feature-backlog/`
- **Beispieldatei:** Link mit `download="pickel-backlog-beispiel.csv"` und `data:text/csv;charset=utf-8,…`-URL aus
  einer Konstante. Keine statische Datei, weil das Frontend keinen Asset-Ordner hat und `angular.json` dafür
  geändert werden müsste (Risiko-Pfad). Angular lässt `data:`-URLs in `href` durch (nur `javascript:` wird ersetzt);
  ein Vitest-Test belegt, dass der Link nicht als `unsafe:` endet
- **Keine neue ADR**; Architektur-Doku unverändert
- **Überschneidung mit #18:** US-18 ändert ebenfalls `program-board.component.*`. PR 1 ist klein; wer zuerst merged,
  der andere rebased

## Aufgaben

PR 1 (Branch `fix/50-ip-drop`):
- [x] 1. Gerüst: IP-Zellen mit Daten-Attribut, öffentliche Methode im `BoardService` für eine Meldung — erledigt,
  wenn `ng build` grün ist und alle bestehenden Vitest-Tests grün bleiben
- [x] 2. Tests aus dem Delta US-13 (test-author), rot aus dem richtigen Grund — erledigt, wenn die neuen Tests an der
  fehlenden Prüfung in `drop()` scheitern
- [x] 3. `drop()` umsetzen — erledigt, wenn die neuen und alle bestehenden Board-Tests grün sind
- [x] 4. Feature-Datei, Story US-13 einpflegen, `delta.md` auf den Abschnitt US-13 kürzen — erledigt, wenn
  `check-story-drift.sh` nichts meldet und `scripts/gate.sh` grün ist; Prüfung von Hand mit dem Playwright-Skript
  aus dem Issue (Karte auf Greif/IP → bleibt, Meldung)

PR 2 (Branch `feat/50-bedienung`):
- [x] 5. Kopfzeile (US-1) und Board-Kopf mit leerem Backlog (US-12) — erledigt, wenn die Vitest-Tests zu den
  Szenarien «Kopfzeile führt zur PI-Übersicht», «Board zeigt die Überschrift der Team-Spalte», «Leeres Backlog auf
  dem Board zeigt einen Hinweis», «Board ohne Features verweist auf die Detailseite» grün sind
- [x] 6. CSV-Hinweis mit Beispieldatei (US-11) — erledigt, wenn der Vitest-Test zum Szenario «Oberfläche nennt das
  CSV-Format und bietet eine Beispieldatei» grün ist
- [x] 7. Auswahl der Abhängigkeiten (US-16) — erledigt, wenn der Vitest-Test zu «Auswahl bietet nur noch nicht
  verbundene Features an» und die geänderten Tests aus dem Delta grün sind
- [x] 8. Meldung je Zeile bei Abhängigkeiten (US-16) — erledigt, wenn der Vitest-Test zu «Abgelehnte Abhängigkeit
  meldet sich beim Feature» und die geänderten Alarm-Tests grün sind
- [x] 9. Bearbeiten in der Zeile (US-10) — erledigt, wenn der Vitest-Test zu «Feature wird in seiner Zeile
  bearbeitet» und die angepassten US-10-Tests grün sind
- [x] 10. Business-Kontext und Links hinter Knöpfen (US-8) — erledigt, wenn der Vitest-Test zu «Business-Kontext
  wird über Bearbeiten geändert» und die angepassten US-8-Tests grün sind
- [ ] 11. Feature-Dateien und Stories einpflegen, Screenshot des Board-Kopfs bei 1400 px im PR — erledigt, wenn
  `check-story-drift.sh` nichts meldet und `scripts/gate.sh` grün ist

## Test-Strategie

Alles auf Ebene Unit (Vitest): kein neuer Endpunkt, keine Persistenz. Die Szenarien sind `@Unit` mit
`# Vitest: <spec> → <testname>`.

- [x] Unit (US-13): Ein Loslassen ohne `isPointerOverContainer` ruft weder `schedule` noch `unschedule` auf, egal aus
  welcher Liste die Karte kam (Backlog, Zelle) und welche Liste sie zuletzt überfuhr
- [x] Unit (US-13): Liegt der Ablagepunkt in einer IP-Zelle, erscheint «In der IP-Iteration wird keine
  Feature-Arbeit geplant.» als Alarm über dem Board, ohne Serveraufruf; ausserhalb von IP und Raster keine Meldung
- [x] Unit (US-13): Ein Ablegen mit `isPointerOverContainer` in eine andere Zelle oder ins Backlog speichert wie
  bisher (bestehende Tests bleiben unverändert grün)
- [x] Unit (US-1): Die Kopfzeile hat auf jeder Seite einen Link «Program Increments» auf `/`
- [x] Unit (US-12): Erste Kopfspalte zeigt sichtbar «Team» (nicht `sr-only`); Backlog leer bei eingeplanten Features
  → «Alle Features sind eingeplant.»; Board ohne Features → «Noch keine Features.» mit Link auf
  `/program-increments/:id`
- [x] Unit (US-11): Hinweis nennt die fünf Spalten; der Download-Link hat `download` und eine `data:text/csv`-URL,
  deren Inhalt mit der Kopfzeile `Titel;Beschreibung;WSJF-Rang;Owner;Schätzung` beginnt und nicht mit `unsafe:`
- [x] Unit (US-16): Auswahl bei «Shop» (braucht «Login», wird von «Karte» gebraucht) enthält weder «Login» noch
  «Karte» noch «Shop»; Platzhalter «Feature wählen …»; Knopf `disabled` ohne Wahl
- [x] Unit (US-16): Abgelehnte Abhängigkeit → Meldung in der Zeile des Features, nicht über der Liste; nach der
  nächsten Aktion an Abhängigkeiten (Erfassen oder Entfernen, auch in einer anderen Zeile) verschwunden. Gilt auch
  für gescheitertes Entfernen
- [x] Unit (US-10): «Bearbeiten» macht die Zeile zum Formular mit den Angaben; Erfassungsformular bleibt leer und
  zeigt «Feature erfassen»; Validierung wie beim Erfassen am Feld der Zeile; Abbrechen verwirft; ein zweites
  «Bearbeiten» schliesst das erste ohne Speichern
- [x] Unit (US-8): Mit Inhalt kein Eingabefeld, der Text steht genau einmal; «Bearbeiten» öffnet das Formular mit
  dem Text; Speichern schliesst es und zeigt den neuen Text; Abbrechen verwirft; ohne Inhalt Hinweis mit «Erfassen»;
  «Link hinzufügen» öffnet das Link-Formular, nach dem Hinzufügen ist es zu
- [x] Bestehende Tests, die sich ändern: die im Delta unter «Geändert → Tests» genannten (US-16, US-10, US-8)
- [ ] Prüfung von Hand: Trennlinie im Board-Kopf (jsdom misst kein Layout; Screenshot im PR wie bei US-12 AK 7);
  echtes Ziehen mit der Maus über IP und neben das Raster (das CDK-Ziehen selbst prüft Vitest nicht; Playwright-Skript
  aus dem Test zum Issue, E2E ist nicht eingerichtet)

## Offene Fragen

- keine (die Fragen zu Bearbeiten, Business-Kontext, Gliederung und Schnitt sind am 2026-11-08 beantwortet)
