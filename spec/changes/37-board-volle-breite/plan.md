# US-12 — Program Board nutzt die volle Fensterbreite (Issue #37)

## Übersicht

`App` begrenzt Kopfzeile und Inhalt auf allen Routen auf `max-w-4xl` (`app.ts`). Neu trägt die Board-Route
`data: { fullWidth: true }`. `App` liest das Flag der aktiven Route und lässt die Begrenzung dann an `<nav>` und
`<main>` weg. Das Raster des Boards bekommt `w-full` und füllt so die gewonnene Fläche. Nur Frontend; Entscheide:
`entscheide.md`.

## Zu erstellende Dateien

- [x] `src/main/webapp/src/app/app.spec.ts` — Vitest für die Breite je Route (AK 1, Szenario)

## Zu ändernde Dateien

- [x] `src/main/webapp/src/app/app.routes.ts` — Board-Route mit `data: { fullWidth: true }`
- [x] `src/main/webapp/src/app/app.ts` — Signal `fullWidth` aus dem Router, `max-w-4xl` an `<nav>` und `<main>` nur
  ohne Flag
- [x] `src/main/webapp/src/app/features/program-board/program-board.component.html` — Tabelle `w-full` zusätzlich
  zu `min-w-max`
- [x] `src/test/resources/features/program-increments/12-program-board-anzeigen.feature` — Szenario «Board nutzt
  die volle Fensterbreite» als `@Unit` mit `# Vitest:`-Verweis
- [x] `spec/user-stories/12-program-board-anzeigen.md` — Delta einpflegen (AK 6 und 7, Testtabelle, «Änderungen»)
- [x] `spec/changes/37-board-volle-breite/delta.md` — `Status: eingepflegt`

## Delta

`spec/changes/37-board-volle-breite/delta.md` — Neu: AK «Board-Seite nicht auf die Inhaltsbreite begrenzt», AK
«1400 px ohne waagrechtes Scrollen» (manuell belegt), Szenario «Board nutzt die volle Fensterbreite» (`@Unit`).
Geändert: keine. Entfernt: keine.

## Architektur-Entscheide

- Nur Frontend, Shell (`app.ts`, `app.routes.ts`) und `features/program-board`. Kein Backend, keine Migration,
  `ArchitectureTest` unberührt
- **Flag lesen:** `toSignal` über `Router.events` (`NavigationEnd`), dann von `routerState.snapshot.root` bis zum
  tiefsten Kind laufen und `data['fullWidth'] === true` prüfen; `initialValue: false`. Verworfen:
  `ActivatedRoute` in `App`, denn das ist die Wurzelroute und sieht die Daten des Kinds nicht. Verworfen: ein
  eigener Layout-Service, der für ein Flag zu viel Abstraktion wäre
- **Klasse binden:** `[class.max-w-4xl]="!fullWidth()"`; `mx-auto`, `px-4` und `py-*` bleiben immer, so bleibt der
  Seitenrand aus AK 1
- Keine ADR, keine arc42-Änderung: reines Seitenlayout

## Aufgaben

- [x] 1. Tests aus der Spezifikation in `app.spec.ts` (durch `test-author`) — erledigt, wenn sie aus dem richtigen
  Grund rot sind (Board-Route hat noch `max-w-4xl`)
- [x] 2. `data: { fullWidth: true }` an der Board-Route und `fullWidth`-Signal in `App` — erledigt, wenn
  `app.spec.ts` grün und alle übrigen Vitest-Tests grün sind
- [x] 3. `w-full` an der Board-Tabelle — erledigt, wenn `program-board.component.spec.ts` grün ist und im Dev-Modus
  das Raster bei 1920 px bis zum rechten Seitenrand reicht
- [x] 4. Feature-Datei und Story nachführen, Delta `eingepflegt` — erledigt, wenn `check-story-drift.sh` nichts
  meldet
- [ ] 5. Screenshot bei 1400 px (PI mit drei Iterationen + IP, drei Teams) für den PR — erledigt, wenn alle vier
  Spalten ohne waagrechtes Scrollen sichtbar sind
- [x] 6. `scripts/gate.sh` — erledigt, wenn grün

## Test-Strategie

- [x] Unit (Vitest, `app.spec.ts`, mit `appConfig`, echten `routes`, gemockten Ports und `RouterTestingHarness` wie
  in `program-board.component.spec.ts`; Kopfzeile und Inhalt über `header nav` bzw. `main` finden):
  - «nutzt auf dem Board die volle Fensterbreite für Kopfzeile und Inhalt (US-12)»: nach Navigation auf
    `/program-increments/pi-7/board` tragen `nav` und `main` kein `max-w-4xl`
  - «begrenzt auf der PI-Liste Kopfzeile und Inhalt auf die Lesebreite (US-12)»: auf `/` tragen beide `max-w-4xl`
  - «begrenzt nach dem Wechsel vom Board zurück wieder auf die Lesebreite (US-12)»: Board, dann PI-Detail →
    `max-w-4xl` ist wieder da. Das ist die Invariante, die ein einmal gesetztes Flag bräche
  - Echte `routes` statt Testrouten, damit der Test rot wird, wenn das Flag an der Board-Route fehlt
- [ ] Akzeptanz (US-12, Delta):
  - AK 1 → die drei Vitest-Tests oben; Szenario «Board nutzt die volle Fensterbreite» als `@Unit` mit
    `# Vitest: app.spec.ts → nutzt auf dem Board die volle Fensterbreite für Kopfzeile und Inhalt (US-12)`
  - AK 2 → **Prüfung von Hand**: Screenshot bei 1400 px im PR (jsdom misst kein Layout, E2E nicht eingerichtet)
- [x] Bestehende Tests: keine Änderung erwartet; `program-board.component.spec.ts` prüft keine Breitenklassen
- [x] Kein Backend-Test, kein `CucumberIT`: kein Endpunkt und keine Persistenz betroffen. PIT unberührt

## Offene Fragen

keine
