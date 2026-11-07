#!/usr/bin/env bash
# Meldet abgeschwächte Tests: weniger Assertions oder Testfälle, neue Abschaltungen, gelöschte Testdateien und
# geänderte Vorlagen. Verglichen wird mit der Abzweigung vom Hauptzweig, damit auch eine schon committete
# Abschwächung auffällt. Erkennt JUnit, jqwik, Jest/Vitest/Jasmine, pytest, Go und Gherkin (`.feature`, Englisch und
# Deutsch): dort zählen Szenarien, Dann-Schritte, Tabellenzeilen und Tags wie `@Pending` oder `@ignore`.
#
# Zwei Arten des Aufrufs:
#   - PostToolUse-Hook (Edit|Write|MultiEdit): JSON auf stdin, prüft die eine Datei, nur mit `test-guard: true` in
#     der flow.md. Meldung über flow_hint an Claude Code bzw. Copilot; die Änderung bleibt bestehen. Dieselbe Meldung zur
#     selben Datei kommt pro Session nur einmal
#   - Von Hand für den Handoff: `check-test-edit.sh --all` prüft alle geänderten Testdateien des Branches, auch
#     solche, die per Bash geändert oder gelöscht wurden. Exit 1, wenn es etwas zu begründen gibt
#
# Eine Meldung heisst nicht, dass die Änderung falsch ist: Sie verlangt eine Begründung im Handoff
# (`/flow-implement` Schritt 12), oder der Code wird geändert statt des Tests.
set -u
. "$(dirname "$0")/lib.sh"

root=$(flow_root)
cd "$root" || exit 0
if [ "${1:-}" != "--all" ]; then
  flow_require "$root" test-guard true
fi

main_branch=$(flow_base_branch "$root")
if ! base=$(git merge-base HEAD "origin/$main_branch" 2>/dev/null); then
  # Ohne den Hauptzweig von origin nur gegen HEAD: Schon committete Abschwächungen bleiben dann unsichtbar.
  base=$(git rev-parse HEAD 2>/dev/null) || exit 0
  [ "${1:-}" = "--all" ] && echo "Hinweis: origin/$main_branch fehlt, verglichen wird nur mit HEAD." >&2
fi

assertions='\b(assert[A-Za-z_]*|fail|expect)\(|\bassert[[:space:]]'
tests='@([A-Za-z.]*\.)?(Test|ParameterizedTest|RepeatedTest|TestFactory|Property)\b|(^|[^A-Za-z_.])(it|test)\(|\bdef test_|\bfunc Test'
switched_off='@([A-Za-z.]*\.)?Disabled|\bassume(True|False|That)\(|Assumptions\.|\.(skip|todo)\(|\bx(it|describe)\(|pytest\.mark\.skip|\bt\.Skip\('

# Gherkin: nur Zeilen ohne `#` davor zählen, ein auskommentiertes Szenario fällt also weg.
gherkin_scenarios='^[[:space:]]*(Scenario|Scenario Outline|Scenario Template|Example|Szenario|Szenariogrundriss|Szenariovorlage|Szenarien|Beispiel):'
gherkin_then='^[[:space:]]*(Then|Dann)[[:space:]]'
gherkin_rows='^[[:space:]]*\|'
gherkin_off='@([Ii]gnored?|[Ss]kip(ped)?|[Ww]ip|WIP|[Dd]isabled|[Pp]ending|[Mm]anual)\b'

# Anzahl Treffer eines Musters im Text auf stdin.
count() {
  grep -oE "$1" | wc -l | tr -d ' '
}

# compare <datei> <muster> <name> less|more: meldet, wenn die Anzahl Treffer gegenüber der Abzweigung sinkt (less)
# oder steigt (more).
compare() {
  local before now
  before=$(git show "$base:$1" | count "$2")
  now=$(count "$2" <"$1")
  if [ "$4" = less ] && [ "$now" -lt "$before" ]; then
    echo "$1: $3 $before → $now"
  elif [ "$4" = more ] && [ "$now" -gt "$before" ]; then
    echo "$1: $3 $before → $now"
  fi
  return 0
}

