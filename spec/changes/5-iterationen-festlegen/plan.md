# US-5 — Iterationen eines PI mit IP-Iteration festlegen (Issue #5)

## Übersicht

Beim Anlegen eines PI wählt der RTE die Iterationslänge (1–4 Wochen, optional, Standard 2). Die Domain teilt den
Zeitraum ab dem Startdatum in Iterationen dieser Länge; die letzte endet am Enddatum, heisst «IP» und darf kürzer
sein, die übrigen heissen «Iteration 1», «Iteration 2» usw. Gespeichert wird nur die Länge (Migration V2). Neu gibt
es `GET /api/v1/program-increments/{id}` (404 bei unbekannter ID) und eine Detailseite, die aus der Liste erreichbar
ist und die Iterationen zeigt. `POST` antwortet mit `Location`.

Rechenregel (Domain): `n = 7 · Wochen`, `tage = Ende − Start + 1` (inklusiv), Anzahl `k = ⌈tage / n⌉`, verlangt
`k ≥ 2`. Iteration `i` (1-basiert): Start `Start + (i−1)·n`, Ende `min(Start + i·n − 1, Ende)`; Iteration `k` ist IP.

## Zu erstellende Dateien

- [x] `spec/user-stories/5-iterationen-festlegen.md` — Story US-5 aus dem Delta
- [x] `src/test/resources/features/program-increments/5-iterationen-festlegen.feature` — Szenarien aus dem Delta
- [x] `src/main/java/ch/grudligstrasse/pickel/domain/model/Iteration.java` — Record `number`, `name`, `startDate`,
  `endDate`, `innovationAndPlanning`
- [x] `src/main/java/ch/grudligstrasse/pickel/domain/model/ProgramIncrementNotFoundException.java` — unbekannte ID
- [x] `src/main/java/ch/grudligstrasse/pickel/adapter/in/rest/IterationDto.java` — Antwort-Darstellung
- [x] `src/main/java/ch/grudligstrasse/pickel/adapter/in/rest/ProgramIncrementNotFoundExceptionMapper.java` — 404
- [x] `src/main/resources/db/migration/V2__add_iteration_length.sql` — Spalte `iteration_length_weeks`
- [x] `src/main/webapp/src/app/features/program-increment-detail/program-increment-detail.component.{ts,html,spec.ts}`
  — Detailseite

## Zu ändernde Dateien

- [x] `domain/model/ProgramIncrement.java` — `iterationLengthWeeks`, Regeln (1–4, mindestens zwei Iterationen),
  `iterations()`; `plan(…)` mit optionaler Länge, Standard als Konstante
- [x] `domain/port/in/ProgramIncrementUseCase.java` — `plan(…, Integer iterationLengthWeeks)`,
  `get(UUID id)`
- [x] `domain/port/out/ProgramIncrementRepository.java` — `Optional<ProgramIncrement> findOne(UUID id)`
- [x] `application/service/ProgramIncrementService.java` — Länge durchreichen, `get` mit Not-Found
- [x] `adapter/in/rest/PlanProgramIncrementRequest.java` — Feld `Integer iterationLengthWeeks`
- [x] `adapter/in/rest/ProgramIncrementDto.java` — `iterationLengthWeeks`, `List<IterationDto> iterations`
- [x] `adapter/in/rest/ProgramIncrementResource.java` — `GET /{id}`, `Location` bei 201, Kommentar nachführen
- [x] `adapter/out/persistence/ProgramIncrementEntity.java`, `ProgramIncrementRepositoryAdapter.java` — Spalte,
  Mapping, `findById`
- [x] `src/main/resources/db/changeLog.xml` — changeSet 2
- [x] `src/test/java/.../domain/model/ProgramIncrementTest.java`, `application/service/ProgramIncrementServiceTest.java`
  — neue Regeln, Aufrufe mit Länge
