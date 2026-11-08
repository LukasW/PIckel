# US-13 — Feature auf dem Board einplanen (Issue #13)

## Übersicht

Ein Feature bekommt eine optionale Einplanung (Team × Iterationsnummer) als Wert am Feature (ADR 0005, 0006). Die
Domain prüft IP-Iteration und Iterationsnummer, zwei Endpunkte am Feature planen ein bzw. nehmen zurück, das Board
liefert die eingeplanten Features mit. Im Frontend werden die Zellen zu Drop-Zonen (Angular CDK), jede Karte hat
die Aktion «Einplanen …» mit Inline-Formular; das Board aktualisiert optimistisch und nimmt bei einem Fehler zurück.
Entfernt jemand ein Team, gehen seine Features in allen PIs ins Backlog zurück, worauf die Bestätigung hinweist.

## Zu erstellende Dateien

- [ ] `src/main/java/ch/grudligstrasse/pikel/domain/model/Scheduling.java` — Wert «Team × Iterationsnummer»
- [ ] `src/main/java/ch/grudligstrasse/pikel/adapter/in/rest/SchedulingRequest.java` — Body `{ teamId, iterationNumber }`
- [ ] `src/main/resources/db/migration/V9__add_feature_scheduling.sql` — Spalten `team_id`, `iteration_number`
- [ ] `src/main/webapp/src/app/features/program-board/board-card.component.{ts,html,spec.ts}` — Karte mit Aktion
  «Einplanen …» und Inline-Formular
- [ ] `src/test/resources/features/program-increments/13-feature-einplanen.feature` — Szenarien aus dem Delta
- [ ] `src/test/java/ch/grudligstrasse/pikel/cucumber/steps/SchedulingSteps.java` — Steps zum Einplanen
  (oder in `BoardSteps`, falls dort kürzer)
- [ ] `spec/user-stories/13-feature-einplanen.md` — Story (beim Einpflegen)

## Zu ändernde Dateien

- [ ] `domain/model/Feature.java` — Feld `Optional<Scheduling> scheduling`, `schedule(ProgramIncrement, teamId,
  iterationNumber)`, `unschedule()`, `scheduled()` echt; `revise` behält die Einplanung
- [ ] `domain/model/ProgramIncrement.java` — `Optional<Iteration> iteration(int number)`
- [ ] `domain/model/ProgramBoard.java` — Liste `scheduled` (eingeplante Features in Backlog-Reihenfolge)
- [ ] `domain/port/in/FeatureUseCase.java` — `schedule(piId, featureId, teamId, iterationNumber)`,
  `unschedule(piId, featureId)`
- [ ] `domain/port/out/FeatureRepository.java` — `unscheduleTeam(UUID teamId)`
- [ ] `application/service/FeatureService.java` — die zwei Use Cases; braucht zusätzlich `TeamRepository`
- [ ] `application/service/BoardService.java` — Backlog und Eingeplante trennen
- [ ] `application/service/TeamService.java` — `remove` ruft vorher `features.unscheduleTeam(id)`
- [ ] `adapter/in/rest/FeatureResource.java` — `PUT /{id}/scheduling`, `DELETE /{id}/scheduling`
- [ ] `adapter/in/rest/FeatureDto.java` — `teamId`, `iterationNumber` (null im Backlog)
- [ ] `adapter/in/rest/BoardDto.java` — `scheduled: List<FeatureDto>`
- [ ] `adapter/out/persistence/FeatureEntity.java`, `FeatureRepositoryAdapter.java` — Spalten, Mapping,
  `unscheduleTeam`
- [ ] `src/main/resources/db/changeLog.xml` — changeSet 9
- [ ] `src/main/webapp/src/app/core/models/feature.model.ts`, `board.model.ts` — `teamId?`, `iterationNumber?`;
  `scheduled: readonly Feature[]`
- [ ] `core/ports/features.repository.ts`, `core/api/features-http.adapter.ts` — `schedule`, `unschedule`
- [ ] `core/services/board.service.ts` — `schedule`/`unschedule` optimistisch mit Rücknahme, Signal `actionError`
- [ ] `features/program-board/program-board.component.{ts,html}` — `cdkDropListGroup`, Backlog und
  Entwicklungs-Zellen als `cdkDropList`, IP-Zellen ohne Drop-Zone, Karten über `app-board-card`
