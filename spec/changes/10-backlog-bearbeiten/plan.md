# Plan #10 — Backlog nach WSJF-Rang bearbeiten

Ohne `/flow-plan` im Nachtlauf geschrieben (`/flow-implement` Schritt 4).

## Aufgaben

- [x] Domain: `Feature#revise` (gleiche ID und PI, Regeln des Compact Constructors), `FeatureNotFoundException`
- [x] Ports: `FeatureUseCase#update`/`#remove`, `FeatureRepository#findOne`/`#remove`
- [x] Service: Feature nur im eigenen PI finden (sonst 404), `@Transactional`
- [x] REST: `PUT` und `DELETE /api/v1/program-increments/{piId}/features/{id}`, `FeatureNotFoundExceptionMapper`
- [x] Frontend: Port und HTTP-Adapter, `FeaturesService#update`/`#remove`, Backlog mit «Bearbeiten» (dasselbe
  Formular wie beim Erfassen) und «Löschen» mit Inline-Bestätigung, Leerzustand mit Hinweis auf den CSV-Import
- [x] Story `spec/user-stories/10-backlog-bearbeiten.md`, Feature-Datei

Keine Liquibase-Migration: Die Tabelle `features` (V5) trägt alles.

## Test-Strategie

| Kriterium | Ebene |
|---|---|
| Sortierung (AK 1, 2) | `CucumberIT` (Reihenfolge der REST-Antwort); Logik seit US-9 in `FeatureServiceTest` |
| Anzeige der Felder (AK 1) | Vitest `feature-backlog.component.spec.ts` |
| Bearbeiten mit denselben Regeln (AK 3) | `FeatureTest` (revise), `FeatureServiceTest` (update), `CucumberIT` (PUT), Vitest (Formular) |
| Löschen nach Bestätigung (AK 4) | `FeatureServiceTest` (remove), `CucumberIT` (DELETE), Vitest (Bestätigung) |
| Leerzustand (AK 5) | Vitest |
| 404 bei unbekanntem/fremdem Feature (AK 6) | `FeatureServiceTest` (fremdes PI bei update und remove), `CucumberIT` (unbekannt und fremdes PI, je bei PUT und DELETE) |
