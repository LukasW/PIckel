#!/usr/bin/env bash
# PreToolUse-Hook (Bash): Der Agent setzt das Label spec-unchanged nie und entfernt needs-human-review nie. Beide
# Labels sind menschliche Entscheide über ein Gate (Spec-Drift bzw. Risiko-Pfad); eine Textregel allein hält das
# nicht zuverlässig. Exit 2 = Befehl wird nicht ausgeführt, die Meldung geht an Claude.
# Bewusst grob: Jeder Bash-Befehl, der spec-unchanged nennt, wird abgelehnt (gh pr edit, gh api, gh label …), auch
# Text in Heredocs oder `git commit -m`. Solchen Text mit Write/Edit schreiben bzw. `git commit -F <datei>` nehmen.
set -u
. "$(dirname "$0")/lib.sh"
[ -f "$(flow_root)/.claude/flow.md" ] || exit 0
flow_need_jq guard-labels

cmd=$(jq -r '.tool_input.command // empty')
[ -n "$cmd" ] || exit 0

if printf '%s' "$cmd" | grep -q 'spec-unchanged'; then
  echo "guard-labels: Das Label spec-unchanged übersteuert das Spec-Drift-Gate und setzt nur ein Mensch. Drift beheben (Delta einpflegen, Feature nachziehen, «Delta: keines — <Grund>» im plan.md) oder den Nutzer fragen." >&2
  exit 2
fi
deny_review() {
  echo "guard-labels: needs-human-review entfernt nur ein Mensch nach dem Review des Risiko-Pfads ($1)." >&2
  exit 2
}
method='(-X|--method)[[:space:]=]*'
# Direkt: Label nennen und entfernen (gh pr/issue edit, gh label, REST DELETE auf das Label)
if printf '%s' "$cmd" | grep -q 'needs-human-review' \
  && printf '%s' "$cmd" | grep -Eqi -- "--remove-label|label (delete|edit)|${method}DELETE"; then
  deny_review "entfernen"
fi
# Indirekt, ohne das Label zu nennen: PUT ersetzt alle Labels, DELETE auf …/labels ohne Namen entfernt alle
if printf '%s' "$cmd" | grep -Eq '(issues|pulls)/[0-9]+/labels' \
  && { printf '%s' "$cmd" | grep -Eqi -- "${method}PUT" \
    || { printf '%s' "$cmd" | grep -Eqi -- "${method}DELETE" \
      && printf '%s' "$cmd" | grep -Eq '(issues|pulls)/[0-9]+/labels([[:space:]"'"'"']|$)'; }; }; then
  deny_review "alle Labels ersetzen oder entfernen"
fi
if printf '%s' "$cmd" | grep -Eq 'gh[[:space:]]+api[[:space:]]+graphql' \
  && printf '%s' "$cmd" | grep -Eq 'clearLabelsFromLabelable|removeLabelsFromLabelable'; then
  deny_review "GraphQL"
fi
exit 0
