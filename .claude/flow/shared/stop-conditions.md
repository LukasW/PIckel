# Fragment: Anhalten

Gilt in jedem Skill und jedem Agenten von flow, dazu die Fälle aus `flow.md` → «Anhalten». Raten ist keine
Option: Wer in einem dieser Fälle weitermacht, baut auf einer Annahme, die niemand geprüft hat.

## Wann anhalten

1. **Drei Fehlversuche:** Derselbe Test oder derselbe Build-Fehler ist nach drei Korrekturen noch rot. Nach drei
   Versuchen trägt der Kontext mehrere falsche Lösungen, und eine vierte Variante baut auf ihnen auf
2. **Spezifikation gegen Bestand:** Story, Plan oder Test verlangen etwas, das bestehendem Verhalten, einer ADR
   oder einer Regel widerspricht, und keine Quelle sagt, was gilt
3. **Unklare Architektur:** Es ist offen, in welche Schicht, welches Modul oder welche Schnittstelle etwas gehört,
   und weder Architektur-Doku noch Regeln klären es
4. **Daten oder Schema ausserhalb von Tests:** eine Migration, die bestehende Daten ändert oder löscht, ein
   Löschen ausserhalb von Test- und Build-Ordnern, ein Eingriff in ein externes System, den der Plan nicht nennt
5. **Ein Test müsste schwächer werden,** damit der Build grün wird, und die Spezifikation verlangt das nicht

## Wie anhalten

Nicht still weiterprobieren und nicht abbrechen, sondern übergeben:

- **Stand:** was versucht wurde, mit dem Ergebnis je Versuch
- **Beobachtung:** die entscheidenden Zeilen aus Fehler, Test oder Spezifikation
- **Hypothese:** woran es vermutlich liegt
- **Frage:** eine konkrete Frage an den Nutzer mit Empfehlung (wie in `/flow-grill-me`)

Ist der Kontext lang geworden, den Stand zusätzlich in `notes.md` im Change-Ordner schreiben (Abschnitt «Stand») und `/clear`
empfehlen: Eine frische Session mit der Antwort des Nutzers löst es meist im ersten Versuch.

**Im Nachtlauf** (`--nachtlauf`) geht die Übergabe als Kommentar ans Issue, und der Lauf endet mit
`NACHTLAUF: ABGEBROCHEN — <grund>` (`.claude/flow/shared/nachtlauf.md`).
