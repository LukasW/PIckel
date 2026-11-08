# Delta US-11 — Features per CSV importieren (Issue #11)

Story: `spec/user-stories/11-csv-import.md` (neu) · Status: eingepflegt

## Warum

Die Features liegen vor dem Planning meist schon im Issue-Tracker. Ein CSV-Import erspart das Abtippen; eine
Synchronisation mit Jira ist nicht im MVP.

## Entscheide

- **Format:** UTF-8, Kopfzeile Pflicht mit den Spalten `Titel`, `Beschreibung`, `WSJF-Rang`, `Owner`, `Schätzung`;
  nur `Titel` muss vorkommen, die Reihenfolge der Spalten ist frei. Trennzeichen Semikolon oder Komma, erkannt an
  der Kopfzeile (Excel in der Schweiz schreibt Semikolon). Felder in Anführungszeichen nach RFC 4180
- **Alles oder nichts:** Ist eine Zeile ungültig, wird nichts importiert, und die Meldung nennt die Zeilennummer.
  Ein halber Import liesse sich nur mühsam bereinigen
- **Regeln** wie beim Erfassen (#9); unbekannte Spalten werden ignoriert, leere Zeilen übersprungen
- **Anhängen:** Der Import ergänzt das Backlog; kein Abgleich mit bestehenden Features (keine Dubletten-Erkennung)
- **Grösse:** höchstens 1 MB und 1000 Zeilen, damit ein Fehlgriff den Server nicht belastet
- **Zeilennummer** (nachgetragen im Nachtlauf, Annahme, `notes.md`): gezählt werden Datensätze ab der Kopfzeile
  (= Zeile 1), leere Zeilen eingeschlossen; ein Zeilenumbruch in Anführungszeichen beginnt keine neue Zeile
- **Schnittstelle** (nachgetragen im Nachtlauf): `POST /api/v1/program-increments/{piId}/features/import` mit
  `Content-Type: text/csv`, Antwort 201 `{ "imported": <n> }`; 400 bzw. 404 mit `{ "message": … }` wie beim Erfassen

## Neu

### AK: Eine CSV-Datei mit Kopfzeile lässt sich ins Backlog eines PI importieren; jede Datenzeile wird ein Feature

### AK: Semikolon und Komma werden als Trennzeichen an der Kopfzeile erkannt, Felder in Anführungszeichen dürfen Trennzeichen und Zeilenumbrüche enthalten

### AK: Ist eine Zeile ungültig oder fehlt die Spalte `Titel`, wird nichts importiert und die Meldung nennt die Zeile und den Grund

### AK: Nach dem Import meldet die Oberfläche die Anzahl der importierten Features

### AK: Dateien über 1 MB oder mit mehr als 1000 Datenzeilen werden abgelehnt

### Szenario: CSV mit Semikolon wird importiert
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich ins PI "PI 27.1" diese CSV importiere:
  ```
  Titel;WSJF-Rang;Owner;Schätzung
  Portal;1;Anna;21
  Suche;2;Beat;13
  ```
- **Dann** wurden 2 Features importiert
- **Und** zeigt das Backlog von "PI 27.1" die Features "Portal", "Suche" in dieser Reihenfolge

### Szenario: CSV mit Komma und Anführungszeichen wird importiert
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich ins PI "PI 27.1" diese CSV importiere:
  ```
  Titel,Beschreibung
  "Export, CSV","Für den Issue-Tracker"
  ```
- **Dann** wurde 1 Feature importiert
- **Und** erscheint "Export, CSV" im Backlog von "PI 27.1"

### Szenario: Ungültige Zeile verhindert den ganzen Import
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich ins PI "PI 27.1" diese CSV importiere:
  ```
  Titel;WSJF-Rang
  Portal;1
  Suche;null
  ```
- **Dann** wird der Import mit der Meldung "Zeile 3: Der WSJF-Rang muss eine ganze Zahl ab 1 sein." abgelehnt
- **Und** ist das Backlog von "PI 27.1" leer

### Szenario: CSV ohne Spalte Titel wird abgelehnt
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich ins PI "PI 27.1" diese CSV importiere:
  ```
  Name;Owner
  Portal;Anna
  ```
- **Dann** wird der Import mit der Meldung "Die Kopfzeile braucht die Spalte «Titel»." abgelehnt

### Szenario: CSV mit mehr als 1000 Datenzeilen wird abgelehnt
Nachgetragen im Nachtlauf, belegt AK 5 über REST.
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich ins PI "PI 27.1" eine CSV mit 1001 Datenzeilen importiere
- **Dann** wird der Import mit der Meldung "Die Datei darf höchstens 1000 Datenzeilen haben." abgelehnt
- **Und** ist das Backlog von "PI 27.1" leer

### Szenario: CSV über 1 MB wird abgelehnt
Nachgetragen im Nachtlauf, belegt AK 5 über REST.
- **Gegeben sei** das PI "PI 27.1" vom "2027-01-04" bis "2027-03-26"
- **Wenn** ich ins PI "PI 27.1" eine CSV mit 1000001 Bytes importiere
- **Dann** wird der Import mit der Meldung "Die Datei darf höchstens 1 MB gross sein." abgelehnt
- **Und** ist das Backlog von "PI 27.1" leer

### Szenario: Import in ein unbekanntes PI wird abgelehnt
Nachgetragen im Nachtlauf, wie beim Erfassen (US-9).
- **Wenn** ich in ein unbekanntes PI diese CSV importiere: `Titel` / `Portal`
- **Dann** wird der Import mit dem Status 404 und der Meldung "Das PI gibt es nicht." abgelehnt

### Szenario: Oberfläche meldet die Anzahl der importierten Features
Nachgetragen im Nachtlauf, belegt AK 4 im Frontend (`@Unit`, Vitest).
- **Gegeben sei** das Backlog eines PI
- **Wenn** ich eine CSV-Datei mit 2 Features zum Import wähle
- **Dann** meldet die Oberfläche "2 Features importiert."
- **Und** zeigt das Backlog die importierten Features

## Geändert

keine

## Entfernt

keine
