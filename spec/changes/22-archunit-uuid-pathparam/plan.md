# Task #22 — ArchUnit-Regel gegen UUID als @PathParam

Delta: keines — Build-Prüfung, kein fachliches Verhalten; der Fehlervertrag (ADR 0003) gilt unverändert.

## Aufgaben

- [x] Regel `RestAdapterRules.noUuidPathParams()`: Methoden in `adapter.in.rest` dürfen keinen Parameter vom Typ `UUID`
  haben, der mit `@PathParam` oder `@RestPath` annotiert ist (Quarkus scheitert an der Umwandlung vor dem
  `ExceptionMapper`, die Antwort hat kein `{ "message": … }`)
- [x] Regel in `ArchitectureTest` gegen den Produktionscode einhängen
- [x] Regel an Fixtures nachweisen (`archfixture`): UUID-`@PathParam` und `@RestPath` schlagen an, `String` nicht

## Test-Strategie

- Unit (ArchUnit): `RestAdapterRulesTest` prüft die Regel an Fixture-Klassen unter `src/test/.../archfixture` (nicht
  im Produktionspaket, damit `ArchitectureTest` grün bleibt); ohne Nachweis am Verstoss wäre eine Regel, die nie
  anschlägt, nicht von einer funktionierenden zu unterscheiden
- `ArchitectureTest`: wendet die Regel auf den echten REST-Adapter an
- Kein Gherkin: kein neues Verhalten nach aussen