- [ ] `features/teams/teams.component.html` — Hinweistext in der Bestätigung
- [ ] `src/test/java/.../cucumber/steps/FeatureSteps.java` — bestehende Prüfung `scheduled == false` bleibt gültig
- [ ] `spec/user-stories/6-teams-erfassen.md`, `src/test/resources/features/teams/6-teams-erfassen.feature` —
  Delta US-6 einpflegen

## Delta

`spec/changes/13-feature-einplanen/delta.md` — Neu (US-13): 7 AK, 8 Szenarien (davon gegenüber dem Entwurf neu:
«Einplanen bei einem unbekannten Team wird abgelehnt», «Bearbeiten eines eingeplanten Features lässt die Einplanung
stehen»; umformuliert: «Einplanen in eine Iteration, die es im PI nicht gibt, wird abgelehnt»). Geändert (US-6):
AK 5 um den Hinweis in der Bestätigung ergänzt. Entfernt: keine.

## Architektur-Entscheide

- **Domain:** `Scheduling(UUID teamId, int iterationNumber)` als Record; `Feature.schedule(pi, teamId, number)`
  prüft, dass das PI das eigene ist, die Nummer existiert (sonst `InvalidFeatureException("Die Iteration gehört
  nicht zu diesem PI.")`) und nicht die IP-Iteration ist (`"In der IP-Iteration wird keine Feature-Arbeit
  geplant."`). Regeln im Record, Service lädt nur — mutationstestbar ohne Mocks
- **Application:** `FeatureService.schedule` lädt PI, Feature (im PI, sonst 404) und Team (`TeamRepository`, sonst
  `TeamNotFoundException` → 404, wie `FeatureDependencyService` bei Referenzen im Body); `unschedule` ist
  idempotent (Backlog-Feature zurücknehmen → 200, unverändert)
- **REST:** `PUT /v1/program-increments/{piId}/features/{id}/scheduling` mit `{ teamId, iterationNumber }` → 200
  mit dem Feature; 400 bei IP/unbekannter Nummer/fehlendem Feld; 404 bei PI, Feature, Team.
  `DELETE …/scheduling` → 200 mit dem Feature. Eigene Unterressource statt Felder im bestehenden `PUT /{id}`: Das
  Formular zum Bearbeiten soll die Einplanung nicht überschreiben, und Drag & Drop schickt nur, was sich ändert
- **Persistenz:** Migration V9 fügt `team_id UUID REFERENCES team (id)` (ohne `ON DELETE`) und
  `iteration_number INTEGER` hinzu, `CHECK ((team_id IS NULL) = (iteration_number IS NULL))`, Index auf `team_id`.
  Ändert keine Daten (alle bestehenden Features bleiben im Backlog)
- **Team entfernen — Abwägung:** `entscheide.md` nahm `ON DELETE SET NULL` an. Das setzt aber nur `team_id` auf
  null; `iteration_number` bliebe stehen und widerspräche der CHECK-Regel (PostgreSQL erlaubt in `SET NULL (…)`
  nur Spalten des Fremdschlüssels). Gewählt: `TeamService.remove` ruft in derselben Transaktion
  `FeatureRepository.unscheduleTeam(teamId)`; der Fremdschlüssel ohne `ON DELETE` lässt ein vergessenes Zurückgeben
  laut scheitern statt still Daten stehen zu lassen. Die Regel ist so im Unit-Test sichtbar, nicht nur im Schema
- **Board:** `ProgramBoard` bekommt `scheduled` (Backlog-Reihenfolge); das Frontend filtert pro Zelle. Eine
  Zellen-Struktur im Domain-Modell erst mit #14 (Last pro Zelle), wenn sie gebraucht wird
