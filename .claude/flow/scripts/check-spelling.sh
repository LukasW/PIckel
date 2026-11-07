#!/usr/bin/env bash
# PostToolUse-Hook (Edit|Write|MultiEdit): meldet «ß» in Repositories mit `spelling: swiss` in der flow.md.
# Meldung über flow_hint (Claude Code und Copilot CLI); die Änderung selbst bleibt bestehen.
set -u
. "$(dirname "$0")/lib.sh"
root=$(flow_root)
flow_require "$root" spelling swiss
flow_need_jq check-spelling

payload=$(cat)
sz=$'\xc3\x9f'
report=""
while IFS= read -r file; do
  [ -n "$file" ] && [ -f "$file" ] || continue
  case "$file" in
    "$root"/.git/*) continue ;;
    "$root"/*) ;;
    *) continue ;;
  esac
  case "$file" in
    *.java|*.kt|*.ts|*.tsx|*.js|*.py|*.go|*.html|*.css|*.md|*.properties|*.sh|*.json|*.xml|*.yaml|*.yml|*.txt) ;;
    *) continue ;;
  esac
  # Eszett (UTF-8 C3 9F); eine Zeile, die die Regel selbst nennt («statt `ß`»), darf es enthalten.
  hits=$(grep -n "$sz" "$file" | grep -v "statt \`$sz\`" | head -5)
  [ -n "$hits" ] || continue
  report+="${file#"$root"/} enthält '$sz' – Schweizer Schreibweise verlangt 'ss':"$'\n'"$hits"$'\n'
done < <(flow_tool_files "$payload")
[ -n "$report" ] || exit 0
flow_hint "${report%$'\n'}"
