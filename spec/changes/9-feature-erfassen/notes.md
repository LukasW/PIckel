# Notizen US-9

## Annahmen im Nachtlauf

- **Backlog-Reihenfolge:** Das Delta nennt keine Sortierung. Angenommen: WSJF-Rang aufsteigend, Features ohne Rang am
  Ende, bei Gleichstand nach Titel (ohne Gross-/Kleinschreibung). Ein Backlog nach Rang ist der übliche Zweck; die
  Reihenfolge ist rein darstellend und leicht zu ändern.
- **Längenmeldungen:** Das Delta gibt Meldungen nur für leeren Titel, Rang und Schätzung vor. Für die Längengrenzen
  analog zu Team: «Der Titel darf höchstens 200 Zeichen lang sein.», «Die Beschreibung darf höchstens 5000 Zeichen
  lang sein.», «Der Owner darf höchstens 100 Zeichen lang sein.»
- **«Nicht eingeplant»:** Die Einplanung (Team × Iteration) kommt mit #13. Bis dahin liefert die API
  `scheduled: false` für jedes Feature; `Feature.scheduled()` ist die Stelle, die #13 ersetzt.
- **404 für unbekanntes PI:** Das Delta nennt dies die «erste 404 der API»; `ProgramIncrementNotFoundExceptionMapper`
  existiert aber schon (US-5, PI-Detail). Wiederverwendet, kein neuer Mapper nötig.
- **Fremdschlüssel ohne Kaskade:** `feature.program_increment_id` verweist streng auf `program_increment`. Löschen
  von PIs gibt es in der API nicht; ob Features mitgehen, entscheidet die Story, die das einführt. Der Test-Step
  «eine leere Liste der PIs» löscht deshalb zuerst die Features.
- **Delta nachgetragen (nur «Neu»):** AK 5 (Backlog-Reihenfolge) mit Szenario, und ein `@Unit`-Szenario für AK 4
  (Feldfehler im Formular), weil AK 4 im Delta kein Szenario hatte.
- **Rang und Schätzung im Formular als Textfeld** (`inputmode="numeric"`) statt `type="number"`: So bleibt «1.5»
  oder «abc» sichtbar und wird am Feld gemeldet, statt vom Browser still geleert.
- **`npm ci` im Worktree** hat der Test-Autor ausgeführt (fehlende `node_modules`); `package.json` unverändert.
- **Offenes Risiko, Kommazahlen über REST:** Jackson nimmt standardmässig `1.5` für ein `Integer`-Feld an und kürzt
  auf `1` (`ACCEPT_FLOAT_AS_INT`). Gilt heute schon für `iterationLengthWeeks` (US-5). Das Formular lehnt Kommazahlen
  ab. Eine Korrektur wäre eine projektweite Jackson-Einstellung, ändert bestehendes Verhalten und gehört in eine
  eigene Story (Spec-Reviewer, nicht umgesetzt).
- **Rolle:** Das Delta nennt keine; die Story sagt «Product Manager», weil in SAFe Product Management die Features
  verantwortet (wie im Issue).
- **Ungültige PI-ID im Pfad** (keine UUID) gilt wie bei den PIs als unbekanntes PI (404 mit Meldung).