- **Frontend:** `BoardService` bekommt die Methoden zum Einplanen (er hält das Board-Signal, das sich optimistisch
  ändert) und nutzt den Port `FeaturesRepository`. Karte als eigene Komponente `app-board-card` unter
  `features/program-board/` (kommt im Backlog und in jeder Zelle vor; das Formular hat eigenen Zustand). Fehler beim
  Speichern: Board auf den Stand vorher zurück, Meldung aus `{ message }` als `role="alert"` über dem Board
- **Abhängigkeit:** `@angular/cdk` (Version passend zu `@angular/core`). Laut `entscheide.md` vorab per
  `chore/`-PR auf `main` — fehlt dort noch (→ Offene Fragen)
- **Keine neue ADR.** arc42 nicht betroffen; die REST-Tabelle in `spec/fachliche-spezifikation.md` führt schon die
  Feature-Endpunkte von #9 nicht, sie wird hier nicht nachgezogen

## Aufgaben

- [ ] 1. Domain: `Scheduling`, `ProgramIncrement.iteration(n)`, `Feature.schedule/unschedule/scheduled`, `revise`
  behält die Einplanung — erledigt, wenn `FeatureTest`/`ProgramIncrementTest` grün und PIT ≥ 80 %
- [ ] 2. Migration V9 + `FeatureEntity`/`FeatureRepositoryAdapter` (Mapping, `unscheduleTeam`) — erledigt, wenn
  `SmokeIT` (Hibernate-Validate) grün
- [ ] 3. `FeatureUseCase`/`FeatureService` mit `schedule`/`unschedule` — erledigt, wenn `FeatureServiceTest` grün
- [ ] 4. `TeamService.remove` gibt Features zurück — erledigt, wenn `TeamServiceTest` grün
- [ ] 5. `ProgramBoard`/`BoardService` trennen Backlog und Eingeplante — erledigt, wenn `BoardServiceTest` grün
- [ ] 6. REST: `SchedulingRequest`, Endpunkte, `FeatureDto`, `BoardDto` + Feature-Datei 13 mit Steps — erledigt,
  wenn `CucumberIT` für US-13 grün
- [ ] 7. Frontend-Kern: Modelle, Port, Adapter, `BoardService.schedule/unschedule` mit Rücknahme — erledigt, wenn
  `board.service.spec.ts` grün
- [ ] 8. `app-board-card` mit Aktion «Einplanen …» — erledigt, wenn `board-card.component.spec.ts` grün
- [ ] 9. Board mit CDK-Drop-Zonen (ohne IP) und Karten in Zellen — erledigt, wenn
  `program-board.component.spec.ts` grün
- [ ] 10. Teams: Hinweistext in der Bestätigung — erledigt, wenn `teams.component.spec.ts` grün
- [ ] 11. Stories 13 und 6 einpflegen, Feature-Dateien gleichlautend, Delta `eingepflegt` — erledigt, wenn
  `check-story-drift.sh` ohne Meldung und `scripts/gate.sh` grün

## Test-Strategie

- [ ] Unit (`FeatureTest`): Einplanen in eine Entwicklungs-Iteration setzt Team und Nummer; Einplanen in die
  IP-Iteration (Nummer = letzte) wirft mit der Meldung aus dem Delta; Nummer 0, letzte + 1 und negativ werfen «Die
  Iteration gehört nicht zu diesem PI.»; Nummer 1 und vorletzte gehen (Grenzen); ein PI mit anderer ID wird
  abgelehnt; Umplanen ersetzt die Zelle (höchstens eine); `unschedule` auf eingeplantem und auf Backlog-Feature
  ergibt dasselbe; `revise` behält die Einplanung; `scheduled()` folgt der Einplanung
- [ ] Unit (`ProgramIncrementTest`): `iteration(n)` liefert für 1…Anzahl die Iteration mit dieser Nummer, sonst leer
- [ ] Unit (`FeatureServiceTest`): `schedule` speichert das eingeplante Feature; unbekanntes PI / Feature / Feature
  aus anderem PI / unbekanntes Team → jeweilige NotFound-Exception, nichts gespeichert; `unschedule` speichert ohne
  Einplanung
