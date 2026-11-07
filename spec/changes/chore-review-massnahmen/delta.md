# Delta US-1 — PI anlegen: Fehlervertrag, fehlende Szenarien, Formular (ohne Issue, Chore aus dem Projekt-Review)

Story: `spec/user-stories/1-pi-anlegen.md` (bestehend) · Status: eingepflegt

## Warum

Das Projekt-Review vom 7. November 2026 fand: Fehlt ein Feld, ist der Name leer, das JSON kaputt oder ein Datum
ungültig, antwortet die API mit 400, aber ohne `{ "message": … }`. Die Oberfläche zeigt dann nur «Das hat nicht
geklappt». AK 2 und AK 5 hatten kein Szenario, das Formular gab keine Rückmeldung am Feld.

## Entscheide

- **Eine Prüfstelle:** Bean-Validation-Annotationen am Request entfallen, die Regeln stehen nur in der Domain
  (CLAUDE.md → «doppelte Prüfungen widersprechen sich irgendwann»)
- **Fehlerformat bleibt `{ "message": … }`**, jetzt für jede 400; Problem Details (RFC 9457) erst mit einem zweiten
  Client (ADR 0003)
- **Kein `Location`-Header** bei 201: Es gibt keinen Endpunkt für ein einzelnes PI
- **Sortierung nur auf dem Server:** Der Client übernimmt die Reihenfolge und lädt nach dem Anlegen neu

## Neu

### AK: Eine unvollständige oder ungültige Anfrage wird mit einer verständlichen Meldung abgelehnt

### AK: Das Formular zeigt fehlende oder zu lange Angaben am Feld an und legt dann nichts an

### AK: Start- und Enddatum erscheinen im Format TT.MM.JJJJ

### Szenario: Liste ist aufsteigend nach Startdatum sortiert
- **Gegeben sei** das PI «PI 27.2» vom 2027-04-05 bis 2027-06-25 und das PI «PI 27.1» vom 2027-01-04 bis 2027-03-26
- **Dann** zeigt die Liste zuerst «PI 27.1» und dann «PI 27.2»

### Szenario: Enddatum gleich Startdatum wird abgelehnt
- **Wenn** ich ein PI mit gleichem Start- und Enddatum anlege
- **Dann** wird es mit «Das Enddatum muss nach dem Startdatum liegen.» abgelehnt

### Szenario: Fehlendes Datum wird abgelehnt
- **Wenn** ich ein PI ohne Enddatum anlege
- **Dann** wird es mit «Start- und Enddatum sind Pflicht.» abgelehnt

### Szenario: Ungültiges Datum wird abgelehnt
- **Wenn** ich ein PI mit dem Enddatum «2027-13-45» anlege
- **Dann** wird es mit «Die Anfrage enthält einen ungültigen Wert.» abgelehnt

### Szenario: Anfrage ohne gültiges JSON wird abgelehnt
- **Wenn** ich «{ kein JSON» sende
- **Dann** wird es mit «Die Anfrage ist kein gültiges JSON.» abgelehnt

### Szenario: Anfrage ohne Inhalt wird abgelehnt
- **Wenn** ich eine Anfrage ohne Inhalt sende
- **Dann** wird sie mit «Die Anfrage ist unvollständig.» abgelehnt

### Szenario: Ohne PIs zeigt die Seite einen Leerzustand
- **Wenn** ich die Seite ohne PIs öffne
- **Dann** sehe ich «Noch keine PIs»

### Szenario: Formular zeigt fehlende Angaben am Feld an
- **Wenn** ich das Formular ohne Angaben absende
- **Dann** sehe ich am Namen «Der Name ist Pflicht.», und es wird kein PI angelegt

### Szenario: Daten erscheinen im Schweizer Format
- **Gegeben sei** das PI «PI 27.1» vom 2027-01-04 bis 2027-03-26
- **Dann** sehe ich «04.01.2027 – 26.03.2027»

## Geändert

### Szenario: Leerer Name wird abgelehnt
- **Vorher:** `@Unit`, belegt nur durch `ProgramIncrementTest → rejectsBlankName`; über REST kam ein
  Violation-Report ohne `message`
- **Nachher:**
  - **Wenn** ich das PI « » anlege
  - **Dann** wird das Anlegen mit der Meldung «Der Name darf nicht leer sein.» abgelehnt (über REST, `CucumberIT`)
- **Grund:** Das `@Unit`-Tag verdeckte, dass der REST-Vertrag nicht eingehalten war
- **Tests:** `@Unit` entfällt, das Szenario läuft in `CucumberIT`

## Entfernt

keine
