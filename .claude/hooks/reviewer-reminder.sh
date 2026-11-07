#!/usr/bin/env bash
# PostToolUse-Hook: schlägt nach einem Edit den passenden Reviewer-Agenten vor.
# Exit 0 = nicht blockierend. Jeder Agent wird einmal pro Session genannt.
#
# Input: JSON auf stdin mit session_id und den Pfaden (flow_tool_files; bei einem Patch zählt jede Datei).
# Output: JSON mit hookSpecificOutput.additionalContext für Claude Code und additionalContext auf oberster Ebene für
# Copilot (stderr erreicht das Modell bei Exit 0 nicht).

set -euo pipefail

command -v jq >/dev/null 2>&1 || exit 0

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
. "$root/.claude/flow/scripts/lib.sh"
payload=$(cat)
session_id=$(printf '%s' "$payload" | jq -r '.session_id // empty' 2>/dev/null || true)

reminders=()
changed=()
add() {
  local r
  for r in ${reminders[@]+"${reminders[@]}"}; do [[ "$r" == "$1" ]] && return 0; done
  reminders+=("$1")
}

# Ein Patch (Copilot) kann mehrere Dateien enthalten: jede zählt
while IFS= read -r file_path; do
  case "$file_path" in
    "$root"/*) ;;
    *) continue ;;
  esac
  before=${#reminders[@]}
  case "$file_path" in
    *src/main/java/ch/grudligstrasse/pikel/*.java)
      add "hexagonal-reviewer — Java-Backend geändert, prüfe Ports/Adapters." ;;
  esac
  case "$file_path" in
    *src/main/webapp/src/app/*.ts|*src/main/webapp/src/app/*.html)
      add "angular-signals-reviewer — Angular-Code geändert, prüfe Signals/OnPush/@if." ;;
  esac
  case "$file_path" in
    *.feature)
      add "bdd-cucumber-author — Feature-Datei geändert, prüfe Tag-Routing." ;;
  esac
  case "$file_path" in
    */src/test/*|*.spec.ts)
      add "test-reviewer — Tests geändert, prüfe Absicht aus der Spezifikation, Fehlerfinder, Abschwächungen." ;;
  esac
  (( ${#reminders[@]} > before )) && changed+=("$file_path")
done < <(flow_tool_files "$payload")

if [[ -n "$session_id" && ${#reminders[@]} -gt 0 ]]; then
  state="${TMPDIR:-/tmp}/pikel-reviewer-${session_id//[^A-Za-z0-9-]/}"
  fresh=()
  for r in "${reminders[@]}"; do
    agent="${r%% *}"
    if ! grep -qxF "$agent" "$state" 2>/dev/null; then
      fresh+=("$r")
      printf '%s\n' "$agent" >>"$state"
    fi
  done
  reminders=(${fresh[@]+"${fresh[@]}"})
fi

if (( ${#reminders[@]} > 0 )); then
  context="[reviewer-reminder] ${changed[*]} erfordert Review vor dem Commit (einmal pro Session genannt):"
  for r in "${reminders[@]}"; do
    context+=$'\n'"  - $r"
  done
  jq -n --arg ctx "$context" \
    '{additionalContext: $ctx, hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $ctx}}'
fi

exit 0
