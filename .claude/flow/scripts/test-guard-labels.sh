#!/usr/bin/env bash
# Prüft guard-labels.sh gegen bekannte Umgehungen (Projekt-Review 2026-11-07): je Fall erwarteter Exit-Code und
# Befehl. Läuft in scripts/gate.sh und CI, damit eine Änderung am Guard keine Lücke wieder öffnet.
set -u
# Laufen die Tests unter Copilot, ginge dessen Projektordner den gesetzten Wurzeln vor (flow_root)
unset COPILOT_PROJECT_DIR
root="$(cd "$(dirname "$0")/../../.." && pwd)"
G="$root/.claude/flow/scripts/guard-labels.sh"
export CLAUDE_PROJECT_DIR="$root"
fails=0
# Copilot-Payload, wie die Copilot CLI sie übersetzt (notes.md: mit timestamp und tool_use_id, ohne
# transcript_path). Erwartet denselben Exit wie Claude; bei Exit 2 zusätzlich JSON mit permissionDecision deny und dem
# Grund von stderr auf stdout (nur so zeigt Copilot den Grund), bei Exit 0 keine Ablehnung auf stdout.
copilot_payload() { jq -cn --arg c "$1" --arg cwd "$2" '{hook_event_name:"PreToolUse",session_id:"c1",timestamp:1793955600000,cwd:$cwd,tool_use_id:"call_1",tool_name:"Bash",tool_input:{command:$c,description:"Befehl"}}'; }
# Eigenes Format der Copilot CLI (.github/hooks/flow.json, notes.md «Interaktive CLI lädt keine Claude-Hooks»):
# toolName, toolArgs, sessionId, toolCallId, ohne tool_name. Erwartet denselben Entscheid wie im Claude-Format.
own_payload() { jq -cn --arg c "$1" --arg cwd "$2" '{sessionId:"c1",timestamp:1793955600000,cwd:$cwd,toolCallId:"call_1",toolName:"bash",toolArgs:{command:$c,description:"Befehl"}}'; }
own_tool() { jq -cn --arg n "$1" --argjson a "$2" --arg cwd "$3" '{sessionId:"c1",timestamp:1793955600000,cwd:$cwd,toolCallId:"call_1",toolName:$n,toolArgs:$a}'; }
check() {
  local want="$1" label="$2" payload="$3" dir="${4:-$root}" o e got out err
  o=$(mktemp) e=$(mktemp)
  printf '%s' "$payload" | (cd "$dir" && CLAUDE_PROJECT_DIR="$dir" bash "$G") >"$o" 2>"$e"
  got=$?
  out=$(cat "$o") err=$(cat "$e")
  rm -f "$o" "$e"
  if [ "$got" != "$want" ]; then
    echo "FAIL $got (erwartet $want) $label"; fails=$((fails+1)); return
  fi
  if [ "$want" = 2 ] && ! { [ -n "$out" ] && printf '%s' "$out" | jq -e --arg e "$err" \
    '.permissionDecisionReason as $r | .permissionDecision == "deny" and ($r | type == "string" and length > 0) and ($e | contains($r))' >/dev/null 2>&1; }; then
    echo "FAIL $got $label: stdout ohne JSON mit permissionDecision deny und dem Grund von stderr: «$out»"; fails=$((fails+1)); return
  fi
  if [ "$want" = 0 ] && [ -n "$out" ] && printf '%s' "$out" | jq -e '.permissionDecision == "deny"' >/dev/null 2>&1; then
    echo "FAIL $got $label: Exit 0, aber stdout lehnt ab: «$out»"; fails=$((fails+1)); return
  fi
  echo "ok   $got $label"
}
t() {
  local want="$1" cmd="$2" got
  printf '{"tool_input":{"command":%s}}' "$(jq -Rn --arg c "$cmd" '$c')" | bash "$G" >/dev/null 2>&1
  got=$?
  if [ "$got" = "$want" ]; then echo "ok   $got $cmd"; else echo "FAIL $got (erwartet $want) $cmd"; fails=$((fails+1)); fi
  check "$want" "copilot: $cmd" "$(copilot_payload "$cmd" "$root")"
  check "$want" "copilot eigen: $cmd" "$(own_payload "$cmd" "$root")"
}
SU="spec-$(printf unchanged)"
t 2 'gh pr edit 5 --remove-label needs-human-review'
t 2 'gh issue edit 5 --remove-label bug,needs-human-review'
t 2 'gh api repos/o/r/issues/5/labels/needs-human-review --method=DELETE'
t 2 'gh api repos/o/r/issues/5/labels/needs-human-review -XDELETE'
t 2 'gh api -X DELETE repos/o/r/issues/5/labels/needs-human-review'
t 2 "gh api -X PUT repos/o/r/issues/5/labels -f 'labels[]=bug'"
t 2 'gh api --method put repos/o/r/issues/5/labels -f labels[]=bug'
t 2 'gh api -X DELETE repos/o/r/issues/5/labels'
t 2 'gh api graphql -f query="mutation { clearLabelsFromLabelable(input:{labelableId:\"x\"}) { clientMutationId } }"'
t 2 'gh api graphql -f query="mutation { removeLabelsFromLabelable(input:{labelableId:\"x\",labelIds:[\"y\"]}) { clientMutationId } }"'
t 2 'gh label delete needs-human-review --yes'
t 2 "gh pr edit 5 --add-label $SU"
t 0 'gh api -X DELETE repos/o/r/issues/5/labels/bug'
t 0 'gh api repos/o/r/issues/5/labels'
t 0 'gh api -X POST repos/o/r/issues/5/labels -f labels[]=bug'
t 0 'gh pr edit 5 --add-label needs-human-review'
t 2 'gh api -X POST repos/o/r/statuses/abc -f state=success -f context="Unit Tests"'
t 2 'gh api repos/o/r/statuses/abc -f state=success'
t 2 'gh api repos/o/r/statuses/abc --raw-field state=success'
t 2 'gh api --method=post repos/o/r/statuses/abc --input s.json'
t 0 'gh api repos/o/r/commits/abc/statuses'
t 0 'scripts/ci-local.sh'
t 0 'git status'
# Bash ohne erkennbaren Befehl: fail closed (Plan #24, Architektur-Entscheide), Claude- und Copilot-Payload
check 2 'claude: Bash ohne Befehl' '{"tool_name":"Bash","tool_input":{"description":"Befehl"}}'
check 2 'copilot: Bash ohne Befehl' \
  "$(jq -cn --arg cwd "$root" '{hook_event_name:"PreToolUse",session_id:"c1",timestamp:1793955600000,cwd:$cwd,tool_use_id:"call_1",tool_name:"Bash",tool_input:{description:"Befehl"}}')"