- [x] `src/test/java/.../cucumber/steps/ProgramIncrementSteps.java` — Schritte für US-5 und den `Location`-Header
- [x] `src/test/resources/features/program-increments/1-pi-anlegen.feature` — Schritt `Location` (Delta US-1)
- [x] `src/main/webapp/src/app/core/models/program-increment.model.ts` — `Iteration`, neue Felder
- [x] `src/main/webapp/src/app/core/ports/program-increments.repository.ts`, `core/api/…http.adapter.ts`,
  `core/services/program-increments.service.ts` (+ spec) — `get(id)`
- [x] `src/main/webapp/src/app/features/program-increments/program-increments.component.{ts,html,spec.ts}` —
  Auswahl Iterationslänge (Standard 2), Link auf die Detailseite
- [x] `src/main/webapp/src/app/app.routes.ts` — Route `program-increments/:id`
- [x] `spec/user-stories/1-pi-anlegen.md` — Delta US-1 einpflegen
- [x] `spec/fachliche-spezifikation.md` — Datenmodell (`iterationLengthWeeks`, Iteration abgeleitet), REST-API
  (`GET /{id}`, `Location`)
- [x] `docs/arc42.md` — §8 «Domänenmodell und Validierung»: Iterationen berechnet, Verweis über PI und Nummer

## Delta

`spec/changes/5-iterationen-festlegen/delta.md` — Neu (US-5): 6 AK, 9 Szenarien. Geändert (US-1): Szenario «PI wird
angelegt und erscheint in der Liste» — Antwort mit `Location`, `iterationLengthWeeks` und `iterations`.
Entfernt: keine.

## Architektur-Entscheide

