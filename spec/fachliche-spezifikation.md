# Fachliche Spezifikation — PIkel

## Zweck

PIkel unterstützt das **PI Planning** nach SAFe: Ein Agile Release Train (ART) plant für ein Program Increment (PI)
Features, verteilt sie auf Teams und Iterationen, macht Abhängigkeiten sichtbar und hält PI-Ziele fest.

## Begriffe

| Begriff | Bedeutung |
|---------|-----------|
| Program Increment (PI) | Planungszeitraum des ART, typisch 8–12 Wochen, mit Name, Start- und Enddatum |
| ART | Agile Release Train: Verbund von Teams, die gemeinsam planen |
| Team | Agiles Team im ART mit Kapazität pro Iteration |
| Iteration | Sprint innerhalb eines PI |
| Feature | Fachliche Leistung, die im PI umgesetzt wird |
| Abhängigkeit | Ein Team braucht etwas von einem anderen bis zu einer Iteration |
| PI Objective | Ziel eines Teams für das PI, mit Business Value |

## Datenmodell (aktueller Stand)

### ProgramIncrement

| Feld | Typ | Regel |
|------|-----|-------|
| `id` | UUID | vom Server vergeben |
| `name` | Text | Pflicht, getrimmt, höchstens 100 Zeichen |
| `startDate` | Datum | Pflicht |
| `endDate` | Datum | Pflicht, nach `startDate` |

## REST-API (aktueller Stand)

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| `GET` | `/api/v1/program-increments` | Alle PIs, aufsteigend nach Startdatum |
| `POST` | `/api/v1/program-increments` | PI anlegen → 201; 400 mit `{ "message": … }` bei Regelverletzung |

## User Stories

| ID | Titel | Status |
|----|-------|--------|
| [US-1](user-stories/1-pi-anlegen.md) | PI anlegen (Walking Skeleton) | Umgesetzt |

## Ausblick (noch nicht spezifiziert)

Teams und Iterationen, Feature-Backlog, Planungs-Board (Teams × Iterationen), Abhängigkeiten, PI Objectives,
Kapazität, Anmeldung mit Rollen (RTE, Product Owner, Team).
