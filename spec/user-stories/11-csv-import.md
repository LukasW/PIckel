# US-11: Features per CSV importieren

**ID:** US-11
**Issue:** [#11](https://github.com/LukasW/PIkel/issues/11)
**Status:** Umgesetzt
**Priorität:** Mittel
**Schätzung:** 5

## Beschreibung

Als Product Manager möchte ich Features aus einer CSV-Datei ins Backlog eines PI importieren, damit ich sie nicht aus
dem Issue-Tracker abtippen muss.

## Akzeptanzkriterien

1. Eine CSV-Datei mit Kopfzeile lässt sich ins Backlog eines PI importieren; jede Datenzeile wird ein Feature
2. Semikolon und Komma werden als Trennzeichen an der Kopfzeile erkannt, Felder in Anführungszeichen dürfen
   Trennzeichen und Zeilenumbrüche enthalten
3. Ist eine Zeile ungültig oder fehlt die Spalte `Titel`, wird nichts importiert und die Meldung nennt die Zeile und
   den Grund
4. Nach dem Import meldet die Oberfläche die Anzahl der importierten Features
5. Dateien über 1 MB oder mit mehr als 1000 Datenzeilen werden abgelehnt

Format: UTF-8, Kopfzeile Pflicht mit den Spalten `Titel`, `Beschreibung`, `WSJF-Rang`, `Owner`, `Schätzung`; nur
`Titel` muss vorkommen, die Reihenfolge ist frei. Regeln wie beim Erfassen (US-9); unbekannte Spalten werden
ignoriert, leere Zeilen übersprungen. Der Import ergänzt das Backlog, ohne Abgleich mit bestehenden Features.

Nicht in dieser Story: Synchronisation mit Jira, Dubletten-Erkennung.

## BDD-Szenarien

Siehe [`src/test/resources/features/features/11-csv-import.feature`](../../src/test/resources/features/features/11-csv-import.feature).

## Änderungen

- #11: Features per CSV importieren — `spec/changes/11-csv-import/delta.md`
