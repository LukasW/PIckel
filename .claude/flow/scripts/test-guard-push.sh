#!/usr/bin/env bash
# Prüft guard-push.sh gegen bekannte Umgehungen (Projekt-Review 2026-11-07): je Fall erwarteter Exit-Code und Befehl.
# Läuft in scripts/gate.sh und CI, auf jedem Zweig: Fälle, deren Ergebnis vom aktuellen Zweig abhängt (Push ohne
# Refspec, HEAD), laufen in temporären Repositorys mit festgelegtem Zweig.
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

# Sicherheits-Review: Ein geerbtes CLAUDE_PROJECT_DIR eines anderen Projekts darf den Guard unter Copilot nicht
# ausschalten; COPILOT_PROJECT_DIR (von Copilot für die laufende Session gesetzt) geht vor
stale=$(mktemp -d)
printf '{"toolName":"bash","toolArgs":{"command":"git push origin HEAD:main"}}' \
  | (cd "$stale" && CLAUDE_PROJECT_DIR="$stale" COPILOT_PROJECT_DIR="$root" bash "$G") >/dev/null 2>&1
got=$?
if [ "$got" = 2 ]; then echo "ok   2 COPILOT_PROJECT_DIR geht einem fremden CLAUDE_PROJECT_DIR vor"
else echo "FAIL $got (erwartet 2) COPILOT_PROJECT_DIR geht einem fremden CLAUDE_PROJECT_DIR vor"; fails=$((fails+1)); fi
rm -rf "$stale"
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
# Dasselbe auf einem Feature-Zweig: Push ohne Refspec und HEAD sind erlaubt
git -C "$tmp" symbolic-ref HEAD refs/heads/feature/x
t 0 'git push' "$tmp"
t 0 'git push origin HEAD' "$tmp"
rm -rf "$tmp"

# --- Issue #26: Umgehungen über die Schreibweise (Plan «Test-Strategie») --------------------------------------------
# Ein Segment ist ein Push, wenn ein Wort mit Basisname git und danach ein Wort push vorkommt; geprüft wird auf dem
# normalisierten Befehl (flow_shell_segments)
t 2 'git -c k=v push origin main'
t 2 'git --no-pager push origin main'
t 2 '"git" push origin main'
t 2 $'git push origin \\\nHEAD:main'
t 2 '/usr/bin/git push origin main'
t 2 'git -C . -c a=b --no-pager push origin HEAD:main'
t 2 'sh -c "git push origin main"'
# Bewusster Fehlalarm (Entscheid 4, fail closed): Text in Anführungszeichen gilt als Befehl; Ausweg git commit -F
t 2 'git commit -m "push origin main"'
# Erlaubt: Push aus dem Devcontainer mit gh als Credential-Helper, globale Option ohne Push, Glob mit Ziel ausserhalb
# von main
t 0 "git -c credential.helper= -c 'credential.https://github.com.helper=!gh auth git-credential' push -u origin feature/x"
t 0 'git --no-pager log'
t 0 "git push origin 'refs/tags/*'"
t 0 "git push origin 'refs/heads/feature/*:refs/heads/feature/*'"

# Zusätze (Entscheid 1): temporäres Repository mit origin, aktueller Zweig ein Feature-Zweig
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
g() { git -C "$tmp/work" -c user.name=t -c user.email=t@example.com -c init.defaultBranch=main "$@" >/dev/null 2>&1; }
git init -q --bare -b main "$tmp/origin.git"
git init -q -b main "$tmp/work"
mkdir -p "$tmp/work/.claude" && cp "$root/.claude/flow.md" "$tmp/work/.claude/"
g commit -q --allow-empty -m start
g remote add origin "$tmp/origin.git"
g push -q origin main main:refs/heads/feature/x
g fetch -q origin
g checkout -q -b feature/y
if [ "$(git -C "$tmp/work" symbolic-ref --short HEAD)" != feature/y ] \
  || ! git -C "$tmp/work" rev-parse --verify -q refs/remotes/origin/main >/dev/null; then
  echo "FAIL Aufbau des temporären Repositorys"; fails=$((fails+1))
fi
# --all und --branches schieben auch main; ein Glob-Refspec mit Ziel refs/heads/* trifft main
t 2 'git push --all origin' "$tmp/work"
t 2 'git push --branches origin' "$tmp/work"
t 2 "git push origin 'refs/heads/*:refs/heads/*'" "$tmp/work"
# Feature-Zweig ohne Upstream: Push ohne Refspec bleibt erlaubt
t 0 'git push' "$tmp/work"
t 0 'git push origin' "$tmp/work"
# Upstream origin/main mit push.default=upstream: @{push} ist main
g config push.default upstream
g branch -q --set-upstream-to=origin/main
t 2 'git push' "$tmp/work"
t 2 'git push origin' "$tmp/work"
# Gleich, nur der Upstream ist ein Feature-Zweig: @{push} ist nicht main
g branch -q --set-upstream-to=origin/feature/x
t 0 'git push' "$tmp/work"
rm -rf "$tmp"
trap - EXIT

