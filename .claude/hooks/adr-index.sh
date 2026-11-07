#!/usr/bin/env bash
# PostToolUse-Hook (Edit|Write|MultiEdit): führt docs/adr/README.md nach, sobald eine ADR geändert wurde.
# Verletzt die ADR den erwarteten Kopf, geht die Meldung über flow_hint an den Agenten; die Änderung bleibt bestehen.
set -u

command -v jq >/dev/null 2>&1 || { echo "adr-index: jq fehlt, Index nicht nachgeführt (brew install jq)" >&2; exit 2; }

root="${COPILOT_PROJECT_DIR:-${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}}"
. "$root/.claude/flow/scripts/lib.sh"
FLOW_PAYLOAD=$(flow_payload)
flow_is_edit "$FLOW_PAYLOAD" || exit 0
adr=false
while IFS= read -r file; do
  case "$file" in "$root"/docs/adr/[0-9][0-9][0-9][0-9]-*.md) adr=true ;; esac
done < <(flow_tool_files "$FLOW_PAYLOAD")
$adr || exit 0

out=$(bash "$root/scripts/adr-index.sh" 2>&1) || flow_hint "$out"
exit 0
