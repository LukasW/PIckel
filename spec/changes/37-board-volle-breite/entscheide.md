# Entscheide — Issue #37

## Geklärte Entscheidungen

- **Nur die Board-Seite wird breit:** Listen- und Formularseiten behalten `max-w-4xl` (Begründung: Lesebreite;
  Quelle: Delta → Entscheide)
- **Nur Frontend:** keine Änderung an Backend, REST oder Schema (Quelle: Issue, Schichten «Frontend»)
- **Ursache:** `app.ts` begrenzt `<main>` und `<nav>` für alle Routen auf `max-w-4xl`. Bei 1400 px bleiben neben
  der Backlog-Spalte (`w-64` + `gap-4`) rund 590 px für das Raster, gebraucht werden rund 740 px (Teamspalte +
  4 × `min-w-40`). Ohne Begrenzung bleiben rund 1100 px (Quelle: Code auf `85a7132`)
- **Mechanismus: Flag in den Routendaten:** Die Route `program-increments/:id/board` trägt `data: { fullWidth: true }`;
  `App` liest das Flag der aktiven Route und lässt dann `max-w-4xl` weg (Begründung: die Breite wird an einem Ort
  deklarativ gesteuert, in Vitest über `App` + Router prüfbar, eine neue Seite bleibt ohne Zutun schmal; verworfen:
  Wrapper pro Seite, weil die Regel dann verteilt ist, und ein CSS-Breakout mit `w-screen`, weil er die
  Scrollbar mitzählt und so oft waagrechten Seitenscroll erzeugt)
- **Kopfzeile zieht mit:** Dasselbe Flag steuert `<nav>` und `<main>` (Begründung: Logo und «Teams» fluchten mit
  dem Board; der Sprung der Kopfzeile zwischen den Seiten ist in Kauf genommen)
- **Raster füllt die Fläche:** Die Tabelle bekommt zusätzlich zu `min-w-max` die Klasse `w-full`, die
  Iterationsspalten teilen sich die freie Breite (Begründung: Planungswand, Platz für Feature-Karten aus #13;
  `min-w-max` mit `min-w-40` hält den Rückfall auf waagrechtes Scrollen)
- **Kein Breitendeckel:** Das Board nutzt die Fensterbreite abzüglich `px-4`, auch auf sehr breiten Monitoren
  (Quelle: AK 1)
- **Waagrechtes Scrollen im Raster bleibt** als Rückfall bei vielen Iterationen (Quelle: US-12, Delta)
- **Nachweis:** AK 1 wird per Vitest in `app.spec.ts` belegt (Board-Route ohne `max-w-4xl` an `nav` und `main`,
  andere Route mit). AK 2 wird **manuell** belegt: ein Screenshot bei 1400 px im PR (Begründung: jsdom misst kein
  Layout, und E2E ist nicht eingerichtet. Laut `CLAUDE.md` kommt E2E erst, wenn eine Story den Browser zwingend
  braucht, und das rechtfertigt diese Ein-Zeilen-Änderung nicht). Das Szenario wird `@Unit`

## Bewusst draussen (Non-Goals)

- Playwright bzw. E2E-Infrastruktur einrichten (eigene Story, falls #13 sie braucht)
- Mobile-Layout des Boards (Quelle: US-12, kein Ziel in M1)
- Breite anderer Seiten ändern

## Verbleibende Annahmen

- Das Risiko, dass eine spätere Änderung (breitere Spalten, mehr Teamspalten-Inhalt) AK 2 unbemerkt bricht, ist
  bewusst in Kauf genommen. Es fällt weg, sobald E2E existiert
- Der genaue Name des Vitest-Tests steht erst nach der Umsetzung fest; das Feature nennt ihn dann als
  `# Vitest: app.spec.ts → <testname>`

## Auswirkung auf die Spezifikation

- Delta `spec/changes/37-board-volle-breite/delta.md`: Entscheide nachgeführt, AK 2 als manuell belegt markiert,
  Szenario als `@Unit` umformuliert
- US-12: Der Satz «Das Raster scrollt waagrecht in seinem eigenen Container» bleibt gültig, beim Einpflegen kommen
  AK 1/2 aus diesem Delta dazu
- Architektur-Doku, ADR: keine