- **Domain:** `Iteration` als Record in `domain/model/`, `ProgramIncrement.iterations()` berechnet die Liste. Der
  Name («Iteration n», «IP») steht in der Domain, weil spätere Szenarien (#13, #15) Iterationen über ihn ansprechen.
  Der Standard 2 Wochen ist eine fachliche Regel und steht als Konstante in der Domain; `plan(…)` setzt ihn bei
  `null`, der Compact Constructor verlangt eine Länge
- **Ports:** `ProgramIncrementUseCase.get(UUID)` wirft `ProgramIncrementNotFoundException`;
  `ProgramIncrementRepository.findById` liefert `Optional`
- **Persistenz:** V2 `ALTER TABLE program_increment ADD COLUMN iteration_length_weeks INTEGER NOT NULL DEFAULT 2`,
  `CHECK (iteration_length_weeks BETWEEN 1 AND 4)`, danach `DROP DEFAULT`. `INTEGER`, damit Hibernate-Validate mit
  `int` passt. «Mindestens zwei Iterationen» nur in der Domain
- **REST:** 404 über eigenen `ExceptionMapper` mit `ErrorResponse` (ADR 0003); `Location` über `UriInfo`
- **Frontend:** Detailseite als eigenes Feature unter `features/program-increment-detail/`, lädt per
  `ProgramIncrementsService.get(id)`; keine Rechenlogik im Client. Iterationen als Tabelle (Name, Start, Ende im
  Format TT.MM.JJJJ), IP-Zeile mit Kennzeichnung «IP-Iteration» (Text, nicht nur Farbe)
- **ADR:** keine neue; ADR 0006 deckt «Iterationen aus fester Länge», arc42 §8 hält «berechnet, Verweis über
  Nummer» fest

## Aufgaben

- [x] 1. Story und Feature-Datei aus dem Delta (US-5-Szenarien zunächst `@Pending`), `Location`-Schritt in US-1 —
  erledigt, wenn `bdd-cucumber-author` die Tags bestätigt
- [x] 2. Migration V2 + changeSet, Entity-Spalte — erledigt, wenn `SmokeIT` grün
- [x] 3. Domain: `Iteration`, `ProgramIncrement` mit Länge, Regeln und `iterations()`; Aufrufer mechanisch
  nachziehen — erledigt, wenn die Domain-Tests grün sind und PIT für `domain/` ≥ 80 %
- [x] 4. Application + Persistenz: `get(id)` mit `findById`, Not-Found-Exception — erledigt, wenn
  `ProgramIncrementServiceTest` grün
- [x] 5. REST: DTOs, `GET /{id}`, 404-Mapper, `Location` — erledigt, wenn die Cucumber-Szenarien von US-5 und
  das geänderte US-1-Szenario grün sind, kein `@Pending`
- [x] 6. Frontend Core: Modell, Port, Adapter, Service `get` — erledigt, wenn `program-increments.service.spec.ts`
  grün
- [x] 7. Frontend UI: Auswahl Iterationslänge, Link in der Liste, Route, Detailseite — erledigt, wenn die
  Vitest-Belege der `@Unit`-Szenarien grün sind
- [x] 8. Doku: Deltas einpflegen, fachliche Spezifikation, arc42 §8 — erledigt, wenn `check-story-drift.sh` ohne
  Meldung
- [x] 9. Abschluss — erledigt, wenn `scripts/gate.sh` grün

## Test-Strategie

Tag-Routing der Szenarien aus dem Delta:

| Szenario | Ebene |
|---|---|
| Iterationen werden aus der Iterationslänge erzeugt | CucumberIT (REST + Persistenz) |
| Ohne Iterationslänge gelten 2 Wochen | CucumberIT (Vertrag: fehlendes Feld) |
| Letzte Iteration ist bei einem Rest kürzer | `@Unit` JUnit `ProgramIncrementTest` |
| Iterationslänge ausserhalb von 1 bis 4 Wochen wird abgelehnt | `@Unit` JUnit `ProgramIncrementTest` |
| Zeitraum für weniger als zwei Iterationen wird abgelehnt | `@Unit` JUnit `ProgramIncrementTest` |
| Unbekanntes PI wird nicht gefunden | CucumberIT (404-Vertrag) |
| Detailseite zeigt die Iterationen mit markierter IP-Iteration | `@Unit` Vitest Detailseite |

- [x] Unit (Domain): 04.01.–26.03.2027 mit 2 Wochen → 6 Iterationen, «Iteration 1» 04.01.–17.01., «Iteration 5»
  01.03.–14.03., «IP» 15.03.–26.03.; Nummern lückenlos, nur die letzte IP
- [x] Unit (Domain): Grenzen der Länge — 0 und 5 abgelehnt, 1 und 4 angenommen; `null` über `plan` → 2 Wochen
- [x] Unit (Domain): Mindestdauer — 04.01.–17.01. (14 Tage, 2 Wochen) abgelehnt, 04.01.–18.01. angenommen mit IP
  18.01.–18.01.; genau aufgehender Zeitraum (04.01.–28.02.) → IP volle 14 Tage, keine leere Iteration
- [x] Unit (Domain): Prüfreihenfolge — Enddatum vor Startdatum liefert weiter die Meldung aus US-1
- [x] Unit (Application): `plan` gibt die Länge weiter; `get` liefert das PI oder wirft Not-Found
- [x] Integration: `GET /{id}` nach dem Speichern liefert die Iterationen aus der gespeicherten Länge (z. B. 3
  Wochen, nicht der Standard); `Location` zeigt auf einen abrufbaren Endpunkt
- [x] Smoke: V2 läuft, Hibernate-Validate passt
- [x] Unit (Vitest): Formular sendet ohne Auswahl 2, nach Wahl 3; Liste verlinkt auf `/program-increments/<id>`;
  Detailseite zeigt 6 Iterationen mit TT.MM.JJJJ und «IP-Iteration»; unbekanntes PI zeigt die Meldung
- [x] Bestehende Tests (Delta US-1 → Geändert): `ProgramIncrementSteps`, `ProgramIncrementTest`,
  `ProgramIncrementServiceTest`, `program-increments.service.spec.ts`, `program-increments.component.spec.ts` — nur
  ergänzt, keine Erwartung abgeschwächt
- [ ] Von Hand: Detailseite im Dev-Modus, Navigation aus der Liste und zurück

## Offene Fragen

Geklärt am 7. November 2026: Entwurf von `main` als Basis; 404 mit Meldung; `Location` als Geändert in US-1;
Iterationen berechnet. Bestehende Dev-PIs kürzer als 15 Tage verletzen nach V2 die Mindestdauer — nur lokale
Dev-Daten, wird in `notes.md` festgehalten.
