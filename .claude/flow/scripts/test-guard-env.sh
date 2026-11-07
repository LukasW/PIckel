#!/usr/bin/env bash
# Prüft guard-env.sh (Issue #24, Test-Strategie): je Fall erwarteter Exit-Code und Befehl, mit der Payload von Claude
# Code und mit der von der Copilot CLI übersetzten (notes.md: mit timestamp und tool_use_id, ohne transcript_path).
# Bei einer Ablehnung steht der Grund zusätzlich als JSON mit permissionDecision deny auf stdout, sonst zeigt Copilot
# ihn nicht. Läuft in scripts/gate.sh und CI.
set -u
# Laufen die Tests unter Copilot, ginge dessen Projektordner den gesetzten Wurzeln vor (flow_root)
unset COPILOT_PROJECT_DIR
root="$(cd "$(dirname "$0")/../../.." && pwd)"
G="$root/.claude/flow/scripts/guard-env.sh"
export CLAUDE_PROJECT_DIR="$root"
fails=0

claude_payload() { jq -cn --arg c "$1" '{tool_input:{command:$c}}'; }
copilot_payload() { jq -cn --arg c "$1" --arg cwd "$root" '{hook_event_name:"PreToolUse",session_id:"c1",timestamp:1793955600000,cwd:$cwd,tool_use_id:"call_1",tool_name:"Bash",tool_input:{command:$c,description:"Befehl"}}'; }
# Eigenes Format der Copilot CLI (.github/hooks/flow.json, notes.md «Interaktive CLI lädt keine Claude-Hooks»):
# toolName, toolArgs, sessionId, toolCallId, ohne tool_name
own_payload() { own_tool bash "$(jq -cn --arg c "$1" '{command:$c,description:"Befehl"}')"; }
own_tool() { jq -cn --arg n "$1" --argjson a "$2" --arg cwd "$root" '{sessionId:"c1",timestamp:1793955600000,cwd:$cwd,toolCallId:"call_1",toolName:$n,toolArgs:$a}'; }

