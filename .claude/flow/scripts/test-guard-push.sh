#!/usr/bin/env bash
# Prüft guard-push.sh gegen bekannte Umgehungen (Projekt-Review 2026-11-07): je Fall erwarteter Exit-Code und Befehl.
# Läuft in scripts/gate.sh und CI. Läuft auf einem Feature-Branch; der Fall «ohne Refspec auf main» braucht main als
# aktuellen Zweig und wird über ein temporäres Repository geprüft.
set -u
# Laufen die Tests unter Copilot, ginge dessen Projektordner den gesetzten Wurzeln vor (flow_root)
unset COPILOT_PROJECT_DIR
root="$(cd "$(dirname "$0")/../../.." && pwd)"
G="$root/.claude/flow/scripts/guard-push.sh"
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
  local want="$1" cmd="$2" dir="${3:-$root}" got
  printf '{"tool_input":{"command":%s}}' "$(jq -Rn --arg c "$cmd" '$c')" \
    | (cd "$dir" && CLAUDE_PROJECT_DIR="$dir" bash "$G") >/dev/null 2>&1
  got=$?
  if [ "$got" = "$want" ]; then echo "ok   $got $cmd"; else echo "FAIL $got (erwartet $want) $cmd"; fails=$((fails+1)); fi
  check "$want" "copilot: $cmd" "$(copilot_payload "$cmd" "$dir")" "$dir"
  check "$want" "copilot eigen: $cmd" "$(own_payload "$cmd" "$dir")" "$dir"
}

t 2 'git push origin main'
t 2 'git push origin HEAD:main'
t 2 'git push origin HEAD:refs/heads/main'
t 2 'git push origin feature/x:main'
t 2 'git push -u origin +feature/x'
t 2 'git push --force origin feature/x'
t 2 'git push -f origin feature/x'
t 2 'git push --force-with-lease origin feature/x'
t 2 'git push origin --delete main'
t 2 'git push origin :main'
t 2 'git fetch origin && git push origin HEAD:main'
t 2 'git -C . push origin main'
t 0 'git push -u origin feature/42-pi-anlegen'
t 0 'git push origin HEAD'

# Sicherheits-Review: Ein geerbtes CLAUDE_PROJECT_DIR eines anderen Projekts darf den Guard unter Copilot nicht
# ausschalten; COPILOT_PROJECT_DIR (von Copilot für die laufende Session gesetzt) geht vor
stale=$(mktemp -d)
printf '{"toolName":"bash","toolArgs":{"command":"git push origin HEAD:main"}}' \
  | (cd "$stale" && CLAUDE_PROJECT_DIR="$stale" COPILOT_PROJECT_DIR="$root" bash "$G") >/dev/null 2>&1
got=$?
if [ "$got" = 2 ]; then echo "ok   2 COPILOT_PROJECT_DIR geht einem fremden CLAUDE_PROJECT_DIR vor"
else echo "FAIL $got (erwartet 2) COPILOT_PROJECT_DIR geht einem fremden CLAUDE_PROJECT_DIR vor"; fails=$((fails+1)); fi
rm -rf "$stale"
t 0 'git push'
t 0 'git status'
t 0 'echo "git pushen ist morgen"'

# Bash ohne erkennbaren Befehl: fail closed (Plan #24, Architektur-Entscheide), Claude- und Copilot-Payload
check 2 'claude: Bash ohne Befehl' '{"tool_name":"Bash","tool_input":{"description":"Befehl"}}'
check 2 'copilot: Bash ohne Befehl' \
  "$(jq -cn --arg cwd "$root" '{hook_event_name:"PreToolUse",session_id:"c1",timestamp:1793955600000,cwd:$cwd,tool_use_id:"call_1",tool_name:"Bash",tool_input:{description:"Befehl"}}')"

check 2 'copilot eigen: bash ohne Befehl' "$(own_tool bash '{"description":"Befehl"}' "$root")"
# Nur Bash wird geprüft (Plan #24, «Nachtrag»): Lesen und Bearbeiten gehen durch, auch wenn der Text einen Push nennt
check 0 'copilot eigen: view' "$(own_tool view "$(jq -cn --arg p "$root/README.md" '{path:$p,view_range:[1,200]}')" "$root")"
check 0 'copilot eigen: edit mit Push im Text' \
  "$(own_tool edit "$(jq -cn --arg p "$root/README.md" '{path:$p,old_str:"x",new_str:"git push --force origin main"}')" "$root")"
check 0 'copilot eigen: apply_patch mit Push im Text' \
  "$(own_tool apply_patch "$(jq -cn --arg p "$root/README.md" '"*** Begin Patch\n*** Update File: " + $p + "\n@@\n+git push origin HEAD:main\n*** End Patch\n"')" "$root")"
check 0 'claude: Read geht durch' "$(jq -cn --arg p "$root/README.md" '{tool_name:"Read",tool_input:{file_path:$p}}')"
# Unlesbare Eingabe: flow_payload reicht sie durch, der Wächter lehnt wie bisher ab
check 2 'Eingabe, die kein JSON ist' 'git push origin main'

# Push ohne Refspec auf main: temporäres Repository mit flow.md, aktueller Zweig main
tmp=$(mktemp -d)
git -C "$tmp" init -q -b main && mkdir -p "$tmp/.claude" && cp "$root/.claude/flow.md" "$tmp/.claude/"
t 2 'git push' "$tmp"
t 2 'git push origin HEAD' "$tmp"
t 0 'git push origin feature/x' "$tmp"
rm -rf "$tmp"

[ "$fails" = 0 ] || { echo "[ERROR] guard-push: $fails Fälle falsch"; exit 1; }
echo "guard-push: alle Fälle ok"
