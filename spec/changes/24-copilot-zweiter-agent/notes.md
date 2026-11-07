# Notizen — Issue #24

## Spike (Aufgabe 1)

### Copilot CLI 1.0.107 (Terminal in VS Code 1.145.0), 2026-11-07

- `.github/hooks/*.json` mit `version: 1` wird gelesen; `sessionStart` und `userPromptSubmitted` feuern
- Payloads in camelCase, ohne Event-Namen im Feld (Event nur über die Registrierung bekannt):
  - `sessionStart`: `{"sessionId","timestamp","cwd","source":"new","initialPrompt"}`
  - `userPromptSubmitted`: `{"sessionId","timestamp","cwd","prompt"}`
- Umgebung: `COPILOT_CLI=1`, `COPILOT_HOOK_DIR`, `COPILOT_PROJECT_DIR`. **Gleichzeitig stehen `CLAUDE_PROJECT_DIR`
  und `CLAUDE_CODE_SSE_PORT` in der Umgebung**, geerbt aus dem VS-Code-Terminal mit Claude-Erweiterung. Bestätigt
  den Plan: Format an der Payload erkennen, nie an Umgebungsvariablen
- Prompt `echo hello`: Das Modell (Auto-Auswahl «MAI-Code-1.1-Flash») antwortete direkt mit «hello», **ohne
  Tool-Aufruf**, deshalb kein `preToolUse`. Der Prompt muss ausdrücklich einen Terminalbefehl verlangen
- Mit ausdrücklichem Prompt feuern `preToolUse` und `postToolUse`. Payload `preToolUse` (CLI):
  `{"sessionId","timestamp","cwd","toolCallId","toolName","toolArgs":{...}}`; `postToolUse` zusätzlich
  `"toolResult":{"resultType":"success","textResultForLlm":"..."}`. `toolArgs` ist ein Objekt, kein JSON-String
  - Terminal: `toolName: "bash"`, `toolArgs: {"command":"echo hallo","description":"...","mode":"sync","initial_wait":10}`
  - Lesen: `toolName: "view"`, `toolArgs: {"path":"/abs/README.md","view_range":[1,200]}`
  - Bearbeiten: `toolName: "edit"`, `toolArgs: {"path":"/abs/README.md","old_str":"...","new_str":"..."}`; Pfad absolut
- Ablehnung: Hook mit JSON `{"permissionDecision":"deny","permissionDecisionReason":"spike deny"}` auf stdout,
  «spike deny» auf stderr und Exit 2 → Copilot zeigt «Denied by preToolUse hook: spike deny», der Befehl läuft
  nicht, kein `postToolUse`. Welcher Kanal (JSON oder Exit-Code) die Ablehnung trägt, ist noch offen
- Kanal JSON allein (stdout `permissionDecision: deny`, Exit 0) → abgelehnt, «Denied by preToolUse hook: kanal-json».
  Das JSON auf stdout trägt die Ablehnung, der Grund kommt aus `permissionDecisionReason`
- Kanal Exit 2 allein (Text nur auf stderr) → ebenfalls abgelehnt, aber Copilot zeigt nur «Denied by preToolUse
  hook: hook exited with code 2»; der stderr-Text erreicht weder Nutzer noch Modell. Folge für `flow_deny`: unter
  Copilot JSON mit Grund auf stdout **und** Exit 2 (lehnt auch ab, falls das JSON nicht geparst wird)
- Exit 1 (Hook-Fehler) → **ebenfalls abgelehnt**: «Denied by preToolUse hook from ".github/hooks/spike-log.json"
  (hook errored)». Die CLI schliesst bei einem Fehler im `preToolUse` (fail closed), anders als Claude Code, wo
  Exit 1 nicht blockiert. Folge: Hinweis-Hooks, die unter Copilot als `preToolUse` laufen, dürfen nur mit 0 enden
  (`set -e`, fehlendes `jq` o. Ä. würden sonst jeden Tool-Aufruf sperren)