- [ ] Unit (`TeamServiceTest`): `remove` gibt die Features des Teams zurück, bevor das Team gelöscht wird; bei
  unbekanntem Team wirft es, wie bisher
- [ ] Unit (`BoardServiceTest`): eingeplante Features fehlen im Backlog und stehen in `scheduled`, Reihenfolge wie im
  Backlog (bestehende Tests mit gemocktem `scheduled()` auf echte Einplanung umstellen — keine Spezifikationsänderung,
  nur die Testdaten)
- [ ] Integration (`CucumberIT`, US-13): die 8 Szenarien des Deltas, alle ohne Tag — einplanen,
  verschieben, zurück ins Backlog, IP abgelehnt (400), Nummer 7 abgelehnt (400), unbekanntes Team (404), Bearbeiten
  lässt Einplanung stehen, entferntes Team gibt Features zurück (belegt auch die Migration und den Fremdschlüssel)
- [ ] Unit Frontend (`board.service.spec.ts`): Karte springt sofort in die Zelle (Signal vor der Antwort), nach
  Erfolg bleibt sie; bei Fehler Board wie vorher und `actionError` mit der Server-Meldung bzw. der allgemeinen
  Meldung; Zurück ins Backlog analog
- [ ] Unit Frontend (`board-card.component.spec.ts`): «Einplanen …» klappt das Formular auf; Iterationsauswahl ohne
  IP; «Einplanen» meldet Team und Nummer; «Abbrechen» schliesst ohne Meldung; «Zurück ins Backlog» nur bei
  eingeplanten Karten; alles per Tastatur erreichbar (native `select`/`button`)
- [ ] Unit Frontend (`program-board.component.spec.ts`): eingeplante Karte erscheint in ihrer Zelle und nicht im
  Backlog; Entwicklungs-Zellen und Backlog sind Drop-Zonen, IP-Zellen nicht; ein Drop-Ereignis (über
  `cdkDropListDropped` ausgelöst) ruft `schedule` bzw. `unschedule`; die Fehlermeldung erscheint als Alert
- [ ] Unit Frontend (`teams.component.spec.ts`): Bestätigung enthält den Hinweis zu den eingeplanten Features
  (Delta US-6)
- [ ] `@Unit` in der Feature-Datei: AK Tastatur, AK Rücknahme bei Fehler und AK Hinweistext je mit Szenario und
  `# Vitest:`-Verweis (Szenarien dafür ergänzt `bdd-cucumber-author` beim Umsetzen im Delta, falls nötig)
- [ ] Prüfung von Hand: echtes Ziehen mit Maus und Touch im Browser (jsdom simuliert kein CDK-Ziehen), IP-Spalte
  nimmt nichts an — Screenshot/GIF im PR

## Offene Fragen

1. **`@angular/cdk` fehlt auf `main`.** `entscheide.md` sieht einen `chore/`-PR vorab vor. Machst du den vorher,
   oder soll die Abhängigkeit in diesem PR kommen (interaktiv erlaubt; CI setzt dann `needs-human-review`)?
2. **Szenario «Iteration eines anderen PI»:** Da Iterationen nur über ihre Nummer im PI adressiert werden, ist
   «Iteration 1 von PI 27.2» nicht von «Iteration 1 von PI 27.1» unterscheidbar. Vorschlag im Delta: «Einplanen in
   eine Iteration, die es im PI nicht gibt» mit Nummer 7, gleiche Meldung. Einverstanden?
3. **Team entfernen über die Anwendung statt `ON DELETE SET NULL`** (Abwägung oben) — weicht von der Annahme in
   `entscheide.md` ab. Einverstanden?
4. **Delta US-6 unter «Geändert»:** `entscheide.md` hielt den Hinweistext nur im Delta US-13 fest. Nach
   `story-delta.md` ändert sich das Ergebnis von US-6 AK 5 (die Bestätigung zeigt mehr), darum ein eigener Abschnitt.
   Einverstanden?
5. **Neue Szenarien** «unbekanntes Team» (404) und «Bearbeiten lässt Einplanung stehen» — beide Lücken im Entwurf;
   ins Delta aufgenommen. Einverstanden?
