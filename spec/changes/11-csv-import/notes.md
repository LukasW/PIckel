# Notizen US-11

## Annahmen im Nachtlauf

- **Zeilennummer:** Das Delta sagt nur «nennt die Zeilennummer». Angenommen: Datensätze ab der Kopfzeile (= Zeile 1),
  leere Zeilen zählen mit, ein Zeilenumbruch in Anführungszeichen beginnt keine neue Zeile. Bei einem nicht
  geschlossenen Anführungszeichen nennt die Meldung die Zeile, in der es beginnt («Das Anführungszeichen ist nicht
  geschlossen.»)
- **Meldungen der Grenzen:** «Die Datei darf höchstens 1 MB gross sein.» (1 MB = 1 000 000 Bytes UTF-8, BOM
  eingeschlossen) und «Die Datei darf höchstens 1000 Datenzeilen haben.» (leere Zeilen zählen nicht)
- **Nicht-Zahlen** in WSJF-Rang oder Schätzung (`null`, `1.5`, `abc`, zu gross für `int`) geben dieselbe Meldung wie
  ein Wert unter der Grenze
- **Spaltennamen** werden getrimmt und exakt verglichen (Gross-/Kleinschreibung zählt); bei doppelter Spalte gilt die
  erste. Fehlt in einer Zeile ein Feld, ist es leer; überzählige Felder werden ignoriert
- **Kopfzeile ohne Trennzeichen** (nur `Titel`): Komma; macht keinen Unterschied, weil es nur eine Spalte gibt
- **Anführungszeichen** öffnen nur am Anfang eines Felds (nach Leerraum); `Export "CSV"` ohne umschliessende
  Anführungszeichen bleibt wörtlich erhalten
- **Byte Order Mark** am Dateianfang wird ignoriert (Excel «CSV UTF-8»)
- **Nur Kopfzeile:** 0 Features, kein Fehler
- **Schnittstelle:** eigener Endpunkt `…/features/import` mit `text/csv` statt Multipart: Der Client liest die Datei
  mit `file.text()`, so braucht es keine neue Abhängigkeit
- **Oberfläche:** Datei-Feld «CSV importieren» über der Liste, Erfolg als `role="status"` («1 Feature importiert.»
  bzw. «<n> Features importiert.»), Fehler im bestehenden `role="alert"`. Die Grenzen prüft nur der Server; der
  Client zeigt dessen Meldung. Nach jeder Wahl wird das Feld geleert, damit dieselbe Datei nach einer Korrektur
  nochmals gewählt werden kann

## Offenes Risiko

- **Andere Kodierung als UTF-8:** Eine Datei in Windows-1252 (Excel «CSV (Trennzeichen-getrennt)») wird nicht
  abgelehnt, sondern mit Ersatzzeichen (U+FFFD) statt Umlauten importiert; `file.text()` liest immer UTF-8. Das
  Delta legt UTF-8 fest und sagt nichts zur Ablehnung; ob erkennen und ablehnen, entscheidet eine Folge-Story
  (Spec-Reviewer)

## Umsetzung

- `Feature` hat neu die Konstanten `INVALID_WSJF_RANK` und `INVALID_ESTIMATE`, damit der Import für Nicht-Zahlen
  denselben Text meldet, ohne ihn zu kopieren
- `FeatureSteps`: «erscheint … im Backlog» und «ist das Backlog … leer» waren an die Antwort des Erfassens bzw.
  Löschens gebunden; der Test-Autor hat sie so erweitert, dass sie auch nach einem Import gelten, ohne eine Prüfung
  für US-9/US-10 zu verlieren
- Die Test-Agenten konnten in ihrer Sandbox weder Maven noch npm starten; den roten Lauf (Backend:
  `UnsupportedOperationException` in jedem neuen Test) hat die Haupt-Session nachgeholt. Die Vitest-Tests liefen
  erst nach der Implementierung (grün); ihr roter Lauf ist nicht belegt
- `npm ci` im Worktree (fehlende `node_modules`); `package.json` unverändert
