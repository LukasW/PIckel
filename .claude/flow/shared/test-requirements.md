# Fragment: Tests als Spezifikation

Pflicht für jede fachliche Änderung. Welche Ebenen, Werkzeuge und Orte ein Projekt hat, steht in `flow.md` →
«Tests»; hier steht, was überall gilt.

## Tests als Spezifikation

Code lässt sich neu schreiben, die Tests sind der Vertrag zwischen Absicht und Umsetzung. Daraus folgt:

- **Reihenfolge:** Spezifikation → Akzeptanzkriterium → Test → Code. Die Tests entstehen vor der Implementierung
  (`/flow-implement` Schritt 6a/6b)
- **Aus der Spezifikation, nicht aus dem Code:** Erwartete Werte stammen aus Story, Plan, Architektur-Doku und
  Regeln, nie aus der Implementierung. Deshalb schreibt sie der Agent `test-author` in einem eigenen Kontext.
  Prüft ein Test nur, was der Code ohnehin tut, sieht er keinen Fehler
- **Fachliche Absicht:** Der Name sagt das Verhalten («Ein zweiter Lauf lädt nichts hoch»), nicht die Methode.
  Eine Assertion prüft einen fachlichen Wert, nicht bloss «nicht leer»
- **Keine Trivialtests:** keine Tests für Datenklassen, Getter und reine Mapper ohne Logik. Coverage ist kein Ziel
- **Am sichtbaren Ergebnis:** über Schnittstellen und Fakes, nicht über Interna oder die Reihenfolge von Aufrufen
- **Schnell:** Der Loop des Agenten führt die Suite oft aus. Teure Objekte einmal je Testklasse bauen, nicht je Fall
- **Invarianten als Property:** Eine Regel, die für jede Eingabe gilt, steht als Property-Test (sofern das Projekt
  ein Werkzeug dafür hat), nicht als Schleife über Beispiele

## Bestehende Tests

- Ein roter Test heisst zuerst: Der Code ist falsch. Geändert wird der Test nur, wenn sich die Spezifikation
  ändert, und dann zuerst das Delta (`.claude/flow/shared/story-delta.md`, mit `deltas:` in `flow.md`)
  bzw. die Story-Datei
- Nie eine Assertion abschwächen, einen Test abschalten oder löschen, um grün zu werden
- Umbauen ohne Verlust ist erlaubt (zusammenfassen, verschieben), aber zu begründen
- `.claude/flow/scripts/check-test-edit.sh` meldet nach jedem Edit weniger Assertions oder Testfälle, neue
  Abschaltungen, gelöschte Testdateien und geänderte Vorlagen gegenüber der Abzweigung vom Hauptzweig; mit `--all`
  für den ganzen Branch, auch für Änderungen per Bash. Jede Meldung braucht eine Begründung im Handoff

## Ebene

Die tiefstmögliche Ebene gewinnt: Unit vor Integration vor Ende-zu-Ende; höchstens ein Golden Path je Story auf
der obersten Ebene. Die Ebenen des Projekts nach `flow.md` → «Tests».

## User-Story-Akzeptanz

Jedes Akzeptanzkriterium hat mindestens einen Test, dessen Name auf die Story verweist (Kennzeichnung laut
`flow.md` → «User Stories», in PIkel gleich der Issue-Nummer). Die Szenarien der Story-Datei stehen in der
Form Gegeben sei / Wenn / Dann; unter «Tests» in der Datei steht, welche Testklasse sie belegt.

## BDD mit Gherkin

Gilt, wenn `flow.md` → «Tests» ein Gherkin-Werkzeug nennt (Cucumber, Cucumber.js, Behave, SpecFlow, …). flow fügt
keines hinzu; Ort, Sprache, Tags und Läufer stehen in `flow.md`.

- **Szenario = Szenario der Story:** Die Szenarien der Story stehen gleichlautend in der Feature-Datei, die Story
  verweist darauf. Ändert ein Delta ein Szenario (`shared/story-delta.md`), ändert sich die Feature-Datei im selben
  Wortlaut
- **Kennzeichnung laut `flow.md`:** etwa die Story-Nummer im Titel der Funktionalität (`Funktionalität: US-12 …`)
  oder ein Tag am Szenario (`@US-12`). Jedes Akzeptanzkriterium hat mindestens ein Szenario
- **Sprache wie der Bestand:** `# language: de` mit `Funktionalität`, `Szenario`, `Angenommen` / `Gegeben sei`,
  `Wenn`, `Dann`, `Und`; sonst Englisch
- **Fallunterscheidung als Szenariogrundriss:** je Zeile unter `Beispiele` weicht nur die unterscheidende Eingabe ab
- **Prüfen nur in `Dann`:** `Angenommen` baut den Zustand, `Wenn` löst aus, `Dann` prüft einen fachlichen Wert
  («finde ich 2 Notizen»), nicht «kein Fehler»
- **Steps wiederverwenden:** zuerst die gemeinsamen Steps des Projekts; ein neuer Step-Text gehört dorthin, wo
  `flow.md` es sagt. Ein Step-Text, der fast wie ein bestehender lautet, ist ein Fehler
- **Rot aus dem richtigen Grund:** Der Step ist definiert und scheitert an «nicht implementiert» aus dem Gerüst oder
  an der Prüfung in `Dann`. «Undefined» oder «pending» ist kein Rot aus dem richtigen Grund
- **Abschalten über Tags** (`@Pending`, `@ignore`, `@wip`, `@skip`, `@disabled`, `@manual`) ist eine Abschaltung wie
  `@Disabled`; `check-test-edit.sh` meldet sie, dazu weniger Szenarien, `Dann`-Schritte und Tabellenzeilen.
  Routing-Tags des Projekts (etwa `@E2E`, `@Unit`) meldet es nicht