# Prüft eine Datei (Pfad relativ zum Repo) und gibt gefundene Abschwächungen aus.
check_file() {
  local relative="$1"
  flow_is_test "$relative" || return 0
  git cat-file -e "$base:$relative" 2>/dev/null || return 0 # neue Datei: nichts abzuschwächen

  if [ ! -f "$relative" ]; then
    echo "$relative: Testdatei gelöscht"
    return 0
  fi
  case "$relative" in
    */vorlagen/*|*/fixtures/*|*/testdata/*)
      git diff --quiet "$base" -- "$relative" || echo "$relative: Vorlage geändert"
      return 0
      ;;
    *.feature)
      compare "$relative" "$gherkin_scenarios" Szenarien less
      compare "$relative" "$gherkin_then" Dann-Schritte less
      compare "$relative" "$gherkin_rows" Tabellenzeilen less
      compare "$relative" "$gherkin_off" 'Abschaltungen (@Pending, @ignore, @wip…)' more
      return 0
      ;;
    *.java|*.kt|*.ts|*.tsx|*.js|*.jsx|*.mjs|*.py|*.go) ;;
    *) return 0 ;;
  esac

  compare "$relative" "$assertions" Assertions less
  compare "$relative" "$tests" Testfälle less
  compare "$relative" "$switched_off" 'Abschaltungen (@Disabled, skip, assume…)' more
}

if [ "${1:-}" = "--all" ]; then
  findings=""
  while IFS= read -r relative; do
    findings+=$(check_file "$relative")
    [ -n "$findings" ] && findings+=$'\n'
  done < <({ git -c core.quotepath=off diff --name-only "$base"; git -c core.quotepath=off ls-files --deleted; } | sort -u)
  findings=$(printf '%s' "$findings" | sed '/^$/d')
  if [ -n "$findings" ]; then
    echo "Abgeschwächte oder entfernte Tests seit $(git rev-parse --short "$base") – im Handoff begründen:"
    echo "$findings"
    exit 1
  fi
  echo "Keine abgeschwächten Tests seit $(git rev-parse --short "$base")."
  exit 0
fi

flow_need_jq check-test-edit
payload=$(flow_payload)
FLOW_PAYLOAD=$payload
flow_is_edit "$payload" || exit 0
session_id=$(printf '%s' "$payload" | jq -r '.session_id // empty' 2>/dev/null)
findings=""
while IFS= read -r file; do
  case "$file" in
    "$root"/*) ;;
    *) continue ;;
  esac
  found=$(check_file "${file#"$root"/}")
  [ -n "$found" ] && findings+="$found"$'\n'
done < <(flow_tool_files "$payload")
findings=$(printf '%s' "$findings" | sed '/^$/d')
[ -n "$findings" ] || exit 0

# Dieselbe Meldung nur einmal pro Session: Wer begründet umbaut, soll nicht bei jedem Edit erinnert werden.
if [ -n "$session_id" ]; then
  state="${TMPDIR:-/tmp}/flow-test-edit-${session_id//[^A-Za-z0-9-]/}"
  fresh="$findings"
  if [ -s "$state" ]; then
    fresh=$(printf '%s\n' "$findings" | grep -vxF -f "$state")
  fi
  [ -n "$fresh" ] || exit 0
  printf '%s\n' "$fresh" >>"$state"
  findings="$fresh"
fi

flow_hint "Bestehender Test abgeschwächt (Vergleich mit der Abzweigung von $main_branch):
$findings
Ändert sich die Spezifikation, zuerst die Story-Datei; sonst den Code ändern statt den Test.
Bleibt die Änderung, im Handoff begründen (/flow-implement Schritt 12)."
