# US-8 — Business-Kontext und ART-Ziele am PI (Issue #8)

## Übersicht

Das PI bekommt `businessContext` und `artObjectives` (Text, je höchstens 10 000 Zeichen, leer erlaubt, Standard
leer) und eine Liste `links` (`PiLink`: `id`, `title` ≤ 100, `url` nur `http`/`https`). Die Regeln stehen in der
Domain (`ProgramIncrement`, `PiLink`); Verletzungen sind `InvalidProgramIncrementException` (400, bestehender Mapper).

REST (alle liefern das ganze PI als `ProgramIncrementDto`, 200):

- `PUT /api/v1/program-increments/{id}/business-context` mit `{businessContext, artObjectives}`
- `POST /api/v1/program-increments/{id}/links` mit `{title, url}`
- `DELETE /api/v1/program-increments/{id}/links/{linkId}`; unbekannte oder ungültige Link-ID → 404
  «Den Link gibt es nicht.»

Unbekanntes PI: 404 wie in US-5. Persistenz: Migration V3 (nur additiv: zwei Textspalten mit Standard `''`, Tabelle
`pi_link`), Link-Liste geordnet über `position`.

Fehlermeldungen (Wortlaut aus dem Delta): «Die URL muss mit http:// oder https:// beginnen.» und «Der Text darf
höchstens 10 000 Zeichen lang sein.»; für den Titel analog zum PI-Namen: «Der Titel darf nicht leer sein.» und
«Der Titel darf höchstens 100 Zeichen lang sein.»; leere URL: «Die URL darf nicht leer sein.»

## Aufgaben

- [x] 1. Story + Feature
- [x] 2. Gerüst + Tests (`test-author`)
- [x] 3. Domain: `PiLink`, `ProgramIncrement.withBusinessContext/withLink/withoutLink`
- [x] 4. Application: `updateBusinessContext`, `addLink`, `removeLink` (`@Transactional`)
- [x] 5. Persistenz: V3, Entity, Adapter-Upsert
- [x] 6. REST: Requests, DTO, Endpunkte, 404-Mapper für Link
- [x] 7. Frontend: Modell, Port, Adapter, Service, Detailseite
- [x] 8. Doku: Delta einpflegen, fachliche Spezifikation (Datenmodell, REST), arc42 §8
- [ ] 9. `scripts/gate.sh` grün

## Test-Strategie

| Szenario / AK | Ebene |
|---|---|
| Business-Kontext erfasst und geändert, Link hinzugefügt und entfernt, Link ohne http abgelehnt | CucumberIT |
| Zu langer Text abgelehnt | `@Unit` JUnit `ProgramIncrementTest` |
| Domain-Regeln: Grenze 10 000/10 001, Titel 100/101, leerer Titel/URL, `ftp:`/`javascript:`, Schema in Grossbuchstaben, Entfernen | JUnit |
| Application | JUnit mit gemocktem Port |
| AK 3/4 Detailseite (Zeilenumbrüche, `target="_blank"` + `rel`, Hinweis ohne Inhalt, Erfassen, Link hinzufügen/entfernen) | Vitest |

Delta: siehe `delta.md` (nur «Neu»).
