# Delta US-8 — Business-Kontext und ART-Ziele am PI (Issue #8)

Story: `spec/user-stories/8-business-kontext.md` (neu) · Status: entwurf

## Warum

Das Planning beginnt mit dem Business-Kontext und den Zielen des ART. Liegen sie am PI, sehen alle Teams dieselbe
Ausgangslage, ohne zwischen Folien und Tool zu wechseln.

## Entscheide

- **Freitext und Links:** ein Textfeld für den Business-Kontext, eines für die PI Objectives des ART, dazu eine Liste
  von Links (Titel und URL), etwa auf Confluence. Kein strukturiertes Objective-Modell, das kommt mit den
  Team-PI-Objectives in M2
- **Nur `http` und `https`** als Link, damit ein Link kein Skript ausführt
- **Text ohne Formatierung**, Zeilenumbrüche bleiben erhalten; kein Markdown in M1

## Neu

### AK: Am PI lassen sich ein Business-Kontext und die PI Objectives des ART als Text erfassen und ändern, je höchstens 10 000 Zeichen

### AK: Am PI lassen sich Links mit Titel (Pflicht, höchstens 100 Zeichen) und URL (Pflicht, `http` oder `https`) hinzufügen und entfernen

### AK: Die Detailseite des PI zeigt Text mit Zeilenumbrüchen und die Links, die sich in einem neuen Tab öffnen

### AK: Ohne Inhalt zeigt der Bereich einen Hinweis, dass noch kein Business-Kontext erfasst ist

### Szenario: Business-Kontext wird erfasst
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich für "PI 27.1" den Business-Kontext "Fokus auf Self-Service" erfasse
- **Dann** zeigt "PI 27.1" den Business-Kontext "Fokus auf Self-Service"

### Szenario: Link wird hinzugefügt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich für "PI 27.1" den Link "Vision" mit der URL "https://wiki.example.ch/vision" hinzufüge
- **Dann** zeigt "PI 27.1" den Link "Vision"

### Szenario: Link ohne http oder https wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich für "PI 27.1" den Link "Vision" mit der URL "javascript:alert(1)" hinzufüge
- **Dann** wird das Hinzufügen mit der Meldung "Die URL muss mit http:// oder https:// beginnen." abgelehnt

### Szenario: Zu langer Business-Kontext wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich für "PI 27.1" einen Business-Kontext mit 10 001 Zeichen erfasse
- **Dann** wird das Erfassen mit der Meldung "Der Text darf höchstens 10 000 Zeichen lang sein." abgelehnt

## Geändert

keine

## Entfernt

keine
