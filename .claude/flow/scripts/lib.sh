#!/usr/bin/env bash
# Gemeinsame Funktionen der Hooks von flow. Ein Repository ohne .claude/flow.md bleibt unberührt: Jeder Hook
# endet dort sofort mit Exit 0.

# Projektordner: COPILOT_PROJECT_DIR (setzt die Copilot CLI für die laufende Session) vor CLAUDE_PROJECT_DIR, das in
# einem VS-Code-Terminal auch geerbt und von einem anderen Projekt sein kann; sonst das Git-Repository.
flow_root() {
  if [ -n "${COPILOT_PROJECT_DIR:-}" ]; then
    printf '%s' "$COPILOT_PROJECT_DIR"
  elif [ -n "${CLAUDE_PROJECT_DIR:-}" ]; then
    printf '%s' "$CLAUDE_PROJECT_DIR"
  else
    git rev-parse --show-toplevel 2>/dev/null || pwd
  fi
}

# Wert eines Schalters aus dem YAML-Kopf der flow.md (nur `schluessel: wert` auf einer Zeile, Kommentare erlaubt).
flow_option() {
  local file="$1/.claude/flow.md" key="$2"
  [ -f "$file" ] || return 1
  awk -v key="$key" '
    NR == 1 && $0 != "---" { exit }
    NR > 1 && $0 == "---" { exit }
    NR > 1 {
      line = $0
      sub(/[[:space:]]+#.*$/, "", line)
      if (index(line, key ":") == 1) {
        value = substr(line, length(key) + 2)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        print value
        exit
      }
    }' "$file"
}

# Endet den Hook, wenn das Repository flow nicht nutzt oder der Schalter nicht den erwarteten Wert hat.
flow_require() {
  local root="$1" key="$2" expected="$3"
  [ -f "$root/.claude/flow.md" ] || exit 0
  [ "$(flow_option "$root" "$key")" = "$expected" ] || exit 0
}

# Hauptzweig: base-branch aus flow.md, sonst der Standardzweig von origin, sonst main.
flow_base_branch() {
  local root="$1" branch
  branch=$(flow_option "$root" base-branch)
  if [ -z "$branch" ]; then
    branch=$(git -C "$root" symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null)
    branch="${branch#origin/}"
  fi
  printf '%s' "${branch:-main}"
}

# Ist der Pfad (relativ zum Repository) eine Testdatei? Testordner und die üblichen Namen von JUnit, Jest/Vitest,
# pytest und Go, dazu Gherkin-Dateien (`.feature`) und Ordner `e2e/` überall.
flow_is_test() {
  case "$1" in
    */src/test/*|test/*|*/test/*|tests/*|*/tests/*|*/__tests__/*|e2e/*|*/e2e/*|*.feature) return 0 ;;
    *Test.java|*Tests.java|*IT.java|*Test.kt|*.test.*|*.spec.*|*_test.*|test_*.py|*/test_*.py) return 0 ;;
  esac
  return 1
}

# Merge-Base des Branches mit origin/<hauptzweig>; ohne den Hauptzweig von origin HEAD, mit Hinweis auf stderr.
flow_merge_base() {
  local main_branch="$1"
  git merge-base HEAD "origin/$main_branch" 2>/dev/null && return 0
  echo "Hinweis: origin/$main_branch fehlt, verglichen wird nur mit HEAD." >&2
  git rev-parse HEAD 2>/dev/null
}

# Ohne jq kann ein Hook die Eingabe nicht lesen. Wächter dürfen dann nicht still durchlassen (fail-closed):
# Exit 2 hält den Befehl an bzw. meldet es Claude, bis jq installiert ist.
flow_need_jq() {
  command -v jq >/dev/null 2>&1 && return 0
  echo "flow: jq fehlt, $1 kann nicht prüfen und hält deshalb an. Installieren: brew install jq bzw. sudo apt-get install -y jq" >&2
  exit 2
}

# Eingabe und Ausgabe der Hooks für Claude Code und die Copilot CLI (ADR 0007). Copilot übersetzt seine Eingabe ins
# Format von Claude Code, liefert Edits aber je nach Modell als Patch-Text in `tool_input` statt mit `file_path`, und
# es zeigt nur, was als JSON auf stdout steht; Claude Code liest bei Exit 2 nur stderr.

# Befehl eines Terminal-Aufrufs aus der Eingabe ($1). Exit 1, wenn keiner erkennbar ist: Ein Wächter lehnt dann ab.
flow_tool_command() {
  local cmd
  cmd=$(printf '%s' "$1" | jq -r 'if (.tool_input | type) == "object" then .tool_input.command // empty else empty end' 2>/dev/null) || return 1
  [ -n "$cmd" ] || return 1
  printf '%s\n' "$cmd"
}

