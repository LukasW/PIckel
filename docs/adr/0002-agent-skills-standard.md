# ADR 0002: Skills nach dem Agent-Skills-Standard

**Status:** Angenommen
**Datum:** 2026-11-07

## Kontext

Die Skills des Ablaufs (`/flow-*`) lagen unter `.claude/skills/` und waren nur für Claude Code geschrieben.
[Agent Skills](https://agentskills.io/specification) ist ein offenes Format für Skills, das mehrere Agenten lesen;
als gemeinsamer Ort hat sich `.agents/skills/` durchgesetzt. PIkel soll den Standard so weit wie möglich nutzen, der
Ablauf muss aber in Claude Code unverändert funktionieren.

Claude Code liest Projekt-Skills nur aus `.claude/skills/`, folgt dort aber Symlinks auf Skill-Ordner. Unbekannte
Frontmatter-Felder ignoriert es. Umgekehrt lehnt der Referenz-Validator `skills-ref validate` jedes Feld ausserhalb
der Spezifikation ab.

## Entscheid

- Skills liegen unter `.agents/skills/<name>/` (Ordnername = `name`). Unter `.claude/skills/` liegt je Skill nur ein
  Symlink `<name> → ../../.agents/skills/<name>`. Ein Symlink pro Skill statt eines Symlinks auf das ganze
  Verzeichnis, weil Claude Code nur diesen Fall dokumentiert
- Frontmatter mit den Feldern der Spezifikation (`name`, `description`, `compatibility`); `compatibility` nennt, was
  ein Skill voraussetzt (Claude Code, `gh`, Maven Wrapper, Subagenten)
- Bewusste Ausnahme: die Claude-Code-Felder `argument-hint`, `disable-model-invocation` und `model`. Ohne sie würde
  Claude die Skills selbst auslösen (auch den Nachtlauf) und mit dem falschen Modell planen
- `scripts/check-skills.sh` prüft die Regeln der Spezifikation (Name, Länge, Felder, unter 500 Zeilen, relative
  Verweise) und die Symlinks; es läuft in `scripts/gate.sh` und in CI. `skills-ref` selbst nicht, weil es die
  Ausnahme nicht kennt und laut eigener Aussage nur zur Demonstration dient
- Nicht verschoben: Subagenten, Hooks, `settings.json` und Regeln unter `.claude/`. Die Spezifikation deckt nur
  Skills ab; dafür gibt es keinen Standard. Die gemeinsamen Fragmente (`.claude/flow/shared/`) und Skripte
  (`.claude/flow/scripts/`) bleiben, weil mehrere Skills, Agenten, Hooks und CI sie nutzen

## Konsequenzen

- Andere Agenten, die `.agents/skills/` lesen, finden die Skills; die Inhalte setzen aber weiterhin Claude-Code-
  Subagenten und -Hooks voraus (steht in `compatibility`)
- Ein Upload in die Skills API oder nach claude.ai scheitert an den Claude-Code-Feldern; ist kein Ziel
- Ein neuer Skill braucht Ordner und Symlink, sonst wird das Gate rot
- Unter Windows können Symlinks in einem Klon als Textdatei landen (`core.symlinks`); PIkel wird auf macOS und Linux
  entwickelt
- `.agents/` ist wie `.claude/` ein Risiko-Pfad in CI (`needs-human-review`)
