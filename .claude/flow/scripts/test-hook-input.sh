#!/usr/bin/env bash
# Prüft die Eingabe und Ausgabe der Hooks in lib.sh für Claude Code und die Copilot CLI (Issue #24, ADR 0007):
# flow_tool_command, flow_tool_files, flow_deny, flow_hint, dazu check-spelling.sh mit einem Patch, wie ihn Copilot
# mit dem GPT-Modell als String in tool_input liefert (spec/changes/24-copilot-zweiter-agent/notes.md).
# Läuft in scripts/gate.sh und CI.
set -u
# Laufen die Tests unter Copilot, ginge dessen Projektordner den gesetzten Wurzeln vor (flow_root)
unset COPILOT_PROJECT_DIR
root="$(cd "$(dirname "$0")/../../.." && pwd)"
LIB="$root/.claude/flow/scripts/lib.sh"
SPELLING="$root/.claude/flow/scripts/check-spelling.sh"
fails=0
ok() { echo "ok   $1"; }
fail() { echo "FAIL $1: $2"; fails=$((fails+1)); }

# Ruft eine Funktion aus lib.sh in einer eigenen Shell auf (flow_deny und flow_hint beenden sie).
# Setzt out, err und code.
run_lib() {
  local o e
  o=$(mktemp) e=$(mktemp)
  bash -c '. "$1"; shift; "$@"' _ "$LIB" "$@" >"$o" 2>"$e"
  code=$?
  out=$(cat "$o") err=$(cat "$e")
  rm -f "$o" "$e"
}

# Eingaben wie gemessen (notes.md): Claude Code und die von Copilot übersetzte Payload
claude_bash() { jq -cn --arg c "$1" '{hook_event_name:"PreToolUse",session_id:"s1",transcript_path:"/tmp/t.jsonl",cwd:"/w",tool_name:"Bash",tool_input:{command:$c,description:"Befehl"}}'; }
copilot_bash() { jq -cn --arg c "$1" '{hook_event_name:"PreToolUse",session_id:"c1",timestamp:1793955600000,cwd:"/w",tool_use_id:"call_1",tool_name:"Bash",tool_input:{command:$c,description:"Befehl"}}'; }
copilot_patch() { jq -cn --arg p "$1" '{hook_event_name:"PostToolUse",session_id:"c1",timestamp:1793955600000,cwd:"/w",tool_use_id:"call_2",tool_name:"Edit",tool_input:$p}'; }

# --- flow_tool_command ---------------------------------------------------------------------------------------------
expect_command() {
  local name="$1" payload="$2" want="$3"
  run_lib flow_tool_command "$payload"
  if [ "$code" = 0 ] && [ "$out" = "$want" ]; then ok "flow_tool_command: $name"
  else fail "flow_tool_command: $name" "Exit $code, Ausgabe «$out» (erwartet Exit 0, «$want»)"; fi
}
expect_command "Befehl aus der Claude-Payload" "$(claude_bash 'git push origin main')" 'git push origin main'
expect_command "Befehl aus der Copilot-Payload" "$(copilot_bash 'git push origin main')" 'git push origin main'
multi=$'git commit -m "Ä ö ü \\"zitiert\\""\necho zweite Zeile'
expect_command "Befehl mit Anführungszeichen, Zeilenumbruch und Umlauten bleibt unverändert" "$(copilot_bash "$multi")" "$multi"

expect_no_command() {
  local name="$1" payload="$2"
  run_lib flow_tool_command "$payload"
  if [ "$code" = 1 ] && [ -z "$out" ]; then ok "flow_tool_command: $name"
  else fail "flow_tool_command: $name" "Exit $code, Ausgabe «$out» (erwartet Exit 1, keine Ausgabe)"; fi
}
expect_no_command "Bash ohne command endet mit 1" '{"tool_name":"Bash","tool_input":{"description":"Befehl"}}'
expect_no_command "Bash mit leerem command endet mit 1" '{"tool_name":"Bash","tool_input":{"command":""}}'
expect_no_command "tool_input als String (Patch) ist kein Befehl" "$(copilot_patch 'git push origin main')"
expect_no_command "Eingabe, die kein JSON ist, endet mit 1" 'git push origin main'

# --- flow_tool_files -----------------------------------------------------------------------------------------------
# Reihenfolge der Pfade ist nicht spezifiziert: verglichen wird sortiert und ohne Dubletten.
expect_files() {
  local name="$1" payload="$2" want="$3" got
  run_lib flow_tool_files "$payload"
  got=$(printf '%s\n' "$out" | sed '/^$/d' | sort -u)
  want=$(printf '%s\n' "$want" | sed '/^$/d' | sort -u)
  if [ "$got" = "$want" ]; then ok "flow_tool_files: $name"
  else fail "flow_tool_files: $name" "Exit $code, Ausgabe «$out», stderr «$err» (erwartet «$want»)"; fi
}
expect_files "Pfad aus tool_input.file_path (Claude Edit)" \
  '{"tool_name":"Edit","tool_input":{"file_path":"/w/docs/a.md","old_string":"x","new_string":"y"},"tool_response":{"filePath":"/w/docs/a.md"}}' \
  '/w/docs/a.md'
expect_files "Pfad aus tool_input.path (Copilot edit)" \
  '{"tool_name":"Edit","tool_input":{"path":"/w/README.md","old_str":"x","new_str":"y"}}' \
  '/w/README.md'
expect_files "Pfad aus tool_response.filePath" \
  '{"tool_name":"Write","tool_input":{"content":"x"},"tool_response":{"filePath":"/w/docs/c.md"}}' \
  '/w/docs/c.md'
