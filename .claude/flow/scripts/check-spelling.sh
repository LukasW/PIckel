#!/usr/bin/env bash
# PostToolUse-Hook (Edit|Write|MultiEdit): meldet «ß» in Repositories mit `spelling: swiss` in der flow.md.
# Exit 2 gibt die Meldung an Claude zurück; die Änderung selbst bleibt bestehen.
set -u
. "$(dirname "$0")/lib.sh"
command -v jq >/dev/null 2>&1 || exit 0

root=$(flow_root)
flow_require "$root" spelling swiss

file=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty')
[ -n "$file" ] && [ -f "$file" ] || exit 0
case "$file" in
  "$root"/.git/*) exit 0 ;;
  "$root"/*) ;;
  *) exit 0 ;;
esac

# Eszett (UTF-8 C3 9F); eine Zeile, die die Regel selbst nennt («statt `ß`»), darf es enthalten.
sz=$'\xc3\x9f'
case "$file" in
  *.java|*.kt|*.ts|*.tsx|*.js|*.py|*.go|*.html|*.css|*.md|*.properties|*.sh|*.json|*.xml|*.yaml|*.yml|*.txt) ;;
  *) exit 0 ;;
esac
hits=$(grep -n "$sz" "$file" | grep -v "statt \`$sz\`" | head -5)
[ -n "$hits" ] || exit 0
printf '%s\n%s\n' "${file#"$root"/} enthält '$sz' – Schweizer Schreibweise verlangt 'ss':" "$hits" >&2
exit 2