- `postToolUse` mit Exit 2 und Text auf stderr («kanal-post-exit2») → der Befehl läuft, der stderr-Text erscheint in
  der CLI-Ausgabe vor dem Tool-Ergebnis. Ob er auch das Modell erreicht (wie bei Claude Code), ist noch offen
- Das Modell sieht den stderr-Text aus `postToolUse` **nicht** (auf Nachfrage: «keine Hook-Meldung»). Die
  Hinweis-Hooks erreichen unter Copilot so nur den Menschen, das Modell korrigiert nicht selbst
- `postToolUse` mit JSON auf stdout (Exit 0) → das Modell erhält **nur das Top-Level-Feld `additionalContext`**
  («kanal-ac»); `systemMessage` und `hookSpecificOutput.additionalContext` (Claude-Format) kommen nicht an. Folge:
  Ein `flow_hint` in `lib.sh` schreibt unter Copilot `{"additionalContext": "<meldung>"}` auf stdout, Exit 0
- Anweisungen: Die CLI lädt `CLAUDE.md` als Repository-Anweisungen (es gibt noch kein `AGENTS.md`), die Dateien
  unter `.claude/rules/` nicht. Bestätigt den Plan: `AGENTS.md` als Quelle, Rules zusätzlich über
  `.github/instructions/`

### Copilot CLI liest die Claude-Konfiguration (eigene Messung mit `copilot -p`, 2026-11-07)

Gemessen mit `BASH_ENV`, das jeden von Copilot gestarteten Hook samt stdin mitschreibt.

- `copilot instruction list`: `CLAUDE.md`, `.claude/rules/angular.md`, `.claude/rules/java.md`. Die Rules werden
  gefunden (vermutlich über `paths:` bedarfsweise geladen; auf Nachfrage hatte das Modell sie nicht im Kontext)
- `copilot skill list`: die acht `flow-*`-Skills **einmal**, keine Dubletten (CLI)
- **Die CLI führt die Hooks aus `.claude/settings.json` aus und übersetzt die Payload ins Claude-Format.** Bei jedem
  Terminal-Aufruf laufen `guard-env`, `guard-labels`, `guard-push` mit
  `{"hook_event_name":"PreToolUse","session_id","timestamp","cwd","tool_use_id","tool_name":"Bash","tool_input":{"command","description"}}`.
  `git push --dry-run origin HEAD:main` wurde von `guard-push` **unverändert abgelehnt** (Anzeige: «hook exited
  with code 2», der Grund auf stderr geht verloren)
- Edits (`apply_patch`, GPT-Modell) lösen die `PostToolUse`-Hooks mit Matcher `Edit|Write|MultiEdit` aus:
  `tool_name: "Edit"`, aber **`tool_input` ist der Patch als String** (`*** Update File: /abs/pfad`), kein
  `file_path`. `check-spelling`, `check-test-edit`, `adr-index` finden deshalb keinen Pfad und bleiben still: Ein
  Eszett-Edit blieb ohne Meldung. Folge: `flow_tool_file` muss den Pfad auch aus `*** Update File:` /
  `*** Add File:` und aus `tool_input.path` lesen
- Bei der Ablehnung eines Edits wegen fehlender Freigabe wich das Modell auf `apply_patch` über `bash` aus; die
  Guards sahen den Patch als Befehl. Ein Schreibschutz über `settings.json`-Regeln wäre so umgehbar, der Hook nicht
- **Repository-Hooks nur in vertrauten Ordnern:** In einem nicht vertrauten Ordner überspringt die CLI
  `.github/hooks` und die Repository-Einstellungen («Skipped repository hooks … not trusted»), dann greift kein
  Guard. `--trust-folder` bzw. `trustedFolders` in `~/.copilot/config.json` nötig; gehört in die Anleitung und in
  die Freigabe-Bedingung

### Ausgabe der Claude-Hooks unter Copilot (Aufgabe 3, `copilot -p`, 2026-11-07)

