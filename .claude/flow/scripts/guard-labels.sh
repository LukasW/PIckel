#!/usr/bin/env bash
# PreToolUse-Hook (Bash): Der Agent setzt das Label spec-unchanged nie und entfernt needs-human-review nie. Beide
# Labels sind menschliche Entscheide über ein Gate (Spec-Drift bzw. Risiko-Pfad); eine Textregel allein hält das
# nicht zuverlässig. Ablehnung über flow_deny: Befehl wird nicht ausgeführt, die Meldung geht an den Agenten.
# Bewusst grob: Jeder Bash-Befehl, der spec-unchanged nennt, wird abgelehnt (gh pr edit, gh api, gh label …), auch
# Text in Heredocs oder `git commit -m`. Solchen Text mit Write/Edit schreiben bzw. `git commit -F <datei>` nehmen.
set -u
. "$(dirname "$0")/lib.sh"
[ -f "$(flow_root)/.claude/flow.md" ] || exit 0
flow_need_jq guard-labels

payload=$(cat)
cmd=$(flow_tool_command "$payload") \
  || flow_deny "guard-labels: Kein Befehl in der Eingabe erkannt; ohne Befehl lässt der Wächter nichts durch (ADR 0007)."

if printf '%s' "$cmd" | grep -q 'spec-unchanged'; then
  flow_deny "guard-labels: Das Label spec-unchanged übersteuert das Spec-Drift-Gate und setzt nur ein Mensch. Drift beheben (Delta einpflegen, Feature nachziehen, «Delta: keines — <Grund>» im plan.md) oder den Nutzer fragen."
fi
deny_review() {
  flow_deny "guard-labels: needs-human-review entfernt nur ein Mensch nach dem Review des Risiko-Pfads ($1)."
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
# Commit-Status mit den Namen der Pflicht-Checks meldet nur scripts/ci-local.sh, nachdem es selbst geprüft hat;
# ein direkt gemeldeter Status wäre ein grüner Check ohne Prüfung (ADR 0004)
if printf '%s' "$cmd" | grep -Eq 'statuses/' \
  && printf '%s' "$cmd" | grep -Eqi -- "${method}(POST|PUT|PATCH)|(^|[[:space:]])(-f|-F|--field|--raw-field|--input)([[:space:]=]|$)"; then
  flow_deny "guard-labels: Einen Commit-Status meldet nur scripts/ci-local.sh, nach den Prüfungen. Das Skript starten statt den Status selbst zu setzen."
fi
exit 0
