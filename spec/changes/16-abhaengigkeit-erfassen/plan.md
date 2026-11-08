# Plan #16 — Abhängigkeit zwischen Features erfassen

Ohne `/flow-plan` im Nachtlauf geschrieben (`/flow-implement` Schritt 4). Delta: `delta.md` (nur «Neu»).

## Aufgaben

- [x] Domain: `FeatureDependency` (Record) mit `between(needing, needed, existing)` — Regeln Selbstbezug, gleiches PI,
  doppelt, direkte Umkehr; Meldungen über `InvalidFeatureException` (400 über den bestehenden Mapper)
- [x] Ports: `FeatureDependencyUseCase`, `FeatureDependencyRepository`
- [x] Application: `FeatureDependencyService` (list, add, remove)
- [x] Persistenz: Migration `V7__create_feature_dependency.sql` (neue Tabelle, Schlüssel aus beiden Feature-IDs,
  `ON DELETE CASCADE` für AK 5; ändert keine bestehenden Daten), Entity, Panache-Adapter
- [x] REST: `FeatureDependencyResource` unter `/api/v1/program-increments/{piId}/dependencies` (GET, POST 201,
  DELETE `/{needingId}/{neededId}` 204)
- [x] Frontend: Model, Port, HTTP-Adapter, `FeatureDependenciesService`; im Backlog je Feature «Braucht: …» mit
  «Entfernen», «Wird gebraucht von: …», Auswahl «Braucht Feature» mit «Abhängigkeit hinzufügen»
- [x] Story `spec/user-stories/16-abhaengigkeit-erfassen.md`, Feature-Datei `features/16-abhaengigkeit-erfassen.feature`

## Test-Strategie

- Unit: `FeatureDependencyTest` (Regeln, Reihenfolge), `FeatureDependencyServiceTest` (Mockito)
- Integration: alle sieben Szenarien in `CucumberIT` (REST-Vertrag, Persistenz, Kaskade beim Löschen)
- Vitest: `feature-dependencies.service.spec.ts`, `feature-backlog.component.spec.ts` (Anzeige beider Richtungen,
  Erfassen, Entfernen, Neuladen nach Löschen eines Features)
