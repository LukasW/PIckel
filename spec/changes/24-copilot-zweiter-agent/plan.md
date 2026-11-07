# Task — Copilot als zweiter Agent neben Claude Code (Issue #24)

Delta: keines — Ablauf und Werkzeuge der Entwicklung, kein fachliches Verhalten von PIkel.

## Übersicht

Das Team soll GitHub Copilot (VS Code Agent-Modus und Copilot CLI) als zusätzliche Option nutzen können. Gewechselt
wird nur **zwischen** zwei Flow-Schritten. Der Zustand liegt im Repository (Issue, Change-Ordner, Branch, Labels),
nicht im Werkzeug. Claude Code bleibt die Referenz: Ein Schritt, der unter Copilot nicht sauber läuft, bleibt bei
Claude.

Es gibt eine Quelle der Wahrheit. Copilot-Dateien sind Symlinks oder werden generiert, nie von Hand gepflegte Kopien,
sonst laufen die beiden Harnesses auseinander. Copilot bekommt erst Schreibrechte, wenn die Hooks unter Copilot
nachweislich greifen: `guard-push` ersetzt die Branch Protection.

Stand der Recherche vor dem Spike (2026-11-07; Messergebnisse in `notes.md`, sie gehen vor):
- VS Code liest Projekt-Skills aus `.github/skills/`, `.claude/skills/` **und** `.agents/skills/`. Die Skills
  brauchen keine Kopie, erscheinen aber möglicherweise doppelt.
- Copilot-Hooks liegen in `.github/hooks/*.json` (`version: 1`). `preToolUse` kann ablehnen. Laut GitHub
  unterscheiden sich Events, Eingaben und Ausgaben je Oberfläche, die Tool-Namen auch (`run_in_terminal` in VS Code,
  `bash` in der CLI, `Bash` in Claude Code).
- Copilot liest `AGENTS.md`, `.github/instructions/*.instructions.md` (`applyTo`) und `.github/agents/*.agent.md`.

**Schnitt:** Dieses Issue legt das Fundament: Spike, Entscheid, Anweisungen, Skills, Hooks, Gate und Doku.
Copilot wird damit für die Schritte ohne Subagenten freigegeben (`task`, `grill-me`, `plan`, `ship`, `fix-build`).
Die Subagenten (`.github/agents/`) und die Freigabe von `implement` und `fast` kommen in ein Folge-Issue, weil sie
vom Ergebnis des Spikes abhängen (siehe «Offene Fragen»).

## Spike-Ergebnis (CLI) und Folgen für den Plan

Nachgeführt nach Aufgabe 1 (CLI gemessen, VS Code offen), Details in `notes.md`:

- Die Copilot CLI liest `CLAUDE.md`, `.claude/rules/` und die Skills aus `.agents/skills/` (keine Dubletten)
- Sie führt die Hooks aus `.claude/settings.json` selbst aus und **übersetzt die Payload ins Claude-Format**
  (`tool_name: "Bash"`, `tool_input.command`). `guard-push` lehnt einen Push auf `main` schon heute unverändert ab
- Lücken: (a) Der Ablehnungsgrund auf stderr erscheint nicht, nur «hook exited with code 2». (b) Edits mit dem
  GPT-Modell kommen als `tool_name: "Edit"` mit dem Patch als String in `tool_input` (`*** Update File: /abs`),
  ohne `file_path`; die Hinweis-Hooks bleiben still. (c) In einem nicht vertrauten Ordner lädt die CLI keine
  Repository-Hooks, dann greift kein Guard
- Damit **entfallen für die CLI**: `.github/hooks/flow.json`, ein zweites Payload-Format in den Guards, `AGENTS.md`,
  die Instructions-Symlinks und `applyTo`, die Erweiterung von `check-skills.sh`. Sie kommen nur zurück, wenn VS Code
  sie braucht
- **VS Code (Agent-Modus) ist nicht gemessen** und kommt ins Folge-Issue (siehe «Schnitt»). Dieses Issue gibt
  Copilot nur für die **CLI** frei

