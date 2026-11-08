# Delta US-12 — Program Board nutzt die volle Fensterbreite (Issue #37)

Story: `spec/user-stories/12-program-board-anzeigen.md` (bestehend) · Status: eingepflegt

## Warum

Das Board steckt im schmalen Inhalts-Container der übrigen Seiten. Schon bei 1400 px Fensterbreite und vier
Iterationen ist die IP-Spalte abgeschnitten und man muss im Raster seitlich scrollen (Playwright-Test vom
2026-11-08 auf `85a7132`). Auf einer Planungswand will man alle Iterationen nebeneinander sehen.

## Entscheide

Ausführlich mit Begründungen: `entscheide.md`.

- **Nur die Board-Seite** wird breit; Listen- und Formularseiten behalten ihre Lesebreite
- **Flag in den Routendaten:** Die Board-Route trägt `data: { fullWidth: true }`, `App` lässt dann `max-w-4xl` an
  Kopfzeile und Inhalt weg; die Kopfzeile zieht also mit
- **Das Raster füllt die Fläche** (`w-full`); die Iterationsspalten teilen sich die freie Breite
- **Waagrechtes Scrollen im Raster bleibt** als Rückfall, wenn ein PI mehr Iterationen hat, als die Fensterbreite
  fasst (bisheriger Entscheid aus US-12)
- **Mobile** bleibt für das Board kein Ziel in M1
- **Nachweis:** AK 1 per Vitest (`app.spec.ts`), AK 2 manuell per Screenshot bei 1400 px im PR. jsdom misst kein
  Layout, und E2E ist nicht eingerichtet

## Neu

### AK: Die Board-Seite ist nicht auf die Inhaltsbreite der übrigen Seiten begrenzt, sondern nutzt die Fensterbreite abzüglich des Seitenrands

### AK: Bei 1400 px Fensterbreite stehen Backlog-Spalte und ein PI mit drei Iterationen und einer IP-Iteration ohne waagrechtes Scrollen nebeneinander (manuell belegt: Screenshot im PR)

### Szenario: Board nutzt die volle Fensterbreite

Tag `@Unit`, belegt durch Vitest `app.spec.ts` (Plan → «Test-Strategie»).
- **Gegeben sei** ein PI
- **Wenn** ich das Board des PI öffne
- **Dann** sind Kopfzeile und Inhalt nicht auf die Lesebreite der übrigen Seiten begrenzt
- **Und** auf der Liste der Program Increments sind sie es weiterhin

## Geändert

keine

## Entfernt

keine