# Ein Lauf: erwarteter Exit; bei Exit 2 JSON mit permissionDecision deny und dem Grund von stderr, bei Exit 0 keine
# Ablehnung auf stdout (Copilot lehnt auch bei Exit 0 ab, wenn das JSON deny sagt).
check() {
  local want="$1" label="$2" payload="$3" o e got out err
  o=$(mktemp) e=$(mktemp)
  printf '%s' "$payload" | (cd "$root" && bash "$G") >"$o" 2>"$e"
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
# Claude-Payload: nur der Exit-Code (Claude Code liest stdout bei Exit 2 nicht); Copilot-Payload: dazu das JSON.
t() {
  local want="$1" cmd="$2" got
  claude_payload "$cmd" | (cd "$root" && bash "$G") >/dev/null 2>&1
  got=$?
  if [ "$got" = "$want" ]; then echo "ok   $got $cmd"; else echo "FAIL $got (erwartet $want) $cmd"; fails=$((fails+1)); fi
  check "$want" "copilot: $cmd" "$(copilot_payload "$cmd")"
  check "$want" "copilot eigen: $cmd" "$(own_payload "$cmd")"
}

t 2 'cat .env'
t 0 'git ls-files .env'
t 0 "grep -rn --exclude='.env*' TOKEN ."

# Bash ohne erkennbaren Befehl: fail closed (Plan, Architektur-Entscheide)
check 2 'claude: Bash ohne Befehl' '{"tool_name":"Bash","tool_input":{"description":"Befehl"}}'
check 2 'copilot: Bash ohne Befehl' \
  "$(jq -cn --arg cwd "$root" '{hook_event_name:"PreToolUse",session_id:"c1",timestamp:1793955600000,cwd:$cwd,tool_use_id:"call_1",tool_name:"Bash",tool_input:{description:"Befehl"}}')"

# Lese- und Suchwerkzeuge (Plan #24, Entscheid 2026-11-07, Variante A): abgelehnt, wenn ein Pfad- oder Musterfeld als
# letzten Pfadteil die .env oder eine Variante mit Punkt-Endung nennt; bei Grep ist pattern der Suchtext, kein Pfad.
# Claude-Form: Read mit file_path, Grep mit path, Glob mit pattern und path. Copilot-Form (notes.md, «Lese- und
# Suchwerkzeuge unter Copilot»): Read mit path und view_range, Grep mit paths, mit timestamp, ohne transcript_path.
claude_tool() { jq -cn --arg n "$1" --argjson i "$2" --arg cwd "$root" '{session_id:"s1",transcript_path:"/tmp/t.jsonl",cwd:$cwd,hook_event_name:"PreToolUse",tool_name:$n,tool_input:$i}'; }
copilot_tool() { jq -cn --arg n "$1" --argjson i "$2" --arg cwd "$root" '{hook_event_name:"PreToolUse",session_id:"c1",timestamp:1793955600000,cwd:$cwd,tool_use_id:"call_1",tool_name:$n,tool_input:$i}'; }
# Read
check 2 'claude: Read file_path absolut .env' "$(claude_tool Read "$(jq -cn --arg p "$root/.env" '{file_path:$p}')")"
check 2 'claude: Read file_path relativ .env' "$(claude_tool Read '{"file_path":".env"}')"
check 2 'claude: Read file_path .env.local' "$(claude_tool Read "$(jq -cn --arg p "$root/.env.local" '{file_path:$p}')")"
check 0 'claude: Read README.md' "$(claude_tool Read "$(jq -cn --arg p "$root/README.md" '{file_path:$p}')")"
check 0 'claude: Read env.example' "$(claude_tool Read "$(jq -cn --arg p "$root/env.example" '{file_path:$p}')")"
check 0 'claude: Read ohne Pfad' "$(claude_tool Read '{}')"
check 2 'copilot: Read path absolut .env' "$(copilot_tool Read "$(jq -cn --arg p "$root/.env" '{path:$p,view_range:[1,-1]}')")"
check 2 'copilot: Read path relativ .env' "$(copilot_tool Read '{"path":".env","view_range":[1,-1]}')"
check 2 'copilot: Read path .env.local' "$(copilot_tool Read "$(jq -cn --arg p "$root/.env.local" '{path:$p,view_range:[1,-1]}')")"
check 0 'copilot: Read README.md' "$(copilot_tool Read "$(jq -cn --arg p "$root/README.md" '{path:$p,view_range:[1,-1]}')")"
check 0 'copilot: Read env.example' "$(copilot_tool Read "$(jq -cn --arg p "$root/env.example" '{path:$p,view_range:[1,-1]}')")"
check 0 'copilot: Read ohne Pfad' "$(copilot_tool Read '{"view_range":[1,-1]}')"
# Grep: Pfad auf die Datei abgelehnt, auf einen Ordner erlaubt; der Suchtext .env in pattern ist kein Pfad
check 2 'claude: Grep path .env' "$(claude_tool Grep "$(jq -cn --arg p "$root/.env" '{pattern:"TOKEN",path:$p}')")"
check 0 'claude: Grep path docs' "$(claude_tool Grep "$(jq -cn --arg p "$root/docs" '{pattern:"TOKEN",path:$p}')")"
check 0 'claude: Grep pattern .env über docs' "$(claude_tool Grep "$(jq -cn --arg p "$root/docs" '{pattern:".env",path:$p}')")"
check 2 'claude: Grep glob .env über Wurzel' "$(claude_tool Grep "$(jq -cn --arg p "$root" '{pattern:"TOKEN",path:$p,glob:".env"}')")"
check 2 'copilot: Grep paths .env (String)' "$(copilot_tool Grep "$(jq -cn --arg p "$root/.env" '{pattern:"TOKEN",paths:$p,output_mode:"content"}')")"
check 2 'copilot: Grep paths mit .env (Liste)' "$(copilot_tool Grep "$(jq -cn --arg d "$root/docs" --arg p "$root/.env" '{pattern:"TOKEN",paths:[$d,$p],output_mode:"content"}')")"
check 0 'copilot: Grep paths docs' "$(copilot_tool Grep "$(jq -cn --arg p "$root/docs" '{pattern:"TOKEN",paths:$p,output_mode:"content"}')")"
check 0 'copilot: Grep pattern .env über docs' "$(copilot_tool Grep "$(jq -cn --arg p "$root/docs" '{pattern:".env",paths:$p,output_mode:"content"}')")"
# Glob: pattern ist hier ein Pfadmuster. Copilot-Form ohne Messung eines Glob-Werkzeugs: Copilot-Hülle, Claude-Felder.
check 2 'claude: Glob pattern .env' "$(claude_tool Glob '{"pattern":".env"}')"
check 2 'claude: Glob pattern **/.env mit path' "$(claude_tool Glob "$(jq -cn --arg p "$root" '{pattern:"**/.env",path:$p}')")"
check 0 'claude: Glob pattern **/*.md mit path' "$(claude_tool Glob "$(jq -cn --arg p "$root" '{pattern:"**/*.md",path:$p}')")"
check 2 'copilot: Glob pattern **/.env.local' "$(copilot_tool Glob "$(jq -cn --arg p "$root" '{pattern:"**/.env.local",path:$p}')")"
check 0 'copilot: Glob pattern **/*.md' "$(copilot_tool Glob "$(jq -cn --arg p "$root" '{pattern:"**/*.md",path:$p}')")"

# Sicherheits-Review, zweite Runde: Ein glob-Feld in Grep übersteuert in ripgrep .gitignore und versteckte Dateien,
# ein Muster mit Platzhalter, das auf den Namen passt, liest also den Inhalt
grep_glob() { claude_tool Grep "$(jq -cn --arg p "$root" --arg g "$1" '{pattern:"TOKEN",path:$p,glob:$g}')"; }
check 2 'claude: Grep glob .env* über Wurzel' "$(grep_glob '.env*')"
check 2 'claude: Grep glob .en[v] über Wurzel' "$(grep_glob '.en[v]')"
check 2 'claude: Grep glob .e?v über Wurzel' "$(grep_glob '.e?v')"
check 2 'claude: Grep glob {.env,x} über Wurzel' "$(grep_glob '{.env,x}')"
check 2 'claude: Grep glob **/.env.* über Wurzel' "$(grep_glob '**/.env.*')"
check 2 'claude: Grep glob .* über Wurzel' "$(grep_glob '.*')"
check 0 'claude: Grep glob *.md über Wurzel' "$(grep_glob '*.md')"
check 0 'claude: Grep glob **/*.{ts,html} über Wurzel' "$(grep_glob '**/*.{ts,html}')"
check 0 'claude: Grep glob .github/**/*.yml über Wurzel' "$(grep_glob '.github/**/*.yml')"
# Gross-/Kleinschreibung (macOS mit APFS ohne Unterscheidung liest dieselbe Datei)
check 2 'claude: Read .ENV' "$(claude_tool Read "$(jq -cn --arg p "$root/.ENV" '{file_path:$p}')")"
# Weitere Pfadfelder
check 2 'claude: Read notebook_path .env' "$(claude_tool Read "$(jq -cn --arg p "$root/.env" '{notebook_path:$p}')")"
check 2 'copilot: Read filePath .env' "$(copilot_tool Read "$(jq -cn --arg p "$root/.env" '{filePath:$p}')")"
# tool_input, das kein Objekt ist, lässt der Wächter nicht durch (fail closed wie im Bash-Zweig)
check 2 'copilot: Read mit tool_input als String' "$(jq -cn --arg p "$root/.env" '{tool_name:"Read",tool_input:$p}')"

# Eigenes Format der Copilot CLI (Plan #24, «Nachtrag: interaktive CLI»): view → Read, rg → Grep, glob → Glob werden
# geprüft; edit und apply_patch (→ Edit) gehen durch, auch auf die .env (jedes andere Werkzeug geht durch)
check 2 'copilot eigen: bash ohne Befehl' "$(own_tool bash '{"description":"Befehl"}')"
check 2 'copilot eigen: view absolut .env' "$(own_tool view "$(jq -cn --arg p "$root/.env" '{path:$p,view_range:[1,200]}')")"
check 2 'copilot eigen: view relativ .env' "$(own_tool view '{"path":".env"}')"
check 2 'copilot eigen: view .env.local' "$(own_tool view "$(jq -cn --arg p "$root/.env.local" '{path:$p}')")"
check 0 'copilot eigen: view README.md' "$(own_tool view "$(jq -cn --arg p "$root/README.md" '{path:$p,view_range:[1,200]}')")"
check 2 'copilot eigen: rg paths .env' "$(own_tool rg "$(jq -cn --arg p "$root/.env" '{pattern:"TOKEN",paths:$p}')")"
check 2 'copilot eigen: rg paths .env relativ' "$(own_tool rg '{"pattern":"TOKEN","paths":".env"}')"
check 0 'copilot eigen: rg paths docs relativ' "$(own_tool rg '{"pattern":"TOKEN","paths":"docs"}')"
check 0 'copilot eigen: rg pattern .env über docs' "$(own_tool rg '{"pattern":".env","paths":"docs"}')"
check 2 'copilot eigen: glob pattern .env' "$(own_tool glob '{"pattern":".env"}')"
check 2 'copilot eigen: glob pattern **/.env' "$(own_tool glob '{"pattern":"**/.env"}')"
check 0 'copilot eigen: glob pattern docs/**/*.md' "$(own_tool glob '{"pattern":"docs/**/*.md"}')"
check 0 'copilot eigen: edit README.md' "$(own_tool edit "$(jq -cn --arg p "$root/README.md" '{path:$p,old_str:"x",new_str:"y"}')")"
check 0 'copilot eigen: edit .env' "$(own_tool edit "$(jq -cn --arg p "$root/.env" '{path:$p,old_str:"x",new_str:"y"}')")"
check 0 'copilot eigen: apply_patch .env' \
  "$(own_tool apply_patch "$(jq -cn --arg p "$root/.env" '"*** Begin Patch\n*** Update File: " + $p + "\n@@\n-A=1\n+A=2\n*** End Patch\n"')")"
check 0 'copilot eigen: apply_patch docs' \
  "$(own_tool apply_patch "$(jq -cn --arg p "$root/docs/arc42.md" '"*** Begin Patch\n*** Update File: " + $p + "\n@@\n-alt\n+neu\n*** End Patch\n"')")"

[ "$fails" = 0 ] || { echo "[ERROR] guard-env: $fails Fälle falsch"; exit 1; }
echo "guard-env: alle Fälle ok"
