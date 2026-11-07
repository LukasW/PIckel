#!/usr/bin/env bash
# Prüft die Eingabe und Ausgabe der Hooks in lib.sh für Claude Code und die Copilot CLI (Issue #24, ADR 0007):
# flow_tool_command, flow_tool_files, flow_deny, flow_hint, dazu check-spelling.sh mit einem Patch, wie ihn Copilot
# mit dem GPT-Modell als String in tool_input liefert (spec/changes/24-copilot-zweiter-agent/notes.md).
# Läuft in scripts/gate.sh und CI.
set -u
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

# --- check-spelling.sh mit Patch-Eingabe ---------------------------------------------------------------------------
# Eigenes Projekt-Root mit flow.md (spelling: swiss); check-spelling prüft nur Dateien darunter.
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/.claude" "$tmp/docs"
printf -- '---\nspelling: swiss\n---\n' >"$tmp/.claude/flow.md"
sz=$'\xc3\x9f'
printf 'Die Strasse ist gross.\n' >"$tmp/docs/sauber.md"
printf 'Die Stra%se ist gro%s.\n' "$sz" "$sz" >"$tmp/docs/eszett.md"

run_spelling() {
  local o e
  o=$(mktemp) e=$(mktemp)
  printf '%s' "$1" | (cd "$tmp" && CLAUDE_PROJECT_DIR="$tmp" bash "$SPELLING") >"$o" 2>"$e"
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
o=$(printf '%s' "$(copilot_patch "$adr_patch")" | CLAUDE_PROJECT_DIR="$tmp" bash "$ADR_HOOK" 2>/dev/null); code=$?
if [ "$code" = 2 ] && [ -n "$o" ] && printf '%s' "$o" | jq -e '.additionalContext | contains("ADR-Kopf verletzt")' >/dev/null 2>&1; then
  ok "adr-index: ADR als zweite Datei eines Patches löst den Index aus, Fehler als JSON"
else fail "adr-index: ADR im Patch" "Exit $code, stdout «$o» (erwartet Exit 2, additionalContext mit der Meldung)"; fi
o=$(printf '%s' "$(copilot_patch "$patch_clean")" | CLAUDE_PROJECT_DIR="$tmp" bash "$ADR_HOOK" 2>/dev/null); code=$?
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

[ "$fails" = 0 ] || { echo "[ERROR] hook-input: $fails Fälle falsch"; exit 1; }
echo "hook-input: alle Fälle ok"