patch=$'*** Begin Patch\n*** Update File: /w/docs/alt.md\n@@\n-alt\n+neu\n*** Add File: /w/docs/neu.md\n+Inhalt\n*** End Patch\n'
expect_files "Patch mit zwei Dateien (Update und Add)" "$(copilot_patch "$patch")" $'/w/docs/alt.md\n/w/docs/neu.md'
patch_quoted=$'*** Begin Patch\n*** Update File: /w/docs/Ärger "x".md\n@@\n-a\n+b\n*** End Patch\n'
expect_files "Patch-Pfad mit Umlaut und Anführungszeichen" "$(copilot_patch "$patch_quoted")" '/w/docs/Ärger "x".md'
# Nur Kopfzeilen zählen: eine eingefügte Zeile, die wie ein Kopf aussieht, ist Inhalt
patch_content=$'*** Begin Patch\n*** Add File: /w/docs/anleitung.md\n+*** Update File: /w/geheim.md\n*** End Patch\n'
expect_files "Kopfzeile im eingefügten Inhalt ist kein Pfad" "$(copilot_patch "$patch_content")" '/w/docs/anleitung.md'

# Ohne Dubletten: Claude liefert denselben Pfad in file_path und tool_response.filePath
run_lib flow_tool_files '{"tool_name":"Edit","tool_input":{"file_path":"/w/docs/a.md"},"tool_response":{"filePath":"/w/docs/a.md"}}'
if [ "$out" = '/w/docs/a.md' ]; then ok "flow_tool_files: derselbe Pfad zweimal wird einmal ausgegeben"
else fail "flow_tool_files: Dubletten" "Ausgabe «$out» (erwartet genau eine Zeile /w/docs/a.md)"; fi

patch_move=$'*** Begin Patch\n*** Update File: /w/docs/alt.md\n*** Move to: /w/docs/neu.md\n@@\n-a\n+b\n*** End Patch\n'
expect_files "Patch mit Umbenennung liefert auch das Ziel (*** Move to:)" "$(copilot_patch "$patch_move")" \
  $'/w/docs/alt.md\n/w/docs/neu.md'
expect_files "Patch mit CRLF und Leerzeichen am Zeilenende" \
  "$(copilot_patch $'*** Begin Patch\r\n*** Update File: /w/docs/a.md  \r\n*** End Patch\r\n')" '/w/docs/a.md'
# Die interaktive CLI schreibt relative Pfade in den Patch (Prüfung von Hand): gegen cwd der Eingabe auflösen
expect_files "Relativer Pfad im Patch wird gegen cwd aufgelöst" \
  "$(copilot_patch $'*** Begin Patch\n*** Update File: README.md\n*** Add File: ./docs/neu.md\n*** End Patch\n')" \
  $'/w/README.md\n/w/docs/neu.md'
expect_files "Relativer Pfad in tool_input.path wird gegen cwd aufgelöst" \
  '{"tool_name":"Edit","cwd":"/w","tool_input":{"path":"docs/a.md","old_str":"x","new_str":"y"}}' '/w/docs/a.md'

expect_no_files() {
  local name="$1" payload="$2"
  run_lib flow_tool_files "$payload"
  if [ -z "$out" ] && [ -z "$err" ]; then ok "flow_tool_files: $name"
  else fail "flow_tool_files: $name" "Exit $code, Ausgabe «$out», stderr «$err» (erwartet nichts)"; fi
}
expect_no_files "Edit ohne Pfad liefert nichts" '{"tool_name":"Edit","tool_input":{"old_string":"x","new_string":"y"}}'
expect_no_files "Patch, der nur löscht, liefert nichts (*** Delete File:)" \
  "$(copilot_patch $'*** Begin Patch\n*** Delete File: /w/docs/weg.md\n*** End Patch\n')"
expect_no_files "Patch-Text ohne Kopfzeile liefert nichts" "$(copilot_patch $'kein Patch\nnur Text')"

# --- flow_deny und flow_hint ---------------------------------------------------------------------------------------
reason=$'guard-test: «Push» auf "main" abgelehnt.\nZweite Zeile mit Ä, ö, ü und \\ Backslash'

run_lib flow_deny "$reason"
if [ "$code" = 2 ]; then ok "flow_deny: Exit 2"; else fail "flow_deny" "Exit $code (erwartet 2)"; fi
if [[ "$err" == *"$reason"* ]]; then ok "flow_deny: Grund auf stderr"; else fail "flow_deny" "stderr «$err» enthält den Grund nicht"; fi
if [ -n "$out" ] && printf '%s' "$out" | jq -e --arg r "$reason" '.permissionDecision == "deny" and .permissionDecisionReason == $r' >/dev/null 2>&1; then
  ok "flow_deny: gültiges JSON mit permissionDecision deny und dem Grund (Sonderzeichen)"
else fail "flow_deny" "stdout ist kein JSON mit permissionDecision deny und dem Grund: «$out»"; fi

run_lib flow_hint "$reason"
if [ "$code" = 2 ]; then ok "flow_hint: Exit 2"; else fail "flow_hint" "Exit $code (erwartet 2)"; fi
if [[ "$err" == *"$reason"* ]]; then ok "flow_hint: Meldung auf stderr"; else fail "flow_hint" "stderr «$err» enthält die Meldung nicht"; fi
if [ -n "$out" ] && printf '%s' "$out" | jq -e --arg r "$reason" '.additionalContext == $r' >/dev/null 2>&1; then
  ok "flow_hint: gültiges JSON mit der Meldung in additionalContext (Sonderzeichen)"
else fail "flow_hint" "stdout ist kein JSON mit der Meldung in additionalContext: «$out»"; fi

