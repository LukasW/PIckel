# ADR 0007: Copilot CLI als zweiter Agent neben Claude Code

**Status:** Angenommen
**Datum:** 2026-11-07

## Kontext

Der Ablauf (`/flow-*`, Hooks, Regeln) läuft mit Claude Code. Das Team soll GitHub Copilot als zusätzliche Option
nutzen können, ohne die Schutzmechanismen aufzugeben: `guard-push` ersetzt die Branch Protection, `guard-env` das
Leseverbot für `.env`, die Hinweis-Hooks melden Eszett, abgeschwächte Tests und den ADR-Index.

Ein Spike mit der Copilot CLI 1.0.107 (Messungen in `spec/changes/24-copilot-zweiter-agent/notes.md`) hat gezeigt:

- Die CLI liest `CLAUDE.md`, `.claude/rules/` und die Skills aus `.agents/skills/` (ADR 0002)
- Im Prompt-Modus (`copilot -p`) führt sie die Hooks aus `.claude/settings.json` aus und übersetzt ihre Eingabe ins
  Format von Claude Code (`tool_name: "Bash"`, `tool_input.command`). Die **interaktive** CLI lädt diese Hooks nicht
  (`/env`: «No hooks loaded»; Prüfung von Hand), sondern nur `.github/hooks/*.json`, mit eigenem Format (`toolName`,
  `toolArgs`, Werkzeuge `bash`, `view`, `rg`, `glob`, `apply_patch`, `edit`)
- Abweichungen: Bei Exit 2 zeigt Copilot nur «hook exited with code 2», den Grund liest es aus
  `permissionDecisionReason` im JSON auf stdout. Edits kommen je nach Modell als Patch-Text in `tool_input` statt mit
  `file_path`. Dem Modell reicht Copilot nur `additionalContext` auf oberster Ebene weiter, nicht stderr
- Im Prompt-Modus lädt die CLI Repository-Hooks nur in einem vertrauten Ordner (`--trust-folder`)
- Der VS-Code-Agent-Modus ist ein anderes Produkt und noch nicht gemessen

## Entscheid

- **Mischbetrieb, sequentiell:** Jeder Flow-Schritt läuft mit Claude Code oder der Copilot CLI; gewechselt wird nur
  zwischen zwei Schritten. Der Zustand liegt im Repository (Issue, Change-Ordner, Branch, Labels), nicht im Werkzeug
- **Eine Konfiguration:** `CLAUDE.md`, `.claude/rules/` und `.agents/skills/` gelten für beide, ohne Kopien. Die
  Hook-Skripte gibt es einmal; registriert sind sie zweimal: in `.claude/settings.json` (Claude Code, Copilot im
  Prompt-Modus) und in `.github/hooks/flow.json` (Copilot interaktiv). `flow_payload` in `lib.sh` übersetzt Copilots
  eigenes Format, jedes Skript filtert selbst nach dem Werkzeug, weil `flow.json` keinen Matcher kennt.
  Läuft ein Hook aus beiden Quellen, meldet `flow_hint` denselben Hinweis nur einmal
- **`guard-env` gilt auch für Lese- und Suchwerkzeuge** (`Read|Grep|Glob`): Unter Copilot ist nicht gemessen, ob es
  die `permissions` aus `settings.json` beachtet, also schützt nur der Hook die Geheimnis-Datei. Abgelehnt wird, wenn
  ein Pfad sie nennt oder ein Muster auf sie passt (ein `glob` in Grep übersteuert `.gitignore`); eine Suche über
  einen ganzen Ordner ohne Muster nicht, dort schützen `.gitignore` und die Ausschlüsse
- **Hooks verstehen beide Formate:** `lib.sh` liest Pfade aus allen bekannten Feldern und aus Patch-Text und meldet
  Ablehnungen und Hinweise so, dass beide Werkzeuge sie anzeigen. Die Prüflogik der Guards gibt es nur einmal
- **Freigabe je Skill und Oberfläche** in `.claude/flow.md` → «Agent wechseln». Claude Code bleibt die Referenz.
  Die Copilot CLI ist freigegeben für Skills ohne Subagenten, nur wenn `/env` die Hooks listet
- Nicht im Umfang: VS-Code-Agent-Modus, Subagenten unter Copilot (`.github/agents/`), Nachtlauf mit Copilot,
  Copilot Cloud Agent

## Konsequenzen

- Eine Änderung an Hooks oder Regeln wirkt in beiden Werkzeugen; die Guard-Tests prüfen beide Eingabeformate
- Ändert eine künftige Copilot-Version, welche Hook-Dateien sie lädt, greift unter Copilot womöglich kein Guard;
  die Prüfung von Hand hat genau das einmal gezeigt. Deshalb steht in der Anleitung eine Kontrolle vor dem ersten
  Schreibzugriff: `/env` listet die Hooks, ein Push auf `main` wird abgelehnt
- Lädt Copilot die Hooks nicht (`/env`: «No hooks loaded»), greift kein Guard; die Kontrolle ist eine Bedingung der
  Freigabe, keine technische Sperre
- Die Skills nennen in `compatibility`, was sie voraussetzen; Claude-spezifische Werkzeugnamen sind neutral
  umschrieben
- VS Code und Subagenten brauchen ein eigenes Issue mit eigenem Spike
- **Bedrohungsmodell der Guards** (#26): `guard-push` und `guard-env` prüfen den Befehl so, wie die Shell ihn sieht
  (ohne Anführungszeichen, Backslashes, Zeilenfortsetzungen; Platzhalter und Symlinks aufgelöst). Sie fangen Versehen
  und naheliegende Umgehungen ab, wie sie ein Agent oder ein eingeschleuster Issue-Text erzeugt, sind aber keine
  Sandbox: Ein Skript, eine Variable, `eval` mit zusammengesetztem Text, ein Git-Alias, ein harter Link oder eine
  Suche über einen ganzen Ordner kommen weiter durch. Die Zusage «`guard-push` ersetzt die Branch Protection» gilt
  in diesem Umfang