**Schnitt (nachgeführt):** Dieses Issue: Spike CLI, ADR, Hook-Lücken (a) bis (c) schliessen, Skills neutral, Gate,
Doku. Copilot CLI wird für `task`, `grill-me`, `plan`, `ship`, `fix-build` freigegeben. Folge-Issue: VS Code
messen und freigeben, Subagenten (`.github/agents/`), Freigabe von `implement` und `fast`, Pilot.

## Zu erstellende Dateien

- [x] `docs/adr/0007-copilot-als-zweiter-agent.md` — Entscheid zum Mischbetrieb, ergänzt ADR 0002
- [x] `.claude/flow/scripts/test-guard-env.sh` — Fälle für `guard-env.sh` (bisher ungetestet)
- [x] `.claude/flow/scripts/test-hook-input.sh` — Fälle für `flow_tool_files`, `flow_deny`, `flow_hint`
- [x] `spec/changes/24-copilot-zweiter-agent/notes.md` — Messergebnisse des Spikes

## Zu ändernde Dateien

- [x] `.claude/flow/scripts/lib.sh` — neu `flow_tool_files` (Pfade aus `file_path`, `path`, `tool_response.filePath`
  und den Kopfzeilen `*** Update|Add File:` eines Patches), `flow_deny` (stderr + JSON + Exit 2), `flow_hint`
- [x] `.claude/flow/scripts/guard-push.sh`, `guard-labels.sh`, `guard-env.sh` — Ablehnung über `flow_deny`
- [x] `.claude/flow/scripts/check-spelling.sh`, `check-test-edit.sh`, `.claude/hooks/adr-index.sh`,
  `.claude/hooks/reviewer-reminder.sh` — Pfade über `flow_tool_files` (ein Patch kann mehrere Dateien enthalten),
  Meldung über `flow_hint`
- [x] `.claude/flow/scripts/test-guard-push.sh`, `test-guard-labels.sh` — jeder Fall zusätzlich mit der von Copilot
  übersetzten Payload (mit `timestamp`, ohne `transcript_path`)
- [x] `.agents/skills/*/SKILL.md` — Claude-spezifische Formulierungen neutral, `compatibility` nachgeführt
- [x] `scripts/gate.sh`, `.github/workflows/ci-pr.yml` — die zwei neuen Testskripte neben den Guard-Tests
- [x] `.claude/flow.md` — Abschnitt «Agent wechseln» mit Freigabe-Tabelle je Skill und Oberfläche
- [x] `claude-guide.md` — Abschnitt «Mit der Copilot CLI» (Ordner vertrauen, Modell, Grenzen)
- [x] `docs/arc42.md` — Stakeholder «Entwicklung» und Verweistabelle um Copilot CLI
- [x] `.claude/settings.json` — `guard-env.sh` zusätzlich am PreToolUse-Matcher `Read|Grep|Glob` (Entscheid
  2026-11-07, Variante A)
- [x] Spike-Hook `.github/hooks/` wieder entfernen

## Architektur-Entscheide

- Betrifft nur den Harness, keinen Produktionscode; `ArchitectureTest` ist nicht berührt
- **Neue ADR 0007:** Mischbetrieb mit sequentiellem Wechsel; eine Konfiguration (`.claude/settings.json`,
  `CLAUDE.md`, `.agents/skills/`), die Claude Code und die Copilot CLI beide lesen; Hooks sind die harte Grenze und
  greifen nur in vertrauten Ordnern; Freigabe je Skill und Oberfläche; VS Code, Nachtlauf, Cloud Agent nicht im
  Umfang. ADR 0002 bleibt gültig
- **Kein zweiter Hook-Satz, nur robustere Eingabe und Ausgabe in `lib.sh`:** Die Prüflogik bleibt, wie sie ist.
  `flow_tool_files` liest alle bekannten Pfadfelder; ein Hinweis-Hook ohne erkennbaren Pfad endet still, ein Guard
  mit `tool_name` `Bash` ohne erkennbaren Befehl lehnt ab (fail closed)