# --- flow_payload: eigenes Format der Copilot CLI → Claude-Format ----------------------------------------------------
# Plan #24, «Nachtrag: interaktive CLI»: toolArgs → tool_input, toolResult → tool_response, sessionId → session_id,
# toolCallId → tool_use_id; bash → Bash, view → Read, rg/grep → Grep, glob → Glob, edit/create/apply_patch → Edit,
# andere Werkzeuge unverändert. Eingaben wie gemessen (notes.md, «Interaktive CLI lädt keine Claude-Hooks» und Spike).
# Geprüft werden nur die Felder, die der Plan nennt; ob die camelCase-Felder bleiben, ist nicht spezifiziert.
run_payload() {
  local o e
  o=$(mktemp) e=$(mktemp)
  printf '%s' "$1" | bash -c '. "$1"; flow_payload' _ "$LIB" >"$o" 2>"$e"
  code=$?
  out=$(cat "$o") err=$(cat "$e")
  rm -f "$o" "$e"
}
# Eigenes Format: toolName ($1), toolArgs als JSON ($2), optional toolResult als JSON ($3)
own() {
  jq -cn --arg n "$1" --argjson a "$2" --argjson r "${3:-null}" \
    '{sessionId:"c7",timestamp:1793955600000,cwd:"/w",toolCallId:"call_7",toolName:$n,toolArgs:$a}
     + (if $r == null then {} else {toolResult:$r} end)'
}
expect_payload() {
  local name="$1" payload="$2" tool="$3" args="$4" result="${5:-null}"
  run_payload "$payload"
  if [ -n "$out" ] && printf '%s' "$out" | jq -e --arg t "$tool" --argjson a "$args" --argjson r "$result" '
      .tool_name == $t and .tool_input == $a and .session_id == "c7" and .tool_use_id == "call_7"
      and (if $r == null then (has("tool_response") | not) else .tool_response == $r end)' >/dev/null 2>&1; then
    ok "flow_payload: $name"
  else fail "flow_payload: $name" "Exit $code, Ausgabe «$out» (erwartet tool_name $tool, tool_input $args, tool_response $result)"; fi
}
a='{"command":"echo hallo","description":"Gruss","mode":"sync","initial_wait":10}'
expect_payload "bash → Bash" "$(own bash "$a")" Bash "$a"
a='{"path":"/w/README.md","view_range":[1,200]}'
expect_payload "view → Read" "$(own view "$a")" Read "$a"
a='{"pattern":"Hexagon","paths":"docs"}'
expect_payload "rg → Grep (paths relativ)" "$(own rg "$a")" Grep "$a"
a='{"pattern":"Hexagon","paths":"/w/docs"}'
expect_payload "grep → Grep" "$(own grep "$a")" Grep "$a"
a='{"pattern":"**/*.md"}'
expect_payload "glob → Glob" "$(own glob "$a")" Glob "$a"
a=$(jq -cn --arg p $'*** Begin Patch\n*** Update File: /w/docs/a.md\n@@\n-alt\n+neu\n*** End Patch\n' '$p')
expect_payload "apply_patch → Edit, Patch als String in tool_input" "$(own apply_patch "$a")" Edit "$a"
a='{"path":"/w/README.md","old_str":"alt","new_str":"neu"}'
expect_payload "edit → Edit" "$(own edit "$a")" Edit "$a"
a='{"path":"/w/docs/neu.md","file_text":"Inhalt"}'
expect_payload "create → Edit" "$(own create "$a")" Edit "$a"
a='{"command":"ls","description":"Liste"}'
expect_payload "anderes Werkzeug bleibt unverändert (write_bash)" "$(own write_bash "$a")" write_bash "$a"
a='{"command":"echo hallo","description":"Gruss"}'
r='{"resultType":"success","textResultForLlm":"hallo\n"}'
expect_payload "postToolUse: toolResult → tool_response" "$(own bash "$a" "$r")" Bash "$a" "$r"

# Claude-Format bleibt unverändert, auch mit einem zusätzlichen toolName (erkennbar ist Copilot nur an toolName
# ohne tool_name); verglichen wird der JSON-Inhalt
expect_payload_same() {
  local name="$1" payload="$2"
  run_payload "$payload"
  if [ -n "$out" ] && printf '%s' "$out" | jq -e --argjson p "$payload" '. == $p' >/dev/null 2>&1; then
    ok "flow_payload: $name"
  else fail "flow_payload: $name" "Exit $code, Ausgabe «$out» (erwartet unverändert «$payload»)"; fi
}
expect_payload_same "Claude-Format bleibt unverändert" \
  '{"hook_event_name":"PostToolUse","session_id":"s1","tool_use_id":"toolu_1","tool_name":"Edit","tool_input":{"file_path":"/w/a.md","old_string":"x","new_string":"y"},"tool_response":{"filePath":"/w/a.md"}}'
expect_payload_same "Claude-Format mit zusätzlichem toolName bleibt unverändert" \
  '{"tool_name":"Bash","tool_input":{"command":"git status"},"toolName":"view","toolArgs":{"path":"/w/README.md"}}'
# Unlesbare Eingabe: unverändert weiterreichen (die Guards lehnen dann wie bisher ab)
nojson=$'git push origin main\n{kein JSON'
run_payload "$nojson"
if [ "$out" = "$nojson" ]; then ok "flow_payload: Eingabe, die kein JSON ist, geht unverändert durch"
else fail "flow_payload: kein JSON" "Ausgabe «$out» (erwartet unverändert «$nojson»)"; fi