# Absolute Pfade der Dateien, die ein Edit aus der Eingabe ($1) berührt, einer pro Zeile und ohne Dubletten; leer,
# wenn keiner erkennbar ist. Quellen: tool_input.file_path, tool_input.path, tool_response.filePath und die Kopfzeilen
# eines Patches (`*** Update File:`, `*** Add File:`, Ziel von `*** Move to:`; gelöschte Dateien nicht; CR und
# Leerzeichen am Zeilenende zählen nicht). Relative Pfade (interaktive Copilot CLI) gelten relativ zu cwd der Eingabe,
# ohne cwd zum aktuellen Verzeichnis. Endet immer mit 0: Ein Hinweis-Hook ohne Pfad endet still.
flow_tool_files() {
  printf '%s' "$1" | jq -r --arg pwd "$PWD" '
    (.cwd // $pwd | sub("/+$"; "")) as $base
    | [ (if (.tool_input | type) == "object" then (.tool_input.file_path, .tool_input.path) else empty end),
      (if (.tool_response | type) == "object" then .tool_response.filePath else empty end),
      (if (.tool_input | type) == "string" then
         .tool_input | split("\n")[] | sub("[[:space:]]+$"; "")
         | capture("^\\*\\*\\* ((Update|Add) File|Move to): (?<p>.+)$").p
       else empty end)
    ] | map(select(type == "string" and . != "")
            | if startswith("/") then . else ($base + "/" + sub("^\\./"; "")) end) | unique[]' 2>/dev/null
  return 0
}

# Lehnt einen Tool-Aufruf mit Grund ($1) ab: stderr für Claude Code, permissionDecision als JSON auf stdout für
# Copilot, Exit 2. Claude Code wertet stdout bei Exit 2 nicht aus.
flow_deny() {
  printf '%s\n' "$1" >&2
  jq -n --arg r "$1" '{permissionDecision: "deny", permissionDecisionReason: $r}'
  exit 2
}

# Meldet dem Modell nach einem Tool-Aufruf einen Hinweis ($1), je Werkzeug-Aufruf und Hook einmal: stderr für Claude Code, additionalContext als JSON auf
# stdout für Copilot (es reicht nur dieses Feld ans Modell weiter, auch bei Exit 2), Exit 2. Die Änderung bleibt.
flow_hint() {
  # Im Prompt-Modus laufen die Hooks aus .claude/settings.json und .github/hooks/flow.json: je Werkzeug-Aufruf und
  # Hook nur einmal melden. Die Hooks legen ihre Eingabe dafür in FLOW_PAYLOAD ab.
  local id state
  id=$(printf '%s' "${FLOW_PAYLOAD:-}" | jq -r '.tool_use_id // empty' 2>/dev/null)
  if [ -n "$id" ]; then
    state="${TMPDIR:-/tmp}/flow-hint-$(basename "$0" .sh)-${id//[^A-Za-z0-9_-]/}"
    [ -e "$state" ] && exit 0
    : >"$state"
  fi
  printf '%s\n' "$1" >&2
  jq -n --arg c "$1" '{additionalContext: $c}'
  exit 2
}

# Liest die Eingabe des Hooks von stdin und gibt sie im Format von Claude Code aus. Die interaktive Copilot CLI ruft
# die Hooks über .github/hooks/flow.json mit ihrem eigenen Format auf (toolName, toolArgs, toolResult, sessionId,
# toolCallId); das wird übersetzt, die Namen der Werkzeuge eingeschlossen (bash → Bash, view → Read, rg/grep → Grep,
# glob → Glob, edit/create/apply_patch → Edit). Claude-Format und unlesbare Eingaben bleiben unverändert.
flow_payload() {
  local raw out
  raw=$(cat)
  out=$(printf '%s' "$raw" | jq -c '
    if type == "object" and has("toolName") and (has("tool_name") | not) then
      { tool_name: ({bash: "Bash", view: "Read", rg: "Grep", grep: "Grep", glob: "Glob",
                     edit: "Edit", create: "Edit", apply_patch: "Edit"}[.toolName] // .toolName),
        tool_input: .toolArgs }
      + (if has("toolResult") then {tool_response: .toolResult} else {} end)
      + (if has("sessionId") then {session_id: .sessionId} else {} end)
      + (if has("toolCallId") then {tool_use_id: .toolCallId} else {} end)
      + (if has("cwd") then {cwd: .cwd} else {} end)
    else . end' 2>/dev/null) || out=$raw
  printf '%s' "${out:-$raw}"
}

# Ist der Aufruf ein Edit? Hinweis-Hooks reagieren nur darauf (.github/hooks/flow.json kennt keinen Matcher). Ohne
# tool_name gilt die Eingabe als Edit, damit Claude-Eingaben ohne das Feld nicht verstummen.
flow_is_edit() {
  case "$(printf '%s' "$1" | jq -r '.tool_name // "Edit"' 2>/dev/null)" in
    Edit|Write|MultiEdit|NotebookEdit) return 0 ;;
  esac
  return 1
}