- **`flow_deny`:** Grund auf stderr (Claude Code) **und** `{"permissionDecision":"deny","permissionDecisionReason"}`
  auf stdout (Copilot zeigt nur so den Grund), Exit 2. Claude Code ignoriert stdout bei Exit 2
- **`flow_hint` (Hinweis-Hooks, `PostToolUse`):** Meldung auf stderr (Claude Code) **und**
  `{"additionalContext": "<meldung>"}` auf stdout (Copilot reicht nur dieses Top-Level-Feld ans Modell), Exit 2.
  Gemessen: Copilot liest das JSON auch bei Exit 2; keine Weiche nach Werkzeug (`notes.md`, Aufgabe 3)
- **Schnittstelle in `lib.sh`:** Die Hooks lesen stdin einmal (`payload=$(cat)`) und reichen es weiter:
  `flow_tool_command "$payload"` (Befehl auf stdout, Exit 1 ohne Befehl), `flow_tool_files "$payload"` (absolute
  Pfade, einer pro Zeile), `flow_deny "<grund>"`, `flow_hint "<meldung>"`. `reviewer-reminder.sh` (Exit 0, JSON mit
  `hookSpecificOutput`) bekommt zusätzlich das Top-Level-`additionalContext`
- **`guard-env` auch für Lese- und Suchwerkzeuge (Entscheid 2026-11-07, Variante A):** Unter Copilot schützt sonst
  kein Hook das Lesen der Geheimnis-Datei über das Lese-Werkzeug; ob Copilot die `permissions` beachtet, ist nicht
  gemessen. Gemessen (`notes.md`): Copilot meldet sein Lese-Werkzeug als `tool_name: "Read"` mit `tool_input.path`,
  die Suche als `"Grep"` mit `tool_input.paths`; Claude Code liefert `Read` mit `file_path`, `Grep` mit `path`,
  `Glob` mit `pattern` und `path`. Regel: Bei `tool_name` `Bash` (oder ohne `tool_name`) gilt die bisherige
  Befehlsprüfung samt fail closed. Bei jedem anderen Werkzeug wird abgelehnt, wenn ein Pfad- oder Musterfeld
  (`file_path`, `path`, `paths` als String oder Liste, `glob`, bei `Glob` auch `pattern`) als letzten Pfadteil die
  Geheimnis-Datei oder eine ihrer Varianten mit Punkt-Endung nennt (gleiche Namen wie in der Befehlsprüfung);
  `env.example` und andere Dateien sind erlaubt. Bei `Grep` ist `pattern` der Suchtext, kein Pfad. Ein Werkzeug ohne
  solches Feld wird durchgelassen (nichts zu lesen). Eine Suche über einen ganzen Ordner wird nicht abgelehnt; das
  schützt weiter `.gitignore` bzw. die Ausschlüsse (bekannte Grenze, in der Doku)
- **Copilot-Freigabe nur mit vertrautem Ordner:** steht in der Anleitung und in der Freigabe-Tabelle

## Aufgaben

- [x] 1a. Spike CLI — Payloads für Terminal, Edit, Ablehnung, Kanäle, gelesene Dateien in `notes.md`
- [x] 1b. Spike-Hook entfernen — erledigt, wenn `.github/hooks/` nicht mehr existiert
- [x] 2. ADR 0007 — erledigt, wenn `scripts/adr-index.sh` sie listet und sie die Spike-Ergebnisse nennt
- [x] 3a. Messen mit `copilot -p`: Grund aus dem JSON erscheint bei Exit 2, `additionalContext` erreicht das Modell
  auch bei Exit 2 (`notes.md`)
- [x] 3b. `flow_tool_command`, `flow_tool_files`, `flow_deny`, `flow_hint` bauen — erledigt, wenn
  `test-hook-input.sh` grün ist
- [x] 4. Guards auf `flow_deny`, Guard-Tests um die übersetzte Payload, `test-guard-env.sh` neu — erledigt, wenn alle
  Guard-Tests für beide Payloads grün sind und ein Guard einen `Bash`-Aufruf ohne Befehl ablehnt
- [x] 5. Hinweis-Hooks auf `flow_tool_files` und `flow_hint` — erledigt, wenn ein Eszett-Edit über `copilot -p` die
  Rechtschreib-Meldung beim Modell auslöst (Ergebnis in `notes.md`)