check 2 'copilot eigen: bash ohne Befehl' "$(own_tool bash '{"description":"Befehl"}' "$root")"
# Nur Bash wird geprüft (Plan #24, «Nachtrag»): Lesen und Bearbeiten gehen durch, auch wenn der Text das Label nennt
check 0 'copilot eigen: view' "$(own_tool view "$(jq -cn --arg p "$root/README.md" '{path:$p,view_range:[1,200]}')" "$root")"
check 0 'copilot eigen: edit mit Label im Text' \
  "$(own_tool edit "$(jq -cn --arg p "$root/README.md" --arg l "$SU" '{path:$p,old_str:"x",new_str:("gh pr edit 5 --add-label " + $l)}')" "$root")"
check 0 'copilot eigen: apply_patch mit Label im Text' \
  "$(own_tool apply_patch "$(jq -cn --arg p "$root/README.md" '"*** Begin Patch\n*** Update File: " + $p + "\n@@\n+gh pr edit 5 --remove-label needs-human-review\n*** End Patch\n"')" "$root")"
check 0 'claude: Read geht durch' "$(jq -cn --arg p "$root/README.md" '{tool_name:"Read",tool_input:{file_path:$p}}')"
check 2 'Eingabe, die kein JSON ist' 'gh pr edit 5 --remove-label needs-human-review'

# Ohne jq: fail-closed. Eigener PATH mit den nötigen Werkzeugen, aber ohne jq (macOS hat jq unter /usr/bin)
nojq=$(mktemp -d)
for b in dirname git grep sed awk cat head tr wc; do ln -s "$(command -v "$b")" "$nojq/$b"; done
printf '{}' | env PATH="$nojq" /bin/bash "$G" >/dev/null 2>&1
got=$?
rm -rf "$nojq"
if [ "$got" = 2 ]; then echo "ok   2 ohne jq"; else echo "FAIL $got (erwartet 2) ohne jq"; fails=$((fails+1)); fi
[ "$fails" = 0 ] || { echo "[ERROR] guard-labels: $fails Fälle falsch"; exit 1; }
echo "guard-labels: alle Fälle ok"
