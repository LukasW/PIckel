#!/usr/bin/env bash
# PreToolUse-Hook (Bash): Der Agent setzt das Label spec-unchanged nie und entfernt needs-human-review nie. Beide
# Labels sind menschliche Entscheide über ein Gate (Spec-Drift bzw. Risiko-Pfad); eine Textregel allein hält das
# nicht zuverlässig. Exit 2 = Befehl wird nicht ausgeführt, die Meldung geht an Claude.
# Bewusst grob: Jeder Bash-Befehl, der spec-unchanged nennt, wird abgelehnt (gh pr edit, gh api, gh label …), auch
# Text in Heredocs oder `git commit -m`. Solchen Text mit Write/Edit schreiben bzw. `git commit -F <datei>` nehmen.
set -u
. "$(dirname "$0")/lib.sh"
command -v jq >/dev/null 2>&1 || exit 0
[ -f "$(flow_root)/.claude/flow.md" ] || exit 0

cmd=$(jq -r '.tool_input.command // empty')
[ -n "$cmd" ] || exit 0

if printf '%s' "$cmd" | grep -q 'spec-unchanged'; then
  echo "guard-labels: Das Label spec-unchanged übersteuert das Spec-Drift-Gate und setzt nur ein Mensch. Drift beheben (Delta einpflegen, Feature nachziehen, «Delta: keines — <Grund>» im plan.md) oder den Nutzer fragen." >&2
  exit 2
fi
if printf '%s' "$cmd" | grep -q 'needs-human-review' \
  && printf '%s' "$cmd" | grep -Eq -- '--remove-label|label (delete|edit)|-X[[:space:]]*DELETE|--method[[:space:]]+DELETE'; then
  echo "guard-labels: needs-human-review entfernt nur ein Mensch nach dem Review des Risiko-Pfads." >&2
  exit 2
fi
exit 0
