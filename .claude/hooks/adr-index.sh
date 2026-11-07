#!/usr/bin/env bash
# PostToolUse-Hook (Edit|Write|MultiEdit): führt docs/adr/README.md nach, sobald eine ADR geändert wurde.
# Verletzt die ADR den erwarteten Kopf, gibt Exit 2 die Meldung an Claude zurück; die Änderung selbst bleibt bestehen.
set -u

command -v jq >/dev/null 2>&1 || { echo "adr-index: jq fehlt, Index nicht nachgeführt (brew install jq)" >&2; exit 2; }

root="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
file=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty')
case "$file" in
  "$root"/docs/adr/[0-9][0-9][0-9][0-9]-*.md) ;;
  *) exit 0 ;;
esac

out=$(bash "$root/scripts/adr-index.sh" 2>&1) || { printf '%s\n' "$out" >&2; exit 2; }
exit 0