# --- .github/hooks/flow.json: Registrierung für die interaktive Copilot CLI ------------------------------------------
FLOW_JSON="$root/.github/hooks/flow.json"
if [ -n "$(jq -c . "$FLOW_JSON" 2>/dev/null)" ]; then ok "flow.json: gültiges JSON"; else fail "flow.json" "kein gültiges JSON"; fi
if [ "$(jq -r '.version' "$FLOW_JSON" 2>/dev/null)" = 1 ]; then ok "flow.json: version 1"
else fail "flow.json" "version ist nicht 1"; fi
# Skript unter genau einem Ereignis; Befehl mit ${COPILOT_PROJECT_DIR:-.}, Pfad existiert
expect_registered() {
  local event="$1" script="$2" other cmds
  [ "$event" = preToolUse ] && other=postToolUse || other=preToolUse
  cmds=$(jq -r --arg e "$event" --arg s "$script" \
    '.hooks[$e][]? | select(.type == "command" and (.bash | type == "string") and (.bash | contains($s))) | .bash' "$FLOW_JSON" 2>/dev/null)
  if [ "$(printf '%s\n' "$cmds" | sed '/^$/d' | wc -l | tr -d ' ')" = 1 ] && [[ "$cmds" == *'${COPILOT_PROJECT_DIR:-.}'* ]] \
    && [ -f "$root/$script" ]; then
    ok "flow.json: $script unter $event"
  else fail "flow.json: $script" "nicht genau einmal unter $event als command mit \${COPILOT_PROJECT_DIR:-.} registriert: «$cmds»"; fi
  if [ -n "$(jq -r --arg e "$other" --arg s "$script" '.hooks[$e][]? | .bash // "" | select(contains($s))' "$FLOW_JSON" 2>/dev/null)" ]; then
    fail "flow.json: $script" "auch unter $other registriert"
  fi
}
for s in guard-env guard-labels guard-push; do expect_registered preToolUse ".claude/flow/scripts/$s.sh"; done
for s in .claude/hooks/reviewer-reminder.sh .claude/flow/scripts/check-spelling.sh .claude/flow/scripts/check-test-edit.sh \
  .claude/hooks/adr-index.sh; do expect_registered postToolUse "$s"; done
n=$(jq '[.hooks.preToolUse[]?, .hooks.postToolUse[]?] | length' "$FLOW_JSON" 2>/dev/null)
if [ "$n" = 7 ]; then ok "flow.json: genau sieben Registrierungen"; else fail "flow.json" "$n Registrierungen (erwartet 7)"; fi

# Die registrierten Befehle, so ausgeführt wie von der interaktiven CLI (eigenes Format, COPILOT_PROJECT_DIR, ohne
# CLAUDE_PROJECT_DIR): Lesen von README geht überall durch, ein Push auf main lehnt guard-push ab
fj_tmp=$(mktemp -d)
run_registered() {
  local event="$1" payload="$2" want="$3" label="$4" cmd got
  while IFS= read -r cmd; do
    [ -n "$cmd" ] || continue
    printf '%s' "$payload" | (cd "$root" && env -u CLAUDE_PROJECT_DIR COPILOT_PROJECT_DIR="$root" TMPDIR="$fj_tmp" bash -c "$cmd") >/dev/null 2>&1
    got=$?
    if [ "$got" = "$want" ]; then ok "flow.json $event: $label — ${cmd##*/}"
    else fail "flow.json $event: $label — ${cmd##*/}" "Exit $got (erwartet $want)"; fi
  done < <(jq -r --arg e "$event" '.hooks[$e][]?.bash' "$FLOW_JSON" 2>/dev/null)
}
view_readme=$(jq -cn --arg p "$root/README.md" '{sessionId:"fj1",timestamp:1793955600000,cwd:"/w",toolCallId:"fj_1",toolName:"view",toolArgs:{path:$p,view_range:[1,20]}}')
run_registered preToolUse "$view_readme" 0 "view auf README geht durch"
run_registered postToolUse "$(printf '%s' "$view_readme" | jq -c '. + {toolResult:{resultType:"success",textResultForLlm:"x"}}')" 0 "view auf README bleibt still"
push_main=$(jq -cn '{sessionId:"fj1",timestamp:1793955600000,cwd:"/w",toolCallId:"fj_2",toolName:"bash",toolArgs:{command:"git push origin HEAD:main",description:"Push"}}')
o=$(printf '%s' "$push_main" | (cd "$root" && env -u CLAUDE_PROJECT_DIR COPILOT_PROJECT_DIR="$root" \
  bash -c "$(jq -r '.hooks.preToolUse[]?.bash | select(contains("guard-push"))' "$FLOW_JSON")") 2>/dev/null); code=$?
