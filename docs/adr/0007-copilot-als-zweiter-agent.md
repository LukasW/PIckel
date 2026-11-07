# ADR 0007: Copilot CLI als zweiter Agent neben Claude Code

**Status:** Angenommen
**Datum:** 2026-11-07

## Kontext

Der Ablauf (`/flow-*`, Hooks, Regeln) läuft mit Claude Code. Das Team soll GitHub Copilot als zusätzliche Option
nutzen können, ohne die Schutzmechanismen aufzugeben: `guard-push` ersetzt die Branch Protection, `guard-env` das
Leseverbot für `.env`, die Hinweis-Hooks melden Eszett, abgeschwächte Tests und den ADR-Index.

Ein Spike mit der Copilot CLI 1.0.107 (Messungen in `spec/changes/24-copilot-zweiter-agent/notes.md`) hat gezeigt:

- Die CLI liest `CLAUDE.md`, `.claude/rules/` und die Skills aus `.agents/skills/` (ADR 0002)
- Sie führt die Hooks aus `.claude/settings.json` selbst aus und übersetzt ihre Eingabe ins Format von Claude Code
  (`tool_name: "Bash"`, `tool_input.command`). `guard-push` lehnt einen Push auf `main` unverändert ab
- Abweichungen: Bei Exit 2 zeigt Copilot nur «hook exited with code 2», den Grund liest es aus
  `permissionDecisionReason` im JSON auf stdout. Edits kommen je nach Modell als Patch-Text in `tool_input` statt mit
  `file_path`. Dem Modell reicht Copilot nur `additionalContext` auf oberster Ebene weiter, nicht stderr
- In einem Ordner, dem die CLI nicht vertraut, lädt sie keine Repository-Hooks; dann greift kein Guard
- Der VS-Code-Agent-Modus ist ein anderes Produkt und noch nicht gemessen

## Entscheid

- **Mischbetrieb, sequentiell:** Jeder Flow-Schritt läuft mit Claude Code oder der Copilot CLI; gewechselt wird nur
  zwischen zwei Schritten. Der Zustand liegt im Repository (Issue, Change-Ordner, Branch, Labels), nicht im Werkzeug
- **Eine Konfiguration:** `.claude/settings.json`, `CLAUDE.md`, `.claude/rules/` und `.agents/skills/` gelten für
  beide. Kein zweiter Satz Hooks unter `.github/hooks/`, keine Kopien, solange Copilot die Claude-Dateien liest
- **`guard-env` gilt auch für Lese- und Suchwerkzeuge** (`Read|Grep|Glob`): Unter Copilot ist nicht gemessen, ob es
  die `permissions` aus `settings.json` beachtet, also schützt nur der Hook die Geheimnis-Datei. Abgelehnt wird, wenn
  ein Pfad sie nennt oder ein Muster auf sie passt (ein `glob` in Grep übersteuert `.gitignore`); eine Suche über
  einen ganzen Ordner ohne Muster nicht, dort schützen `.gitignore` und die Ausschlüsse
- **Hooks verstehen beide Formate:** `lib.sh` liest Pfade aus allen bekannten Feldern und aus Patch-Text und meldet
  Ablehnungen und Hinweise so, dass beide Werkzeuge sie anzeigen. Die Prüflogik der Guards gibt es nur einmal
- **Freigabe je Skill und Oberfläche** in `.claude/flow.md` → «Agent wechseln». Claude Code bleibt die Referenz.
  Die Copilot CLI ist freigegeben für Skills ohne Subagenten und nur in einem vertrauten Ordner
- Nicht im Umfang: VS-Code-Agent-Modus, Subagenten unter Copilot (`.github/agents/`), Nachtlauf mit Copilot,
  Copilot Cloud Agent

## Konsequenzen

- Eine Änderung an Hooks oder Regeln wirkt in beiden Werkzeugen; die Guard-Tests prüfen beide Eingabeformate
- Liest eine künftige Copilot-Version die Claude-Dateien nicht mehr, greift unter Copilot kein Guard. Deshalb steht
  in der Anleitung eine Kontrolle (abgelehnter Push auf `main`) vor dem ersten Schreibzugriff
- Ein Ordner, dem Copilot nicht vertraut, hebelt alle Hooks aus; das ist eine Bedingung der Freigabe, keine
  technische Sperre
- Die Skills nennen in `compatibility`, was sie voraussetzen; Claude-spezifische Werkzeugnamen sind neutral
  umschrieben
- VS Code und Subagenten brauchen ein eigenes Issue mit eigenem Spike