- [x] 6. Skills neutral, `compatibility` nachführen — erledigt, wenn `check-skills.sh` grün ist und `grep` in
  `.agents/skills/` keine Claude-Tool-Namen (`AskUserQuestion`, «Agent-Aufruf») ohne neutrale Umschreibung findet
- [x] 7. `gate.sh`, `ci-pr.yml` — erledigt, wenn `scripts/gate.sh --fast` grün ist
- [x] 8. Doku: `flow.md` «Agent wechseln», `claude-guide.md`, `arc42.md` — erledigt, wenn `check-spelling.sh` ohne
  Meldung ist und die Tabelle jeden der acht Skills je Oberfläche (Claude Code, Copilot CLI, VS Code) zuordnet

## Test-Strategie

- [x] Guard-Tests (Shell, Gate und CI): Jeder Fall in `test-guard-push.sh` und `test-guard-labels.sh` liefert für
  die Claude- und die übersetzte Copilot-Payload denselben Exit-Code. `guard-env`: `cat .env` abgelehnt,
  `git ls-files .env` und `--exclude='.env*'` erlaubt. `Bash` ohne Befehl → abgelehnt
- [x] `test-hook-input.sh`: `flow_tool_files` liefert den Pfad aus `file_path`, `path`, `tool_response.filePath` und
  aus einem Patch mit zwei Dateien (`Update` und `Add`), nichts bei einer Eingabe ohne Pfad; `flow_tool_command`
  liefert `tool_input.command` und endet mit 1 ohne Befehl; `flow_deny` schreibt den Grund auf stderr, auf stdout
  gültiges JSON mit `permissionDecision: "deny"` und dem Grund in `permissionDecisionReason` und endet mit 2;
  `flow_hint` schreibt die Meldung auf stderr, auf stdout gültiges JSON mit der Meldung in `additionalContext` und
  endet mit 2. Sonderzeichen (Anführungszeichen, Zeilenumbruch, Umlaute) bleiben im JSON gültig
- [x] Hinweis-Hooks mit Patch-Eingabe: `check-spelling.sh` meldet Eszett in einer per Patch bearbeiteten Datei
  (stderr, JSON, Exit 2) und bleibt bei einer Datei ohne Eszett still (Exit 0, keine Ausgabe)
- [x] `test-guard-env.sh` für Lese- und Suchwerkzeuge, je Claude- und Copilot-Form: `Read` der Geheimnis-Datei
  (`file_path` bzw. `path`, auch als relativer Pfad und mit Endung `.local`) → abgelehnt mit JSON; `Read` von
  `README.md` und `env.example` → erlaubt; `Grep` mit `paths`/`path` auf die Datei → abgelehnt, auf `docs` → erlaubt;
  `Grep` mit dem Suchtext `.env` in `pattern` über `docs` → erlaubt; `Glob` mit Muster auf die Datei → abgelehnt;
  `Read` ohne Pfad → erlaubt
- [x] Bestehende Tests: keine Abschwächung; die bisherigen Claude-Fälle bleiben unverändert
- [x] Prüfung mit `copilot -p` (von Claude Code aus, eng freigegebene Tools, `--trust-folder`): Push auf `main` mit
  sichtbarem Grund abgelehnt, Eszett-Edit gemeldet; Ergebnis in `notes.md`

## Entschieden (2026-11-07)


- **Schnitt:** Subagenten (`.github/agents/*.agent.md`, Generator, Gate-Prüfung), Freigabe von `implement` und
  `fast` sowie der Pilot mit einer gemischten Story kommen in ein Folge-Issue. Sie hängen vom Spike ab, und dieses
  Issue hat ohne sie schon acht Aufgaben
- **Label:** `maintenance` auf GitHub angelegt, #22 und #24 gelabelt

- **Nach dem Spike (2026-11-07):** Plan auf die gemessene CLI nachgeführt; VS Code ins Folge-Issue

## Offene Fragen

- Keine, sofern der nachgeführte Schnitt bestätigt ist