# --- Issue #26, Nachtrag Reviewer-Runde (Entscheid 5) ---------------------------------------------------------------
# Unabhängig vom aktuellen Zweig: Umleitung am Wort, gebündelte Kurzoptionen, abgekürzte Langoptionen, Refspec «:»,
# Ziel heads/main, git-push als Befehl, Glob-Ziel ohne refs/heads/
t 2 'git push origin main>/dev/null'
t 2 'git push -fu origin feature/x'
t 2 'git push -uf origin feature/x'
t 2 'git push -ud origin feature/x'
t 2 'git push --del origin feature/x'
t 2 'git push --force-w origin feature/x'
t 2 'git push --al origin'
t 2 'git push --mirr origin'
t 2 'git push --bra origin'
t 2 'git push --force-i origin feature/x'
t 2 'git push --force-w=feature/x origin feature/x'
t 2 'git push origin :'
t 2 'git push origin heads/main'
t 2 'git push origin feature/x:heads/main'
t 2 'git-push origin main'
t 2 '/usr/lib/git-core/git-push origin main'
t 2 "git push origin 'm*:m*'"
# Gegenstücke: Kurz- und Langoptionen, die kein Präfix einer verbotenen sind, Ziele ausserhalb von main
t 0 'git push -uv origin feature/x'
t 0 'git push --dry-run origin feature/x'
t 0 'git push --verbose origin feature/x'
t 0 'git push --set-upstream origin feature/x'
t 0 'git push origin heads/feature/x'
t 0 'git-push origin feature/x'
t 0 "git push origin 'f*:f*'"

# Temporäres Repository: origin mit main, feature/x und feature/main; zuerst auf main, dann auf einem Feature-Zweig
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
git init -q --bare -b main "$tmp/origin.git"
git init -q -b main "$tmp/work"
mkdir -p "$tmp/work/.claude" && cp "$root/.claude/flow.md" "$tmp/work/.claude/"
g commit -q --allow-empty -m start
g remote add origin "$tmp/origin.git"
g push -q origin main main:refs/heads/feature/x main:refs/heads/feature/main
g fetch -q origin
if [ "$(git -C "$tmp/work" symbolic-ref --short HEAD)" != main ] \
  || ! git -C "$tmp/work" rev-parse --verify -q refs/remotes/origin/feature/main >/dev/null; then
  echo "FAIL Aufbau des temporären Repositorys (Nachtrag)"; fails=$((fails+1))
fi
# Auf main: @ gilt wie HEAD, ein Refspec mit $ (Rest von $(…) oder einer Variablen) ebenso
t 2 'git push origin @' "$tmp/work"
t 2 'git push origin $(git branch --show-current)' "$tmp/work"
t 2 'git push -u origin "$(git rev-parse --abbrev-ref HEAD)"' "$tmp/work"
t 2 'git push origin $BRANCH' "$tmp/work"
# git stash push ist kein Push, auch mit --all und auf main
t 0 'git stash push' "$tmp/work"
t 0 'git stash push --all' "$tmp/work"
t 0 'git stash push -m wip' "$tmp/work"
# Bewusster Fehlalarm (fail closed): «push» als Wort nach git gilt als Push ohne Refspec auf main
t 2 'git log --grep push' "$tmp/work"
# Auf einem Feature-Zweig: dieselben Refspecs erlaubt
g checkout -q -b feature/y
t 0 'git push origin @' "$tmp/work"
t 0 'git push origin $(git branch --show-current)' "$tmp/work"
t 0 'git push -u origin "$(git rev-parse --abbrev-ref HEAD)"' "$tmp/work"
t 0 'git log --grep push' "$tmp/work"
# @{push} trifft nur refs/remotes/<remote>/main: Upstream origin/feature/main ist kein Push auf main
g config push.default upstream
g branch -q --set-upstream-to=origin/feature/main
t 0 'git push' "$tmp/work"
t 0 'git push origin' "$tmp/work"
rm -rf "$tmp"
trap - EXIT

[ "$fails" = 0 ] || { echo "[ERROR] guard-push: $fails Fälle falsch"; exit 1; }
echo "guard-push: alle Fälle ok"