Gemessen mit vorübergehend umgebautem `guard-push.sh` und `check-spelling.sh` (danach zurückgesetzt), ohne geerbtes
`CLAUDE_PROJECT_DIR`:

- Copilot setzt `CLAUDE_PROJECT_DIR` für die Claude-Hooks selbst; die Befehle in `settings.json` laufen auch aus
  einem gewöhnlichen Terminal
- `PreToolUse` mit Grund auf stderr, `{"permissionDecision":"deny","permissionDecisionReason":…}` auf stdout und
  Exit 2 → abgelehnt, Copilot zeigt den Grund aus dem JSON («Denied by preToolUse hook: MESSUNG guard-push: …»)
- `PostToolUse` mit `{"additionalContext":…,"hookSpecificOutput":{…}}` auf stdout, Meldung auf stderr und Exit 2 →
  das Modell erhält das Top-Level-`additionalContext` auch bei Exit 2
- Entscheid daraus: `flow_deny` und `flow_hint` schreiben immer beide Kanäle (stderr für Claude Code, JSON auf
  stdout für Copilot) und enden mit 2. Keine Weiche an der Payload nötig. Claude Code wertet stdout bei Exit 2 nicht
  aus
- Der Pfad aus dem Patch-Text (`*** Update File: /abs`) reicht, damit `check-spelling` die Datei findet

### Umsetzung

- `permissions` aus `.claude/settings.json` unter Copilot nicht gemessen: Der Test mit `gh secret list` wurde vom
  Auto-Modus von Claude Code gesperrt (berührt Secrets). Doku sagt deshalb «nicht gemessen, nur Hooks zählen»
- `.github/hooks/` (Spike-Hook) entfernt; es gibt keinen Copilot-eigenen Hook-Satz (ADR 0007)
- Skills: `compatibility` nennt je Skill Claude Code oder zusätzlich Copilot CLI; `AskUserQuestion`, «Agent-Aufrufe»
  und `/clear` neutral umschrieben
- Ende-zu-Ende mit `copilot -p` nach der Umsetzung (ohne geerbtes `CLAUDE_PROJECT_DIR`): Eszett-Edit per Patch →
  das Modell zitiert «spike-test.md enthält '<Eszett>' – Schweizer Schreibweise verlangt 'ss'»; Push auf `main` → «Denied
  by preToolUse hook: guard-push: Push auf main («HEAD:main»). …». Aufgaben 4 und 5 belegt
- Claude Code akzeptiert das zusätzliche Top-Level-`additionalContext` in `reviewer-reminder.sh` (Hinweis kam in
  dieser Session an)
- Annahmen (klein, im Handoff vorgelegt): `flow_tool_files` endet immer mit 0 und gibt Pfade ohne Dubletten aus;
  `*** Delete File:` wird ignoriert; leerer `command` gilt als kein Befehl; ein Guard ohne Befehl lehnt ab (auch ohne
  `tool_name`); `reviewer-reminder` nimmt bei einem Patch mit mehreren Dateien nur die erste

### Reviewer-Runde

- test-reviewer: Lücken bei Dubletten, `Delete File`, Eszett in der ersten Datei, stillen Zweigen von
  `check-spelling`, `adr-index` im Patch, Top-Level-`additionalContext` von `reviewer-reminder` → Tests ergänzt; die
  drei gemeldeten Mutanten werden jetzt gefunden. Verworfen: Patch-Test für `check-test-edit.sh` (bräuchte ein
  Wegwerf-Repository mit `origin/main`; dieselbe Schleife über `flow_tool_files` ist an `check-spelling` belegt)
- Sicherheits-Review: `reviewer-reminder` nahm bei einem Patch die alphabetisch erste Datei → iteriert jetzt über
  alle; Patch-Parser liest `*** Move to:` und ignoriert CR/Leerzeichen am Zeilenende → behoben, je mit Test.
  `jq -e` endet in jq 1.6 bei leerer Eingabe mit 0: neue Prüfungen verlangen zusätzlich nicht leeres stdout
