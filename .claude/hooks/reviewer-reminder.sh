#!/usr/bin/env bash
# PostToolUse-Hook: schlägt nach einem Edit den passenden Reviewer-Agenten vor.
# Exit 0 = nicht blockierend. Jeder Agent wird einmal pro Session genannt.
#
# Input: JSON auf stdin mit tool_input.file_path und session_id.
# Output: JSON mit hookSpecificOutput.additionalContext (stderr erreicht Claude bei Exit 0 nicht).

set -euo pipefail

command -v jq >/dev/null 2>&1 || exit 0

payload=$(cat)
file_path=$(printf '%s' "$payload" | jq -r '.tool_input.file_path // empty' 2>/dev/null || true)
session_id=$(printf '%s' "$payload" | jq -r '.session_id // empty' 2>/dev/null || true)

[[ -z "$file_path" ]] && exit 0
root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
case "$file_path" in
  "$root"/*) ;;
  *) exit 0 ;;
esac

reminders=()

case "$file_path" in
  *src/main/java/ch/grudligstrasse/pikel/*.java)
    reminders+=("hexagonal-reviewer — Java-Backend geändert, prüfe Ports/Adapters.")
    ;;
esac

case "$file_path" in
  *src/main/webapp/src/app/*.ts|*src/main/webapp/src/app/*.html)
    reminders+=("angular-signals-reviewer — Angular-Code geändert, prüfe Signals/OnPush/@if.")
    ;;
esac

case "$file_path" in
  *.feature)
    reminders+=("bdd-cucumber-author — Feature-Datei geändert, prüfe Tag-Routing.")
    ;;
esac

case "$file_path" in
  */src/test/*|*.spec.ts)
    reminders+=("flow:test-reviewer — Tests geändert, prüfe Absicht aus der Spezifikation, Fehlerfinder, Abschwächungen.")
    ;;
esac

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
  context="[reviewer-reminder] ${file_path} erfordert Review vor dem Commit (einmal pro Session genannt):"
  for r in "${reminders[@]}"; do
    context+=$'\n'"  - $r"
  done
  jq -n --arg ctx "$context" \
    '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $ctx}}'
fi

exit 0
