# Plan #11 — Features per CSV importieren

Ohne `/flow-plan` im Nachtlauf geschrieben (`/flow-implement` Schritt 4).

## Aufgaben

- [x] Domain: `FeatureCsv.parse` (Trennzeichen an der Kopfzeile, RFC 4180, BOM, Grenzen 1 MB / 1000 Datenzeilen,
  Meldungen mit Zeilennummer); Regeln je Feature über `Feature.create` wie beim Erfassen
- [x] Port und Service: `FeatureUseCase#importCsv`, erst die ganze Datei prüfen, dann speichern (`@Transactional`)
- [x] REST: `POST /api/v1/program-increments/{piId}/features/import` (`text/csv`), 201 `{ "imported": n }`;
  400/404 über die bestehenden Mapper
- [x] Frontend: Port, HTTP-Adapter, `FeaturesService#importCsv` mit Signal `imported`; im Backlog ein Datei-Feld
  «CSV importieren» und die Meldung «<n> Features importiert.»
- [x] Story `spec/user-stories/11-csv-import.md`, Feature-Datei `11-csv-import.feature`

Keine Liquibase-Migration: Die Tabelle `features` (V5) trägt alles.

## Test-Strategie

| Kriterium | Ebene |
|---|---|
| Datenzeile → Feature (AK 1) | `FeatureCsvTest`, `FeatureServiceTest` (speichert alle in Reihenfolge), `CucumberIT` |
| Trennzeichen, Anführungszeichen (AK 2) | `FeatureCsvTest`, `CucumberIT` (Komma mit Anführungszeichen) |
| Alles oder nichts, Zeile und Grund (AK 3) | `FeatureCsvTest`, `FeatureServiceTest` (nichts gespeichert), `CucumberIT` |
| Anzahl in der Oberfläche (AK 4) | Vitest `features.service.spec.ts`, `feature-backlog.component.spec.ts` |
| Grenzen (AK 5) | `FeatureCsvTest` (Grenzwerte), `CucumberIT` (je eine Ablehnung über REST) |
