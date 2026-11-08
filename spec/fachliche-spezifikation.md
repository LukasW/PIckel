# Fachliche Spezifikation — PIckel

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
| `iterationLengthWeeks` | Ganzzahl | 1 bis 4, ohne Angabe 2; der Zeitraum ergibt mindestens zwei Iterationen |
| `iterations` | Liste von Iteration | berechnet, nicht gespeichert (siehe unten) |
| `businessContext` | Text | ohne Formatierung, Zeilenumbrüche bleiben; leer erlaubt, höchstens 10 000 Zeichen |
| `artObjectives` | Text | PI Objectives des ART; Regeln wie `businessContext` |
| `links` | Liste von PiLink | in der Reihenfolge des Hinzufügens |

### PiLink

| Feld | Typ | Regel |
|------|-----|-------|
| `id` | UUID | vom Server vergeben |
| `title` | Text | Pflicht, getrimmt, höchstens 100 Zeichen |
| `url` | Text | Pflicht, beginnt mit `http://` oder `https://` (Schema ohne Rücksicht auf Gross- und Kleinschreibung) |

### Iteration (abgeleitet)

Die Domain teilt den Zeitraum eines PI ab `startDate` in Iterationen zu `iterationLengthWeeks` Wochen. Jede endet am
Tag vor dem Beginn der nächsten, die letzte am `endDate` des PI und ist bei einem Rest kürzer. Spätere Stories
verweisen auf eine Iteration über PI und `number`; das bleibt stabil, weil Länge und Zeitraum eines PI nicht
änderbar sind.

| Feld | Typ | Regel |
|------|-----|-------|
| `number` | Ganzzahl | 1, 2, … lückenlos |
| `name` | Text | «Iteration 1», «Iteration 2» …; die letzte heisst «IP» |
| `startDate` | Datum | Startdatum des PI plus (`number` − 1) Iterationslängen |
| `endDate` | Datum | Tag vor der nächsten Iteration, bei der letzten das Enddatum des PI |
| `innovationAndPlanning` | Wahrheitswert | nur bei der letzten Iteration (IP-Iteration) |

### Team

Gehört zum impliziten ART und gilt für alle PIs (ADR 0005).

| Feld | Typ | Regel |
|------|-----|-------|
| `id` | UUID | vom System vergeben |
| `name` | Text | Pflicht, getrimmt, höchstens 50 Zeichen; eindeutig ohne Rücksicht auf Gross- und Kleinschreibung |

## REST-API (aktueller Stand)

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| `GET` | `/api/v1/program-increments` | Alle PIs, aufsteigend nach Startdatum |
| `GET` | `/api/v1/program-increments/{id}` | Ein PI mit Iterationen; 404 mit `{ "message": … }`, wenn es die ID nicht gibt |
| `POST` | `/api/v1/program-increments` | PI anlegen (`iterationLengthWeeks` optional) → 201 mit dem neuen PI und `Location`; 400 mit `{ "message": … }` bei Regelverletzung, fehlendem Inhalt, ungültigem JSON oder ungültigem Wert |
| `PUT` | `/api/v1/program-increments/{id}/business-context` | `businessContext` und `artObjectives` ersetzen (fehlendes Feld = leer) → 200 mit dem PI; 400 bei zu langem Text, 404 ohne PI |
| `POST` | `/api/v1/program-increments/{id}/links` | Link mit `title` und `url` anhängen → 200 mit dem PI; 400 bei Regelverletzung, 404 ohne PI |
| `DELETE` | `/api/v1/program-increments/{id}/links/{linkId}` | Link entfernen → 200 mit dem PI; 404 mit `{ "message": … }` ohne PI oder ohne Link |
| `GET` | `/api/v1/teams` | Alle Teams, alphabetisch nach Name |
| `POST` | `/api/v1/teams` | Team erfassen (`name`) → 201 mit dem Team; 400 mit `{ "message": … }` bei ungültigem oder doppeltem Namen |
| `PUT` | `/api/v1/teams/{id}` | Team umbenennen (`name`) → 200 mit dem Team; 400 wie beim Erfassen; 404, wenn es die ID nicht gibt |
| `DELETE` | `/api/v1/teams/{id}` | Team entfernen → 204; 404, wenn es die ID nicht gibt |

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

### Entscheide vor den Stories

- Aggregatschnitt: ein impliziter ART, Teams am ART, Kapazität und Board am PI — [ADR 0005](../docs/adr/0005-aggregatschnitt-mvp.md)
- Kapazität und Schätzung in Story Points, Iterationen aus fester Länge mit IP-Iteration am Ende —
  [ADR 0006](../docs/adr/0006-kapazitaet-story-points.md)
- Anmeldung und Rollen: erst in M2 mit dem Bereich «Zusammenarbeit»; M1 läuft ohne Anmeldung und nicht öffentlich
- Import: nur CSV (siehe «Nicht im MVP»)

### Offene Fragen

- Echtzeit: WebSocket oder SSE, und wie das Frontend den State nachzieht — ADR mit der ersten Story aus Bereich 8
- Anonyme Abstimmung per Link: wie sie zu Anmeldung und Rollen passt — mit den Stories aus Bereich 7
