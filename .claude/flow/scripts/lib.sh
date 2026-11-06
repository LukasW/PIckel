#!/usr/bin/env bash
# Gemeinsame Funktionen der Hooks von flow. Ein Repository ohne .claude/flow.md bleibt unberührt: Jeder Hook
# endet dort sofort mit Exit 0.

flow_root() {
  if [ -n "${CLAUDE_PROJECT_DIR:-}" ]; then
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