- Offen (Entscheid Nutzer): Unter Copilot schützt kein Hook das Lesen der `.env` über das Lese-Werkzeug (`view`);
  `guard-env` hängt nur am Matcher `Bash`, die `permissions` (`Read(./.env)`) sind unter Copilot nicht gemessen
- Ausserhalb des Diffs, schon auf `main`: `guard-push` lässt `git -c k=v push …`, `git --no-pager push …`,
  `"git" push …` und einen Push mit Zeilenfortsetzung (`\` + Umbruch) auf `main` durch → Folge-Issue
- Nicht gemessen: ob Copilot-Werkzeuge wie `write_bash` (Eingabe an eine laufende Shell) als `Bash` an die Hooks
  gehen; sonst liefe ein Push daran vorbei

### Lese- und Suchwerkzeuge unter Copilot (Variante A, 2026-11-07)

- Vorübergehender Log-Hook (Matcher `.*` in `settings.json`, danach entfernt): Copilot meldet `view` als
  `{"tool_name":"Read","tool_input":{"path":"/abs/README.md","view_range":[1,-1]}}` und seine Suche als
  `{"tool_name":"Grep","tool_input":{"pattern":"Hexagon","paths":"/abs/docs","output_mode":"content",…}}`
- Folge: `guard-env.sh` hängt zusätzlich am Matcher `Read|Grep|Glob` und prüft dort Pfad- und Musterfelder
- Umgesetzt: `guard-env.sh` unterscheidet Bash (Befehlsprüfung, fail closed) und andere Werkzeuge (letzter Pfadteil
  aus `file_path`, `path`, `paths`, `glob`, bei Glob `pattern`); Matcher `Read|Grep|Glob` in `settings.json`.
  Kleine Annahmen: Glob-Muster mit Platzhalter am Namen (z. B. Stern nach dem Punktnamen) wird nicht abgelehnt (nur
  Pfade, keine Inhalte); die Punkt-Variante `.example` wird wie in der Befehlsprüfung abgelehnt; `.envrc`, `foo.env`
  und Unterpfade eines gleichnamigen Ordners sind erlaubt
- Ende-zu-Ende unter Copilot (Lesen der Geheimnis-Datei mit `copilot -p`) vom Auto-Modus gesperrt (Credential
  Materialization): **Prüfung von Hand offen.** Abgedeckt durch die gemessene Read-Eingabe und `test-guard-env.sh`
- Sicherheits-Review, zweite Runde: Ein `glob` in Grep mit Platzhalter (Stern, Klammern, Fragezeichen) liest den
  Inhalt der Geheimnis-Datei, weil ripgrep mit `--glob` `.gitignore` und versteckte Dateien übersteuert (gemessen vom
  Reviewer). Behoben: Muster werden gegen typische Namen abgeglichen (Klammern eine Ebene tief), ohne Gross- und
  Kleinschreibung; dazu die Felder `notebook_path` und `filePath` und fail closed bei `tool_input`, das kein Objekt
  ist. Die frühere Annahme «Platzhalter am Namen wird nicht abgelehnt» ist damit überholt
- Für das Folge-Issue (schon auf `main`, betrifft auch Claude Code): Der Bash-Zweig von `guard-env` lässt Befehle mit
  Shell-Platzhaltern oder getrennten Anführungszeichen im Dateinamen durch (Stern, Fragezeichen, Klammer, `""`
  mitten im Namen), ebenso einen Symlink auf die Datei; zusammen mit den Push-Umgehungen von `guard-push`

### Interaktive CLI lädt keine Claude-Hooks (Prüfung von Hand, 2026-11-07)

- Prüfung von Hand durch den Nutzer: In der **interaktiven** CLI las Copilot die Geheimnis-Datei über das Terminal,
  ein `git push --dry-run origin HEAD:main` führte zur Rückfrage statt zur Ablehnung. `/env` zeigt «Hooks: No hooks
  loaded», listet aber `CLAUDE.md`, `.claude/rules/`, die Skills und die Agenten unter `.claude/agents/`
- Die Hooks aus `.claude/settings.json` lädt die CLI nur im Prompt-Modus (`-p --trust-folder`, Log: «Loading repo
  hooks in prompt mode»). `.github/hooks/*.json` lädt sie auch interaktiv (Spike am Anfang). Die Messungen weiter
  oben gelten deshalb nur für den Prompt-Modus
- Eigenes Format von `.github/hooks` (Prompt-Modus, vorübergehender Log-Hook, danach entfernt):
  `{"sessionId","timestamp","cwd","toolCallId","toolName","toolArgs"}`; Werkzeuge: `bash` `{command}`, `view`
  `{path}`, `rg` `{pattern, paths}` (`paths` auch relativ, z. B. `docs`), `glob` `{pattern}`, `apply_patch` (Patch
  als String in `toolArgs`), `edit` `{path, old_str, new_str}`. `postToolUse` zusätzlich `toolResult`
- Folge: `.github/hooks/flow.json` registriert dieselben Skripte; `lib.sh` übersetzt das eigene Format ins
  Claude-Format, und jedes Skript filtert selbst nach dem Werkzeug (die Datei kennt keinen Matcher)
- Umgesetzt (N1, N2): `flow_payload` übersetzt das eigene Format, `flow_is_edit` und die Werkzeug-Filter in den
  Guards, Dedup in `flow_hint` über `tool_use_id` je Hook. Prompt-Modus mit beiden Quellen (`copilot -p`): Eszett-
  Meldung genau einmal, Push auf `main` mit Grund abgelehnt, README lesbar
- Annahme: `guard-env` schützt das Lesen, nicht das Schreiben (Edit/`apply_patch` auf die Geheimnis-Datei geht durch,
  wie unter Claude Code mit dem Matcher `Read|Grep|Glob`); im Handoff vorgelegt
- Prüfung von Hand, interaktive CLI (N4): `/env` listet 3 preToolUse- und 4 postToolUse-Hooks aus `flow.json`; Push
  auf `main` und Lesen der Geheimnis-Datei abgelehnt. Eszett-Edit blieb zuerst still: Interaktiv stehen im Patch
  **relative** Pfade (`*** Update File: README.md`, aus `~/.copilot/session-state/*/events.jsonl`), die Hinweis-Hooks
  prüfen nur Pfade unter dem Root. Behoben: `flow_tool_files` löst relative Pfade gegen `cwd` der Eingabe auf (Test)
  Danach interaktiv: Eszett-Edit gemeldet, genau einmal. N4 erledigt
- Sicherheits-Review, dritte Runde: Ein geerbtes `CLAUDE_PROJECT_DIR` eines anderen Projekts schaltete unter Copilot
  alle Guards aus (`flow_root` fand dort keine `flow.md`). Behoben: `COPILOT_PROJECT_DIR` geht vor (auch in
  `adr-index.sh`, `reviewer-reminder.sh`), Test in `test-guard-push.sh`; die Testskripte entfernen
  `COPILOT_PROJECT_DIR`, damit sie auch unter Copilot ihre eigenen Wurzeln nutzen. Geprüft ohne Befund:
  `toolArgs` als String/fehlend, `toolName` null/Zahl, `view`/`rg`/`grep`/`glob` auf die Geheimnis-Datei, Dedup
  (wirkt nur auf Hinweise). `write_bash` gibt es in Copilot 1.0.107 nicht (Werkzeuge `bash`, `read_bash`,
  `stop_bash`, `list_bash`)
- Fürs Folge-Issue (Copilot-Ausbau): ob `web_fetch` `file://` liest (ginge an `guard-env` vorbei) und ob Hooks für
  Werkzeug-Aufrufe von Copilot-Subagenten feuern, ist nicht gemessen
- Vom Nutzer bestätigt (2026-11-07): `guard-env` schützt das Lesen, nicht das Schreiben; ohne `tool_name` gilt bei
  Guards Bash, bei Hinweis-Hooks Edit; ein doppelter Hinweis endet still
