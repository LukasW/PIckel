# ADR 0003: Einheitlicher Fehlervertrag `{ "message": … }`, Problem Details später

**Status:** Angenommen
**Datum:** 2026-11-07

## Kontext

Das Projekt-Review vom 7. November 2026 fand zwei Prüfstellen für dieselben Regeln: Bean Validation am Request und
die Domain. Bean Validation, kaputtes JSON und unpassende Werte lieferten 400 in der Form von Quarkus, ohne
`message`; die Oberfläche zeigte dann nur eine allgemeine Meldung. Zur Wahl stand, jetzt auf Problem Details
([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)) umzustellen.

## Entscheid

- Fachregeln prüft nur die Domain; der Request trägt keine Bean-Validation-Annotationen mehr. Einzig `@NotNull` am
  Body bleibt, weil ohne Inhalt kein Objekt entsteht, das die Domain prüfen könnte
- Jede 400 hat den Körper `{ "message": "<Satz für die Oberfläche>" }` (`ErrorResponse`). Je Fehlerart ein
  `ExceptionMapper` im REST-Adapter: fachliche Regel, fehlender Inhalt, kein gültiges JSON, unpassender Wert
- **Problem Details noch nicht:** Es gibt genau einen Client, die eigene Oberfläche, und sie braucht nur einen Satz.
  `type`, `title`, `instance` hätten heute keinen Leser. Umstellen, sobald ein zweiter Client (Integration,
  Fremdsystem) Fehler maschinell unterscheiden muss; dann `message` als `detail` weiterführen

## Konsequenzen

- Neue Fehlerarten brauchen einen Mapper und ein `CucumberIT`-Szenario mit der erwarteten Meldung
- Der Mapper für `WebApplicationException` greift nur bei einem JSON-Lesefehler; alle anderen behalten ihre Antwort
- Ein Wechsel auf Problem Details ist ein Breaking Change der API und braucht eine eigene Story
