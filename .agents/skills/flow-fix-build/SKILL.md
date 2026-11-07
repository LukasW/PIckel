---
name: flow-fix-build
description: Bringt den Build iterativ auf grün — Gate fail-fast starten, ersten Fehler im Code beheben (nie den Test abschwächen), neu starten. Einsetzen, wenn scripts/gate.sh oder CI rot ist.
compatibility: Claude Code oder Copilot CLI (in einem vertrauten Ordner, ADR 0007) im Repository PIkel. Braucht git, gh (GitHub) und den Maven Wrapper; Projektangaben in .claude/flow.md.
argument-hint: "[modul oder testklasse]"
disable-model-invocation: true
model: sonnet
---

Ziel: Der Befehl «Abschluss ohne Fail-Fast» aus `flow.md` → «Build» läuft komplett grün, mit allen Prüfungen
(Formatierer, statische Analyse, Architekturtests, Integrationstests).

Ablauf-Verzeichnis: `.claude/flow`. Projektangaben: `.claude/flow/shared/project.md`.

Optionaler Startpunkt: **$ARGUMENTS** (Modul oder Testklasse). Leer: mit dem ganzen Gate beginnen.

## Ablauf

Wiederhole diese Schleife, bis der komplette Build grün durchläuft:

1. **Starten:** den Abschluss-Befehl mit Fail-Fast (Maven:
   `-Dsurefire.skipAfterFailureCount=1 -Dfailsafe.skipAfterFailureCount=1`, npm/Jest: `--bail`, pytest: `-x`) im
   Hintergrund. Der Build bricht beim ersten Fehler ab.

2. **Beobachten:** auf die Benachrichtigung warten oder die Testberichte des Werkzeugs ansehen.

3. **Report:** Sobald der Build abbricht oder fertig ist:
   - **Welches Modul und welche Phase** (Kompilieren, Formatieren, statische Analyse, Unit, Integration)
   - **Welcher Test** (Klasse + Methode)
   - **Art des Fehlers:** Compiler-Warnung, Formatierung, Analyse-Regel, Architekturregel, Assertion, Container
     startet nicht
   - **Fehlermeldung:** die relevanten Zeilen

4. **Flicken:** den Fehler analysieren und **ausschliesslich** diese Stelle flicken.
   - Formatierung: Befehl «Formatieren», dann den Diff ansehen
   - Compiler-Warnung: Ursache beheben, keine Unterdrückung ohne Begründung
   - Architekturregel: Die Regel hat recht. Den Code verschieben, nicht die Regel lockern; eine neue Ausnahme
     braucht eine ADR
   - Statische Analyse: Code vereinfachen; eine Regel nur mit Begründung am Ort unterdrücken
   - Container: Läuft Podman oder Docker, und antwortet die API? Liegen alte Testcontainer herum (`podman ps -a`,
     `docker ps -a`)? Ein Fehler beim Start eines Containers ist meist die Laufzeit, nicht der Code. Dann erst den
     Test ansehen
   - **Nicht** andere grüne Tests mitändern, nur den einen, der gerade scheitert
   - Ein scheiternder Test heisst zuerst: Der Code ist falsch. Einen Test nie löschen, abschalten oder seine
     Assertion abschwächen; `check-test-edit.sh` meldet das

5. **Im Modul iterieren:** Befehl «Im Loop» für das Modul des Fehlers, mit Fail-Fast; einzelne Testklasse, wenn
   derselbe Test mit demselben Fehler scheitert. Erst wenn das Modul grün ist: zurück zu Schritt 1.

6. **Anhalten:** Derselbe Fehler nach drei Korrekturen, oder der Fehler liesse sich nur durch einen schwächeren
   Test beheben: nicht weiterprobieren, sondern übergeben nach `.claude/flow/shared/stop-conditions.md`.
   Ein Fehler, der ohne Änderung mal rot und mal grün ist, ist ein eigener Befund: Ursache suchen, nicht neu starten,
   bis er grün ist.

## Commit & Abschluss

Nur dann einen Commit erstellen, wenn der komplette Build ohne Fail-Fast grün durchläuft.
Commit-Message: `fix(build): <kurze Beschreibung> (#<issue>)`. Nicht pushen ohne explizite Freigabe.
