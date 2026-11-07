# Fachliche Spezifikation — PIkel

Fachlicher Gesamtstand: Begriffe, Datenmodell mit Regeln, API. Zweck und Stakeholder stehen in
[`docs/arc42.md`](../docs/arc42.md) → Abschnitt 1, die einzelnen Änderungen mit Akzeptanzkriterien unter
[`user-stories/`](user-stories/); deren Status steht in der Story-Datei.

## Begriffe

| Begriff | Bedeutung |
|---------|-----------|
| Program Increment (PI) | Planungszeitraum des ART, typisch 8–12 Wochen, mit Name, Start- und Enddatum |
| ART | Agile Release Train: Verbund von Teams, die gemeinsam planen |
| Team | Agiles Team im ART mit Kapazität pro Iteration |
| Iteration | Sprint innerhalb eines PI |
| Feature | Fachliche Leistung, die im PI umgesetzt wird |
| IP-Iteration | Letzte Iteration eines PI für Innovation und Planung; dort wird keine Feature-Arbeit geplant |
| WSJF-Rang | Rang eines Features nach Weighted Shortest Job First; bestimmt die Reihenfolge im Backlog |
| Program Board | Raster Teams × Iterationen, auf dem die Features, Milestones und Abhängigkeiten eines PI liegen |
| Milestone | Termin oder Ereignis im PI, das auf dem Program Board über den Iterationen erscheint |
| Abhängigkeit | Ein Team braucht etwas von einem anderen bis zu einer Iteration; auf dem Board als Linie («Red String») |
| Konflikt | Abhängigkeit, deren Lieferung in einer späteren Iteration liegt als der Bedarf |
| PI Objective | Ziel eines Teams für das PI, mit Business Value; als Committed oder Uncommitted markiert |
| Business Owner | Bewertet die PI Objectives der Teams mit Business Value |
| Risiko (ROAM) | Risiko für das PI mit Owner und Status Resolved, Owned, Accepted oder Mitigated |
| Confidence Vote | Abstimmung «Fist of Five» (1–5) am Ende des Plannings, pro Team und für den ART |
| Facilitator | Moderiert das Planning, in der Regel der RTE |

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
| `POST` | `/api/v1/program-increments` | PI anlegen → 201 mit dem neuen PI (ohne `Location`); 400 mit `{ "message": … }` bei Regelverletzung, fehlendem Inhalt, ungültigem JSON oder ungültigem Wert |

## MVP-Umfang (noch nicht spezifiziert)

Das MVP deckt ab, was im Planning-Raum selbst wehtut: das Program Board, die Abhängigkeiten und den Confidence Vote.
Was Issue-Tracker wie Jira schon gut können, bleibt draussen. Die Bereiche geben nur die Richtung vor. Verbindlich
wird ein Bereich erst mit seinen User Stories, und die Akzeptanzkriterien stehen nur dort.

| Nr. | Bereich | Inhalt |
|-----|---------|--------|
| 1 | PI-Setup | PI mit Name, Zeitraum und Iterationen inkl. IP-Iteration; Teams des ART mit Kapazität pro Iteration; Business-Kontext und PI Objectives des ART als Freitext oder Links |
| 2 | Features und Backlog | Features manuell erfassen oder per CSV importieren (Titel, Beschreibung, WSJF-Rang, Owner); priorisierte Liste als Ausgangspunkt fürs Planning |
| 3 | Program Board | Features per Drag & Drop in die Zellen Team × Iteration legen; Kapazität pro Zelle geplant gegenüber verfügbar, bei Überlast rot; Milestones als Marker |
| 4 | Abhängigkeiten | Abhängigkeit zwischen zwei Features ziehen, als Linie auf dem Board; Hinweis bei einem Konflikt |
| 5 | Risiken | Risiken mit Owner und ROAM-Status erfassen; ROAM-Ansicht fürs Management-Review |
| 6 | Team-PI-Objectives | Objectives pro Team, Business Value durch den Business Owner, Committed oder Uncommitted |
| 7 | Confidence Vote | Anonym per Handy oder Link, pro Team und für den ART; Ergebnis live als Balken, Historie pro Planning |
| 8 | Zusammenarbeit | Mehrere Personen gleichzeitig auf dem Board, Änderungen sofort sichtbar; Rollen Facilitator, Team-Mitglied, Read-only |
| 9 | Export | Program Board als PDF oder PNG; Features mit Team und Iteration als CSV für den Issue-Tracker |

### Releases

| Release | Bereiche | Ergebnis |
|---------|----------|----------|
| M1 «Klebezettel-Wand ersetzt» | 1–4 | Ein ART plant ein PI auf dem digitalen Board statt an der Wand |
| M2 «Planning-Rituale» | 5–8 | Risiken, Objectives und Confidence Vote im Tool; mehrere Personen planen gleichzeitig |
| M3 «Export» | 9 | Ergebnis geht nach Confluence und in den Issue-Tracker zurück |

### Nicht im MVP

- Planung auf Story-Ebene, Burndowns, Zeiterfassung
- Zweiseitige Synchronisation mit Jira (nur CSV-Import und -Export)
- Mehrere ARTs, Solution Train
- Retrospektiven

### Offene Fragen

Bevor die ersten Stories entstehen, sind diese Fragen zu entscheiden. Was mehrere Stories betrifft, kommt als ADR
nach `docs/adr/`:

- Aggregatschnitt: Ist der ART der Wurzel-Container? Gehören Teams zum ART oder zum PI?
- Einheit der Kapazität: Story Points oder Personentage, fest oder pro ART wählbar
- Echtzeit: WebSocket oder SSE, und wie das Frontend den State nachzieht
- Anmeldung und Rollen: ab wann, und wie die anonyme Abstimmung per Link dazu passt
- Import: nur CSV oder auch direkt aus Jira