if [ "$code" = 2 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '.permissionDecision == "deny"' >/dev/null 2>&1; then
  ok "flow.json preToolUse: guard-push lehnt Push auf main im eigenen Format ab"
else fail "flow.json preToolUse: guard-push" "Exit $code, stdout «$o» (erwartet Exit 2 mit deny-JSON)"; fi
rm -rf "$fj_tmp"

# --- check-spelling.sh mit Patch-Eingabe ---------------------------------------------------------------------------
# Eigenes Projekt-Root mit flow.md (spelling: swiss); check-spelling prüft nur Dateien darunter.
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/.claude" "$tmp/docs"
printf -- '---\nspelling: swiss\n---\n' >"$tmp/.claude/flow.md"
sz=$'\xc3\x9f'
printf 'Die Strasse ist gross.\n' >"$tmp/docs/sauber.md"
printf 'Die Stra%se ist gro%s.\n' "$sz" "$sz" >"$tmp/docs/eszett.md"

# Jeder Aufruf mit eigenem TMPDIR: flow_hint meldet je tool_use_id und Hook nur einmal (Plan #24, N2), und
# copilot_patch trägt immer dieselbe tool_use_id. Mit $spelling_state teilen sich Aufrufe den Zustand (Dedup-Fälle).
run_spelling() {
  local o e state="${spelling_state:-}"
  o=$(mktemp) e=$(mktemp)
  [ -n "$state" ] || state=$(mktemp -d "$tmp/state.XXXXXX")
  printf '%s' "$1" | (cd "$tmp" && CLAUDE_PROJECT_DIR="$tmp" TMPDIR="$state" bash "$SPELLING") >"$o" 2>"$e"
  code=$?
  out=$(cat "$o") err=$(cat "$e")
  rm -f "$o" "$e"
}
expect_spelling_hit() {
  local name="$1" payload="$2" rel="docs/eszett.md"
  run_spelling "$payload"
  if [ "$code" = 2 ]; then ok "check-spelling: $name — Exit 2"; else fail "check-spelling: $name" "Exit $code (erwartet 2), stderr «$err»"; fi
  if [[ "$err" == *"$rel"* ]]; then ok "check-spelling: $name — Meldung auf stderr"; else fail "check-spelling: $name" "stderr «$err» nennt $rel nicht"; fi
  if [ -n "$out" ] && printf '%s' "$out" | jq -e --arg f "$rel" '.additionalContext | type == "string" and contains($f)' >/dev/null 2>&1; then
    ok "check-spelling: $name — JSON mit additionalContext"
  else fail "check-spelling: $name" "stdout ist kein JSON mit additionalContext zu $rel: «$out»"; fi
}
# Die Datei mit Eszett ist die zweite des Patches: Ein Patch kann mehrere Dateien enthalten
patch_hit="*** Begin Patch
*** Update File: $tmp/docs/sauber.md
@@
-alt
+Die Strasse ist gross.
*** Add File: $tmp/docs/eszett.md
+Die Stra${sz}e ist gro${sz}.
*** End Patch
"
expect_spelling_hit "Copilot-Patch mit Eszett in der zweiten Datei" "$(copilot_patch "$patch_hit")"
expect_spelling_hit "Claude-Edit mit Eszett" \
  "$(jq -cn --arg f "$tmp/docs/eszett.md" '{hook_event_name:"PostToolUse",tool_name:"Edit",tool_input:{file_path:$f,old_string:"a",new_string:"b"},tool_response:{filePath:$f}}')"

patch_clean="*** Begin Patch
*** Update File: $tmp/docs/sauber.md
@@
-alt
+Die Strasse ist gross.
*** End Patch
"
run_spelling "$(copilot_patch "$patch_clean")"
if [ "$code" = 0 ] && [ -z "$out" ] && [ -z "$err" ]; then ok "check-spelling: Copilot-Patch ohne Eszett bleibt still"
else fail "check-spelling: Copilot-Patch ohne Eszett" "Exit $code, stdout «$out», stderr «$err» (erwartet Exit 0, keine Ausgabe)"; fi

# Eszett in beiden Dateien eines Patches: Die Meldung nennt beide
cp "$tmp/docs/eszett.md" "$tmp/docs/eszett2.md"
run_spelling "$(copilot_patch "*** Begin Patch
*** Update File: $tmp/docs/eszett.md
*** Update File: $tmp/docs/eszett2.md
*** End Patch
")"
if [ "$code" = 2 ] && [ -n "$out" ] && printf '%s' "$out" | jq -e '.additionalContext | contains("docs/eszett.md") and contains("docs/eszett2.md")' >/dev/null 2>&1; then
  ok "check-spelling: Eszett in zwei Dateien eines Patches — beide gemeldet"
else fail "check-spelling: zwei Dateien mit Eszett" "Exit $code, stdout «$out»"; fi

# Still: Pfad ausserhalb des Roots, unter .git/, Endung ausserhalb der Liste
outside=$(mktemp -d)
printf 'Stra%se\n' "$sz" >"$outside/aussen.md"
mkdir -p "$tmp/.git"
printf 'Stra%se\n' "$sz" >"$tmp/.git/COMMIT_EDITMSG.md"
printf 'Stra%se\n' "$sz" >"$tmp/docs/bild.png"
for f in "$outside/aussen.md" "$tmp/.git/COMMIT_EDITMSG.md" "$tmp/docs/bild.png"; do
  run_spelling "$(copilot_patch "*** Begin Patch
*** Update File: $f
*** End Patch
")"
  if [ "$code" = 0 ] && [ -z "$out" ]; then ok "check-spelling: ${f#"$tmp"/} bleibt still"
  else fail "check-spelling: ${f#"$tmp"/}" "Exit $code, stdout «$out» (erwartet Exit 0, keine Ausgabe)"; fi
done
rm -rf "$outside"

# Pfad aus tool_input.path (Copilot edit) erreicht den Hook
run_spelling "$(jq -cn --arg f "$tmp/docs/eszett.md" '{tool_name:"Edit",tool_input:{path:$f,old_str:"a",new_str:"b"}}')"
if [ "$code" = 2 ]; then ok "check-spelling: Pfad aus tool_input.path wird geprüft"
else fail "check-spelling: tool_input.path" "Exit $code (erwartet 2)"; fi

# --- adr-index.sh: ADR als zweite Datei eines Patches, Fehler des Index-Skripts über flow_hint ------------------------
ADR_HOOK="$root/.claude/hooks/adr-index.sh"
mkdir -p "$tmp/scripts" "$tmp/docs/adr" "$tmp/.claude/flow/scripts"
cp "$LIB" "$tmp/.claude/flow/scripts/lib.sh"
printf '#!/usr/bin/env bash\necho "[ERROR] ADR-Kopf verletzt"\nexit 1\n' >"$tmp/scripts/adr-index.sh"
adr_patch="*** Begin Patch
*** Update File: $tmp/docs/sauber.md
*** Add File: $tmp/docs/adr/0099-test.md
*** End Patch
"
o=$(printf '%s' "$(copilot_patch "$adr_patch")" | CLAUDE_PROJECT_DIR="$tmp" TMPDIR="$(mktemp -d "$tmp/state.XXXXXX")" bash "$ADR_HOOK" 2>/dev/null); code=$?
if [ "$code" = 2 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '.additionalContext | contains("ADR-Kopf verletzt")' >/dev/null 2>&1; then
  ok "adr-index: ADR als zweite Datei eines Patches löst den Index aus, Fehler als JSON"
else fail "adr-index: ADR im Patch" "Exit $code, stdout «$o» (erwartet Exit 2, additionalContext mit der Meldung)"; fi
o=$(printf '%s' "$(copilot_patch "$patch_clean")" | CLAUDE_PROJECT_DIR="$tmp" TMPDIR="$(mktemp -d "$tmp/state.XXXXXX")" bash "$ADR_HOOK" 2>/dev/null); code=$?
if [ "$code" = 0 ] && [ -z "$o" ]; then ok "adr-index: Patch ohne ADR bleibt still"
else fail "adr-index: Patch ohne ADR" "Exit $code, stdout «$o»"; fi

# --- reviewer-reminder.sh: additionalContext auch auf oberster Ebene (Copilot) -----------------------------------------
REMINDER="$root/.claude/hooks/reviewer-reminder.sh"
sid="hookinput-$$"
o=$(printf '%s' "$(jq -cn --arg s "$sid" --arg f "$root/src/test/resources/features/x.feature" \
  '{session_id:$s,tool_name:"Edit",tool_input:("*** Begin Patch\n*** Update File: " + $f + "\n*** End Patch\n")}')" \
  | CLAUDE_PROJECT_DIR="$root" TMPDIR="$tmp" bash "$REMINDER" 2>/dev/null); code=$?
if [ "$code" = 0 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '(.additionalContext | contains("bdd-cucumber-author")) and (.hookSpecificOutput.additionalContext == .additionalContext)' >/dev/null 2>&1; then
  ok "reviewer-reminder: Patch-Eingabe, Hinweis in additionalContext und hookSpecificOutput"
else fail "reviewer-reminder" "Exit $code, stdout «$o»"; fi

# Patch mit zwei Dateien: jede zählt, auch wenn die alphabetisch erste keine Review-Regel trifft
sid2="hookinput2-$$"
o=$(printf '%s' "$(jq -cn --arg s "$sid2" --arg a "$root/docs/arc42.md" --arg j "$root/src/main/java/ch/grudligstrasse/pikel/domain/model/X.java" \
  '{session_id:$s,tool_name:"Edit",tool_input:("*** Begin Patch\n*** Update File: " + $j + "\n*** Update File: " + $a + "\n*** End Patch\n")}')" \
  | CLAUDE_PROJECT_DIR="$root" TMPDIR="$tmp" bash "$REMINDER" 2>/dev/null); code=$?
if [ "$code" = 0 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '.additionalContext | contains("hexagonal-reviewer")' >/dev/null 2>&1; then
  ok "reviewer-reminder: Java-Datei als eine von zwei Dateien eines Patches wird erkannt"
else fail "reviewer-reminder: Patch mit zwei Dateien" "Exit $code, stdout «$o»"; fi

# --- Hinweis-Hooks im eigenen Format der Copilot CLI (.github/hooks/flow.json, Plan #24 «Nachtrag») ------------------
# postToolUse wie gemessen: toolCallId, toolName, toolArgs, toolResult. Die Hinweis-Hooks reagieren nur auf
# Edit|Write|MultiEdit|NotebookEdit (nach der Übersetzung: edit, create, apply_patch), nicht auf view.
own_post() {
  jq -cn --arg n "$1" --argjson a "$2" --arg id "${3:-own_1}" \
    '{sessionId:"c8",timestamp:1793955600000,cwd:"/w",toolCallId:$id,toolName:$n,toolArgs:$a,toolResult:{resultType:"success",textResultForLlm:"ok"}}'
}
own_patch() { own_post apply_patch "$(jq -cn --arg p "$1" '$p')" "${2:-own_1}"; }
expect_silent() {
  local name="$1"
  if [ "$code" = 0 ] && [ -z "$out" ] && [ -z "$err" ]; then ok "$name"
  else fail "$name" "Exit $code, stdout «$out», stderr «$err» (erwartet Exit 0, keine Ausgabe)"; fi
}

expect_spelling_hit "eigenes Format: apply_patch mit Eszett" "$(own_patch "*** Begin Patch
*** Update File: $tmp/docs/eszett.md
@@
-alt
+Die Stra${sz}e
*** End Patch
")"
expect_spelling_hit "eigenes Format: edit mit Eszett" \
  "$(own_post edit "$(jq -cn --arg f "$tmp/docs/eszett.md" '{path:$f,old_str:"alt",new_str:"neu"}')")"
# Lesen ist kein Edit: Dieselbe Datei mit Eszett, nur das Werkzeug weicht ab
run_spelling "$(own_post view "$(jq -cn --arg f "$tmp/docs/eszett.md" '{path:$f,view_range:[1,20]}')")"
expect_silent "check-spelling: eigenes Format, view auf eine Datei mit Eszett bleibt still"
run_spelling "$(jq -cn --arg f "$tmp/docs/eszett.md" '{hook_event_name:"PostToolUse",session_id:"s8",tool_use_id:"toolu_r",tool_name:"Read",tool_input:{file_path:$f},tool_response:{filePath:$f}}')"
expect_silent "check-spelling: Claude-Format, Read auf eine Datei mit Eszett bleibt still"

# Dedup (N2): flow_hint meldet je tool_use_id und Hook einmal; Zustand unter TMPDIR
claude_edit_id() { jq -cn --arg f "$tmp/docs/eszett.md" --arg id "$1" '{hook_event_name:"PostToolUse",session_id:"s9",tool_use_id:$id,tool_name:"Edit",tool_input:{file_path:$f,old_string:"a",new_string:"b"},tool_response:{filePath:$f}}'; }
claude_edit_noid() { jq -cn --arg f "$tmp/docs/eszett.md" '{hook_event_name:"PostToolUse",session_id:"s9",tool_name:"Edit",tool_input:{file_path:$f,old_string:"a",new_string:"b"},tool_response:{filePath:$f}}'; }
eszett_patch="*** Begin Patch
*** Update File: $tmp/docs/eszett.md
*** End Patch
"
spelling_state=$(mktemp -d "$tmp/state.XXXXXX")
expect_spelling_hit "Dedup, Claude-Format: erster Aufruf mit tool_use_id meldet" "$(claude_edit_id dup_a)"
run_spelling "$(claude_edit_id dup_a)"
expect_silent "check-spelling: Dedup, Claude-Format: zweiter Aufruf mit gleicher tool_use_id bleibt still"
expect_spelling_hit "Dedup: andere tool_use_id meldet wieder" "$(claude_edit_id dup_b)"
spelling_state=$(mktemp -d "$tmp/state.XXXXXX")
expect_spelling_hit "Dedup, eigenes Format: erster Aufruf meldet" "$(own_patch "$eszett_patch" dup_c)"
run_spelling "$(own_patch "$eszett_patch" dup_c)"
expect_silent "check-spelling: Dedup, eigenes Format: zweiter Aufruf mit gleicher toolCallId bleibt still"
# Prompt-Modus: derselbe Aufruf kommt aus settings.json (Claude-Format) und aus flow.json (eigenes Format)
spelling_state=$(mktemp -d "$tmp/state.XXXXXX")
expect_spelling_hit "Dedup über beide Quellen: Claude-Format zuerst meldet" "$(claude_edit_id dup_d)"
run_spelling "$(own_patch "$eszett_patch" dup_d)"
expect_silent "check-spelling: Dedup über beide Quellen: eigenes Format mit derselben toolCallId bleibt still"
# Ohne tool_use_id wie bisher: jeder Aufruf meldet
spelling_state=$(mktemp -d "$tmp/state.XXXXXX")
expect_spelling_hit "ohne tool_use_id: erster Aufruf meldet" "$(claude_edit_noid)"
expect_spelling_hit "ohne tool_use_id: zweiter Aufruf meldet wieder" "$(claude_edit_noid)"
# Je Hook: adr-index meldet zur selben tool_use_id, obwohl check-spelling schon gemeldet hat
spelling_state=$(mktemp -d "$tmp/state.XXXXXX")
adr_dup_patch="*** Begin Patch
*** Update File: $tmp/docs/eszett.md
*** Add File: $tmp/docs/adr/0099-test.md
*** End Patch
"
expect_spelling_hit "Dedup je Hook: check-spelling meldet" "$(own_patch "$adr_dup_patch" dup_e)"
o=$(printf '%s' "$(own_patch "$adr_dup_patch" dup_e)" | CLAUDE_PROJECT_DIR="$tmp" TMPDIR="$spelling_state" bash "$ADR_HOOK" 2>/dev/null); code=$?
if [ "$code" = 2 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '.additionalContext | contains("ADR-Kopf verletzt")' >/dev/null 2>&1; then
  ok "adr-index: Dedup je Hook — meldet zur selben tool_use_id nach check-spelling"
else fail "adr-index: Dedup je Hook" "Exit $code, stdout «$o» (erwartet Exit 2, additionalContext mit der Meldung)"; fi
unset spelling_state

# adr-index im eigenen Format: apply_patch auf eine ADR löst den Index aus, view nicht
o=$(printf '%s' "$(own_patch "$adr_patch" adr_own)" | CLAUDE_PROJECT_DIR="$tmp" TMPDIR="$(mktemp -d "$tmp/state.XXXXXX")" bash "$ADR_HOOK" 2>/dev/null); code=$?
if [ "$code" = 2 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '.additionalContext | contains("ADR-Kopf verletzt")' >/dev/null 2>&1; then
  ok "adr-index: eigenes Format, apply_patch auf eine ADR löst den Index aus"
else fail "adr-index: eigenes Format, apply_patch" "Exit $code, stdout «$o» (erwartet Exit 2, additionalContext mit der Meldung)"; fi
o=$(printf '%s' "$(own_post view "$(jq -cn --arg f "$tmp/docs/adr/0099-test.md" '{path:$f}')" adr_view)" \
  | CLAUDE_PROJECT_DIR="$tmp" TMPDIR="$(mktemp -d "$tmp/state.XXXXXX")" bash "$ADR_HOOK" 2>/dev/null); code=$?
if [ "$code" = 0 ] && [ -z "$o" ]; then ok "adr-index: eigenes Format, view auf eine ADR bleibt still"
else fail "adr-index: eigenes Format, view" "Exit $code, stdout «$o» (erwartet Exit 0, keine Ausgabe)"; fi

# reviewer-reminder im eigenen Format: apply_patch auf eine Feature-Datei erinnert, Lesen einer Java-Datei nicht
o=$(printf '%s' "$(jq -cn --arg s "own-rr-$$" --arg f "$root/src/test/resources/features/y.feature" \
  '{sessionId:$s,timestamp:1793955600000,cwd:"/w",toolCallId:"rr_1",toolName:"apply_patch",toolArgs:("*** Begin Patch\n*** Update File: " + $f + "\n*** End Patch\n"),toolResult:{resultType:"success",textResultForLlm:"ok"}}')" \
  | CLAUDE_PROJECT_DIR="$root" TMPDIR="$tmp" bash "$REMINDER" 2>/dev/null); code=$?
if [ "$code" = 0 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '.additionalContext | contains("bdd-cucumber-author")' >/dev/null 2>&1; then
  ok "reviewer-reminder: eigenes Format, apply_patch auf eine Feature-Datei erinnert"
else fail "reviewer-reminder: eigenes Format, apply_patch" "Exit $code, stdout «$o»"; fi
o=$(printf '%s' "$(jq -cn --arg s "own-rr2-$$" --arg f "$root/src/main/java/ch/grudligstrasse/pikel/domain/model/X.java" \
  '{sessionId:$s,timestamp:1793955600000,cwd:"/w",toolCallId:"rr_2",toolName:"view",toolArgs:{path:$f},toolResult:{resultType:"success",textResultForLlm:"ok"}}')" \
  | CLAUDE_PROJECT_DIR="$root" TMPDIR="$tmp" bash "$REMINDER" 2>/dev/null); code=$?
if [ "$code" = 0 ] && [ -z "$o" ]; then ok "reviewer-reminder: eigenes Format, view auf eine Java-Datei bleibt still"
else fail "reviewer-reminder: eigenes Format, view" "Exit $code, stdout «$o» (erwartet Exit 0, keine Ausgabe)"; fi
o=$(printf '%s' "$(jq -cn --arg s "claude-rr3-$$" --arg f "$root/src/main/java/ch/grudligstrasse/pikel/domain/model/X.java" \
  '{session_id:$s,tool_use_id:"toolu_rr3",tool_name:"Read",tool_input:{file_path:$f}}')" \
  | CLAUDE_PROJECT_DIR="$root" TMPDIR="$tmp" bash "$REMINDER" 2>/dev/null); code=$?
if [ "$code" = 0 ] && [ -z "$o" ]; then ok "reviewer-reminder: Claude-Format, Read auf eine Java-Datei bleibt still"
else fail "reviewer-reminder: Claude-Format, Read" "Exit $code, stdout «$o» (erwartet Exit 0, keine Ausgabe)"; fi

# --- flow_shell_segments (Issue #26, Plan «Normalisierung») -----------------------------------------------------------
# Zeilenfortsetzung entfernen, an ; & | ( ) ` und Umbruch trennen (ein Segment pro Zeile, leere weglassen), dann
# " ' \ entfernen. Leerzeichen am Rand eines Segments sind nicht spezifiziert: verglichen wird ohne sie, Zeilen nur
# aus Leerzeichen zählen als leer.
segments_of() {
  run_lib flow_shell_segments "$1"
  segs=$(printf '%s\n' "$out" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//' | sed '/^$/d')
}
expect_segments() {
  local name="$1" cmd="$2" want="$3"
  segments_of "$cmd"
  if [ "$code" = 0 ] && [ "$segs" = "$want" ]; then ok "flow_shell_segments: $name"
  else fail "flow_shell_segments: $name" "Exit $code, Segmente «$segs», stderr «$err» (erwartet Exit 0, «$want»)"; fi
}
expect_segments "Zeilenfortsetzung ergibt ein Segment" $'git push origin \\\nHEAD:main' 'git push origin HEAD:main'
expect_segments "Anführungszeichen um ein Wort fallen weg" '"git" push origin main' 'git push origin main'
expect_segments "einfache Anführungszeichen fallen weg" "'git' push" 'git push'
expect_segments "getrennte Anführungszeichen im Wort fallen weg" 'cat .e""nv' 'cat .env'
expect_segments "Backslash im Wort fällt weg" 'cat .\env' 'cat .env'
expect_segments "Trennen an ; & und |" 'git status; git log && git diff | less & echo x' \
  $'git status\ngit log\ngit diff\nless\necho x'
expect_segments "Trennen am Umbruch" $'git status\ngit push origin main' $'git status\ngit push origin main'
expect_segments "Trennen an Backticks" 'echo `git push x` y' $'echo\ngit push x\ny'
expect_segments "Text in Anführungszeichen wird zum Befehl (sh -c)" 'sh -c "git push origin main"' 'sh -c git push origin main'
expect_segments "Subshell in Klammern ist ein eigenes Segment" '(git push x)' 'git push x'

segments_of 'echo $(git push x)'
if [ "$code" = 0 ] && printf '%s\n' "$segs" | grep -qxF 'git push x' && ! printf '%s\n' "$segs" | grep -q 'echo.*push'; then
  ok "flow_shell_segments: \$(…) ergibt ein eigenes Segment"
else fail "flow_shell_segments: \$(…)" "Exit $code, Segmente «$segs», stderr «$err» (erwartet eine Zeile «git push x», getrennt von echo)"; fi

for empty in '' ';;' $'\n\n' ' ; & | '; do
  run_lib flow_shell_segments "$empty"
  if [ "$code" = 0 ] && [ -z "$(printf '%s' "$out" | tr -d '[:space:]')" ]; then ok "flow_shell_segments: «${empty//$'\n'/\\n}» ergibt keine Zeile"
  else fail "flow_shell_segments: «${empty//$'\n'/\\n}»" "Exit $code, Ausgabe «$out», stderr «$err» (erwartet Exit 0, keine Zeile)"; fi
done

# Nachtrag Reviewer-Runde (#26): < und > werden zu Leerzeichen; verglichen wird mit zusammengefassten Leerzeichen
expect_words() {
  local name="$1" cmd="$2" want="$3" got
  segments_of "$cmd"
  got=$(printf '%s\n' "$segs" | tr -s ' \t' '  ')
  if [ "$code" = 0 ] && [ "$got" = "$want" ]; then ok "flow_shell_segments: $name"
  else fail "flow_shell_segments: $name" "Exit $code, Segmente «$segs», stderr «$err» (erwartet Exit 0, «$want»)"; fi
}
expect_words "Umleitung > klebt nicht am Wort" 'git push origin main>/dev/null' 'git push origin main /dev/null'
expect_words "Umleitung < klebt nicht am Wort" 'cat<x' 'cat x'
expect_words "Umleitung mit Dateideskriptor" 'cat 0<.en[v]' 'cat 0 .en[v]'

[ "$fails" = 0 ] || { echo "[ERROR] hook-input: $fails Fälle falsch"; exit 1; }
echo "hook-input: alle Fälle ok"
