#!/usr/bin/env bash
# Nachtlauf von flow: setzt die Issues einer Batch-Datei unbeaufsichtigt um. Je Issue ein eigener Worktree und ein
# frischer `claude -p` mit `/flow-implement <nr> --nachtlauf`, danach `/flow-ship --nachtlauf`. Die Wellen laufen
# nacheinander; ein Issue startet nur, wenn seine Vorgänger auf dem Hauptzweig liegen. Erfolg heisst: Der
# Squash-Commit des Issues liegt auf origin/<hauptzweig>, nicht bloss Exit 0. Ein zweiter Lauf derselben Datei setzt
# dort fort, wo der erste stand. Format der Batch-Datei und Anleitung: docs/nachtlauf.md im Repository claude-flow.
#
# Bash 3.2 (macOS): keine assoziativen Arrays, kein `wait -n`, leere Arrays nie ungeschützt unter `set -u`.
set -uo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib.sh
. "$here/lib.sh"

NIGHT_DIR=${FLOW_NIGHT_DIR:-$HOME/night-batch}
CLAUDE_BIN=${FLOW_CLAUDE:-claude}
# Kein --dangerously-skip-permissions: Nachts gelten die Rechte aus .claude/settings.json (allow/deny), Edits sind
# erlaubt, alles andere wird abgelehnt statt erfragt. Dazu ein Kostendeckel je Phase gegen Endlosschleifen.
MAX_BUDGET=${FLOW_MAX_BUDGET_USD:-20}
# Absichtlich ohne Doppelpunkt: Ein leerer Wert schaltet die Voreinstellung ab
CLAUDE_ARGS=${FLOW_CLAUDE_ARGS---permission-mode acceptEdits --max-budget-usd $MAX_BUDGET}
MAX_ATTEMPTS=${FLOW_MAX_ATTEMPTS:-3}
POLL=${FLOW_POLL_SECONDS:-10}
# Kurzzeitige API-Fehler: Die letzte Anfrage stirbt nach Stunden Arbeit, claude -p endet mit Fehler. Nur diese
# Läufe werden wiederholt, echte Fehler nicht.
TRANSIENT='Request timed out|socket connection was closed|connection error|API Error|overloaded|rate.?limit|"(status|http_status)":(429|500|503|529)'
TAB=$(printf '\t')

usage() {
  cat <<'EOF'
Aufruf:
  night-batch.sh <batch-datei>             Lauf im Vordergrund (etwa in tmux)
  night-batch.sh --start <batch-datei>     prüfen, dann im Hintergrund starten
  night-batch.sh --dry-run <batch-datei>   nur prüfen und den Plan zeigen
  night-batch.sh --status [<batch|repo>]   Stand des letzten Laufs (ohne Argument: Repository im aktuellen Ordner)
  night-batch.sh --stop [<batch|repo>]     laufenden Lauf beenden

Umgebung: FLOW_NIGHT_DIR (Ablage, ~/night-batch), FLOW_CLAUDE (claude), FLOW_CLAUDE_ARGS
(--permission-mode acceptEdits --max-budget-usd <FLOW_MAX_BUDGET_USD>), FLOW_MAX_BUDGET_USD (20), FLOW_MAX_ATTEMPTS (3), FLOW_POLL_SECONDS (10), FLOW_NO_CAFFEINATE=1
EOF
}

ts() { date +'%F %T'; }
die() { echo "night-batch: $*" >&2; exit 2; }
is_num() { case "$1" in '' | *[!0-9]*) return 1 ;; *) return 0 ;; esac; }
trim() {
  local s=$1
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

# --- Batch-Datei --------------------------------------------------------------------------------------------------

B_REPO="" B_EPIC="" B_POOL="" B_MERGE="" B_HINT=""
B_PRE=() B_WARM=() B_MANUAL=()
I_NR=() I_DEPS=() I_WAVE=() W_TITLE=()

# <nr>[: <vorgänger…>][  # bemerkung]. Vorgänger dürfen ein # tragen; eine Bemerkung beginnt mit «# ».
parse_issue_line() {
  local line=$1 n=$2 nr rest deps="" tok t comment=0 toks=()
  nr=${line%%[!0-9]*}
  rest=$(trim "${line#"$nr"}")
  if [ "${rest#:}" != "$rest" ]; then
    read -r -a toks <<<"${rest#:}"
    for tok in ${toks[@]+"${toks[@]}"}; do
      [ "$comment" -eq 1 ] && break
      t=${tok#\#}
      if is_num "$t"; then
        deps="$deps $t"
      elif [ "${tok#\#}" != "$tok" ]; then
        comment=1
      else
        die "Zeile $n: «${tok}» ist keine Issue-Nummer (eine Bemerkung beginnt mit «# »)"
      fi
    done
  elif [ -n "$rest" ]; then
    case "$rest" in
      \#[0-9]*) die "Zeile $n: Vorgänger stehen nach einem Doppelpunkt: «$nr: ${rest}»" ;;
      \#*) ;;
      *) die "Zeile $n: nach der Issue-Nummer folgt «: <vorgänger>» oder «# <bemerkung>»" ;;
    esac
  fi
  [ ${#W_TITLE[@]} -gt 0 ] || die "Zeile $n: Issue #$nr steht vor der ersten Zeile «welle:»"
  I_NR+=("$nr")
  I_DEPS+=("$(trim "$deps")")
  I_WAVE+=($((${#W_TITLE[@]} - 1)))
}

parse_batch() {
  local file=$1 line key value n=0
  [ -f "$file" ] || die "Batch-Datei fehlt: $file"
  while IFS= read -r line || [ -n "$line" ]; do
    n=$((n + 1))
    line=$(trim "$line")
    case "$line" in
      '' | \#*) continue ;;
      [0-9]*) parse_issue_line "$line" "$n"; continue ;;
    esac
    key=${line%%:*}
    [ "$key" != "$line" ] || die "Zeile $n: erwartet «schlüssel: wert» oder eine Issue-Nummer: $line"
    value=$(trim "${line#*:}")
    case "$key" in
      repo) B_REPO=${value/#\~/$HOME} ;;
      epic) B_EPIC=${value#\#} ;;
      pool) B_POOL=$value ;;
      merge) B_MERGE=$value ;;
      vorbedingung) B_PRE+=("$value") ;;
      warmup) B_WARM+=("$value") ;;
      hinweis) B_HINT="${B_HINT:+$B_HINT }$value" ;;
      von-hand) B_MANUAL+=("$value") ;;
      welle) W_TITLE+=("${value:-Welle $((${#W_TITLE[@]} + 1))}") ;;
      *) die "Zeile $n: unbekannter Schlüssel «${key}»" ;;
    esac
  done <"$file"
}

# Ablage je Repository: <ordner>-<repo>, damit gleiche Issue-Nummern zweier Repositories nicht kollidieren.
state_dir() { printf '%s/%s-%s' "$NIGHT_DIR" "$(basename "$(dirname "$1")")" "$(basename "$1")"; }

BASE="" STATE="" RUN="" MAINLOG="" COPY_DIR=""

load() {
  parse_batch "$1"
  [ -n "$B_REPO" ] || die "«repo:» fehlt in $1"
  [ -d "$B_REPO" ] || die "Repository fehlt: $B_REPO"
  B_REPO=$(git -C "$B_REPO" rev-parse --show-toplevel 2>/dev/null) || die "kein Git-Repository: $B_REPO"
  B_POOL=${B_POOL:-1}
  is_num "$B_POOL" && [ "$B_POOL" -ge 1 ] || die "«pool:» muss eine Zahl ab 1 sein, nicht «${B_POOL}»"
  B_MERGE=${B_MERGE:-nein}
  case "$B_MERGE" in ja | nein) ;; *) die "«merge:» ist «ja» oder «nein», nicht «${B_MERGE}»" ;; esac
  [ ${#I_NR[@]} -gt 0 ] || die "keine Issues in $1"
  BASE=$(flow_base_branch "$B_REPO")
  STATE=$(state_dir "$B_REPO")
  MAINLOG=$(mktemp "${TMPDIR:-/tmp}/night-batch-main.XXXXXX")
}

idx_of() {
  local i
  for i in "${!I_NR[@]}"; do
    [ "${I_NR[$i]}" = "$1" ] && { echo "$i"; return; }
  done
  echo -1
}

# --- Hauptzweig ---------------------------------------------------------------------------------------------------

refresh_main() {
  git -C "$B_REPO" fetch origin --quiet --prune 2>/dev/null || echo "[$(ts)] WARN: git fetch gescheitert" >&2
  # Der Titel jedes Commits beginnt mit \037, damit on_main ihn vom Body unterscheiden kann
  git -C "$B_REPO" log "origin/$BASE" --format='%x1f%s%n%b' >"$MAINLOG" 2>/dev/null || : >"$MAINLOG"
}

# Liegt das Issue auf dem Hauptzweig? /flow-ship setzt «(#<nr>)» in den Titel und «Closes #<nr>» an den Anfang einer
# Zeile im Body. «(#<nr>)» zählt nur im Titel: Ein Body, der ein anderes Issue in Klammern nennt («in a/b geteilt
# (#344)»), macht es nicht erledigt.
on_main() { grep -qE "^$(printf '\037').*\(#$1\)|^[[:space:]]*([Cc]loses|[Ff]ixes|[Rr]esolves) #$1([^0-9]|$)" "$MAINLOG"; }

pr_branch() { git -C "$B_REPO" for-each-ref --format='%(refname:short)' "refs/remotes/origin/feature/$1-*" | head -1; }

blocked_by() {
  local d missing=""
  for d in $(echo "${I_DEPS[$(idx_of "$1")]}"); do
    on_main "$d" || missing="$missing #$d"
  done
  echo "$missing"
}

# --- Prüfung und Plan ---------------------------------------------------------------------------------------------

check_batch() {
  local errors=0 i j k d nr c
  err() { echo "FEHLER  $*"; errors=$((errors + 1)); }
  git -C "$B_REPO" cat-file -e "origin/$BASE:.claude/flow.md" 2>/dev/null \
    || err ".claude/flow.md fehlt auf origin/$BASE; die Worktrees hätten keine Projektangaben"
  command -v "$CLAUDE_BIN" >/dev/null 2>&1 || err "$CLAUDE_BIN nicht im PATH"
  command -v jq >/dev/null 2>&1 || err "jq nicht im PATH (Settings je Phase, Hooks)"
  for i in "${!I_NR[@]}"; do
    nr=${I_NR[$i]}
    for j in "${!I_NR[@]}"; do
      [ "$j" -lt "$i" ] && [ "${I_NR[$j]}" = "$nr" ] && err "#$nr steht zweimal in der Datei"
    done
    for d in $(echo "${I_DEPS[$i]}"); do
      k=$(idx_of "$d")
      if [ "$d" = "$nr" ]; then
        err "#$nr wartet auf sich selbst"
      elif [ "$k" -ge 0 ]; then
        if [ "$B_MERGE" = nein ]; then
          err "#$nr wartet auf #$d, das mit «merge: nein» nachts nie auf $BASE kommt"
        elif [ "${I_WAVE[$k]}" -gt "${I_WAVE[$i]}" ]; then
          err "#$nr wartet auf #$d aus einer späteren Welle"
        elif [ "${I_WAVE[$k]}" -eq "${I_WAVE[$i]}" ]; then
          if [ "$B_POOL" -gt 1 ]; then
            err "#$nr wartet auf #$d in derselben Welle; mit «pool: ${B_POOL}» laufen beide gleichzeitig"
          elif [ "$k" -gt "$i" ]; then
            err "#$nr wartet auf #$d, das in derselben Welle weiter unten steht"
          fi
        fi
      elif ! on_main "$d"; then
        err "#$nr wartet auf #$d, das weder im Lauf steht noch auf origin/$BASE liegt"
      fi
    done
  done
  for c in ${B_PRE[@]+"${B_PRE[@]}"}; do
    (cd "$B_REPO" && bash -c "$c") >/dev/null 2>&1 || err "Vorbedingung scheitert: $c"
  done
  return "$errors"
}

print_plan() {
  local w i nr state c
  echo "Repository: $B_REPO (Hauptzweig $BASE)"
  [ -n "$B_EPIC" ] && echo "Epic:       #$B_EPIC"
  echo "Ablage:     $STATE"
  echo "Pool:       $B_POOL    Merge: $B_MERGE"
  [ -n "$B_HINT" ] && echo "Hinweis:    $B_HINT"
  for c in ${B_MANUAL[@]+"${B_MANUAL[@]}"}; do echo "Von Hand:   $c"; done
  for w in "${!W_TITLE[@]}"; do
    echo
    echo "${W_TITLE[$w]}"
    for i in "${!I_NR[@]}"; do
      [ "${I_WAVE[$i]}" -eq "$w" ] || continue
      nr=${I_NR[$i]}
      if on_main "$nr"; then state="liegt schon auf $BASE"; else state=""; fi
      printf '  #%-6s %-28s %s\n' "$nr" "${I_DEPS[$i]:+nach #${I_DEPS[$i]// / #}}" "$state"
    done
  done
}

# --- Ablage und Stand ---------------------------------------------------------------------------------------------

mlog() { echo "[$(ts)] $*" | tee -a "$STATE/logs/master.log"; }

# status.tsv: <nr> <zustand> <detail> <zeit>; schreibt nur der Hauptprozess.
set_status() {
  local f="$STATE/status.tsv"
  { grep -v "^$1$TAB" "$f" 2>/dev/null; printf '%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$(ts)"; } >"$f.tmp"
  mv "$f.tmp" "$f"
}

running_pid() {
  local pid
  pid=$(cat "$STATE/night-batch.pid" 2>/dev/null) || return 1
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null && echo "$pid"
}

# --- Ein Issue ----------------------------------------------------------------------------------------------------

# Legt den Worktree im Hauptprozess an, nacheinander: So gibt es kein Rennen um den Lock von .git/config.
prepare_worktree() {
  local nr=$1 wt="$STATE/worktrees/$1" log="$STATE/logs/$1.log"
  {
    echo
    echo "------------------------------------------------------------"
    echo "[$(ts)] START #$nr"
    echo "------------------------------------------------------------"
  } >>"$log"
  if [ -e "$wt" ]; then
    git -C "$B_REPO" worktree remove --force "$wt" >>"$log" 2>&1 || rm -rf "$wt"
  fi
  git -C "$B_REPO" worktree prune >>"$log" 2>&1
  git -C "$B_REPO" branch -D "auto/$nr" >>"$log" 2>&1 || true
  git -C "$B_REPO" worktree add -b "auto/$nr" "$wt" "origin/$BASE" >>"$log" 2>&1 || return 1
  git -C "$wt" rev-parse HEAD >"$RUN/base.$nr"
}

cleanup_issue() {
  local nr=$1 b
  git -C "$B_REPO" worktree remove --force "$STATE/worktrees/$nr" >/dev/null 2>&1 || rm -rf "$STATE/worktrees/$nr"
  git -C "$B_REPO" worktree prune >/dev/null 2>&1
  git -C "$B_REPO" branch -D "auto/$nr" >/dev/null 2>&1
  [ "$2" = merged ] || return 0
  for b in $(git -C "$B_REPO" for-each-ref --format='%(refname:short)' "refs/heads/feature/$nr-*"); do
    git -C "$B_REPO" branch -D "$b" >/dev/null 2>&1
  done
}

result() { printf '%s\n%s\n' "$2" "$3" >"$RUN/result.$1"; }

# claude_phase <nr> <phase> <prompt> <reset|keep>: 0 bei Erfolg, sonst steht das Ergebnis in result.<nr>.
claude_phase() {
  local nr=$1 phase=$2 prompt=$3 mode=$4 wt="$STATE/worktrees/$1" log="$STATE/logs/$1.log" attempt=1 rc out last why
  while :; do
    out="$RUN/$nr.$phase.$attempt.jsonl"
    echo "[$(ts)] $phase Versuch $attempt/$MAX_ATTEMPTS #$nr" >>"$log"
    # --output-format stream-json: Das Log wächst live, sonst puffert claude bis zum Ende des Laufs.
    # Nachts zusätzlich gesperrt: Leitplanken, Pipeline und Abhängigkeiten ändern (.claude/settings.night.json,
    # falls das Repository eine hat) und in jeder Phase ausser «ship» mergen. Nur «ship» mit «merge: ja» mergt.
    # Eine Datei je Phase, weil Regeln mit Leerzeichen als Argument von --disallowedTools zerfallen würden.
    local night="$wt/.claude/settings.night.json" settings="$RUN/settings.$nr.$phase.json"
    [ -f "$night" ] || night=<(echo '{}')
    if [ "$phase" = ship ]; then
      jq '.' "$night" >"$settings"
    else
      jq '.permissions.deny = ((.permissions.deny // []) + ["Bash(gh pr merge:*)"])' "$night" >"$settings"
    fi
    # shellcheck disable=SC2086 # CLAUDE_ARGS soll in Wörter zerfallen
    (cd "$wt" && "$CLAUDE_BIN" -p --verbose --output-format stream-json $CLAUDE_ARGS --settings "$settings" \
      "$prompt") \
      </dev/null 2>&1 \
      | tee "$out" >>"$log"
    rc=$?
    last=$(grep '"type":"result"' "$out" | tail -1)
    if printf '%s' "$last" | grep -q 'NACHTLAUF: ABGEBROCHEN'; then
      why=$(printf '%s' "$last" | grep -o 'NACHTLAUF: ABGEBROCHEN[^"\\]*' | tail -1 \
        | sed -e 's/^NACHTLAUF: ABGEBROCHEN *//' -e 's/^— *//' -e 's/^- *//')
      result "$nr" abgebrochen "$phase: ${why:-ohne Grund}"
      return 1
    fi
    if [ "$rc" -eq 0 ] && [ -n "$last" ] && ! printf '%s' "$last" | grep -q '"is_error":true'; then
      return 0
    fi
    if printf '%s' "$last" | grep -qiE "$TRANSIENT"; then
      if [ "$attempt" -lt "$MAX_ATTEMPTS" ]; then
        echo "[$(ts)] API-Fehler #$nr ($phase, Versuch $attempt, exit=$rc), nächster Versuch" >>"$log"
        if [ "$mode" = reset ]; then
          git -C "$wt" checkout -q -f "auto/$nr" >>"$log" 2>&1
          git -C "$wt" reset -q --hard "$(cat "$RUN/base.$nr")" >>"$log" 2>&1
          git -C "$wt" clean -qfdx >>"$log" 2>&1
        fi
        attempt=$((attempt + 1))
        continue
      fi
      result "$nr" gescheitert "$phase: API-Fehler nach $attempt Versuchen"
    else
      result "$nr" gescheitert "$phase: exit=$rc"
    fi
    return 1
  done
}

run_one() {
  local nr=$1
  claude_phase "$nr" implement "/flow-implement $nr --nachtlauf${B_HINT:+ $B_HINT}" reset || return 0
  if [ "$B_MERGE" = ja ]; then
    claude_phase "$nr" ship "/flow-ship --nachtlauf" keep || return 0
  fi
  result "$nr" ok ""
}

# Kosten aller Phasen und Versuche dieses Laufs als Kommentar ans Issue. Exakt aus total_cost_usd von claude -p,
# darum hier und nicht in /flow-ship (dort fehlte die Ship-Phase selbst). Ein Fehler hier hält den Lauf nicht auf.
post_cost() {
  local nr=$1 f body rows="" total=0 cost phase attempt
  for f in "$RUN/$nr".*.jsonl; do
    [ -f "$f" ] || continue
    cost=$(grep '"type":"result"' "$f" | tail -1 | jq -r '.total_cost_usd // empty' 2>/dev/null)
    [ -n "$cost" ] || cost=0
    phase=$(basename "$f" .jsonl)
    attempt=${phase##*.}
    phase=${phase#"$nr".}
    phase=${phase%.*}
    rows="$rows$(printf '| %s | %s | %.2f |' "$phase" "$attempt" "$cost")"$'\n'
    total=$(awk -v a="$total" -v b="$cost" 'BEGIN { printf "%.4f", a + b }')
  done
  [ -n "$rows" ] || return 0
  body=$(printf '### Kosten der Umsetzung (Nachtlauf)\n\n| Phase | Versuch | Kosten (USD) |\n|---|---:|---:|\n%s| **Total** | | **%.2f** |\n\nAus `total_cost_usd` von `claude -p`, Lauf vom %s. Ein Versuch ohne Ergebnis (abgebrochen, gestoppt) zählt 0.\n' \
    "$rows" "$total" "$RUN_STARTED")
  (cd "$B_REPO" && gh issue comment "$nr" --body "$body") >/dev/null 2>&1 \
    || mlog "  WARN: Kosten von #$nr nicht ans Issue geschrieben"
}

record() {
  local nr=$1 state="" detail="" branch
  post_cost "$nr"
  refresh_main
  if [ -f "$RUN/result.$nr" ]; then
    state=$(sed -n 1p "$RUN/result.$nr")
    detail=$(sed -n 2p "$RUN/result.$nr")
  fi
  if on_main "$nr"; then
    set_status "$nr" auf-main ""
    mlog "  ✓ #$nr auf $BASE"
    cleanup_issue "$nr" merged
    return
  fi
  branch=$(pr_branch "$nr")
  if [ "$B_MERGE" = nein ] && [ "$state" = ok ] && [ -n "$branch" ]; then
    set_status "$nr" pr-offen "$branch"
    mlog "  ✓ #$nr PR offen ($branch)"
    cleanup_issue "$nr" open
    return
  fi
  case "$state" in
    abgebrochen | gescheitert) ;;
    ok) state=gescheitert detail="fertig gemeldet, aber nicht auf $BASE" ;;
    *) state=gescheitert detail="kein Ergebnis" ;;
  esac
  set_status "$nr" "$state" "$detail"
  mlog "  ✗ #$nr $state: $detail (Log: $STATE/logs/$nr.log, Worktree: $STATE/worktrees/$nr)"
}

start_issue() {
  local nr=$1
  if ! prepare_worktree "$nr"; then
    set_status "$nr" gescheitert "Worktree nicht angelegt"
    mlog "  ✗ #$nr Worktree nicht angelegt (Log: $STATE/logs/$nr.log)"
    return 1
  fi
  set_status "$nr" läuft ""
  mlog "→ #$nr start"
}

run_wave() {
  local w=$1 i nr missing todo=""
  mlog "================================================================"
  mlog "${W_TITLE[$w]}"
  mlog "================================================================"
  refresh_main
  for i in "${!I_NR[@]}"; do
    [ "${I_WAVE[$i]}" -eq "$w" ] || continue
    nr=${I_NR[$i]}
    if on_main "$nr"; then
      set_status "$nr" erledigt "lag schon auf $BASE"
      mlog "  = #$nr liegt schon auf $BASE"
      continue
    fi
    missing=$(blocked_by "$nr")
    if [ -n "$missing" ] && [ "$B_POOL" -gt 1 ]; then
      set_status "$nr" übersprungen "wartet auf$missing"
      mlog "  ⏭ #$nr übersprungen, Vorgänger fehlen auf $BASE:$missing"
      continue
    fi
    todo="$todo $nr"
  done
  [ -n "$todo" ] || { mlog "  (nichts zu tun)"; return; }

  if [ "$B_POOL" -le 1 ]; then
    for nr in $todo; do
      # Vorgänger erneut prüfen: Sie können eben in dieser Welle gescheitert sein
      refresh_main
      missing=$(blocked_by "$nr")
      if [ -n "$missing" ]; then
        set_status "$nr" übersprungen "wartet auf$missing"
        mlog "  ⏭ #$nr übersprungen, Vorgänger fehlen auf $BASE:$missing"
        continue
      fi
      start_issue "$nr" || continue
      # Im Hintergrund und mit wait: Nur so greift --stop sofort und nicht erst nach dem laufenden claude
      run_one "$nr" &
      wait $!
      record "$nr"
    done
  else
    local started=""
    for nr in $todo; do
      while [ "$(jobs -rp | wc -l)" -ge "$B_POOL" ]; do sleep "$POLL"; done
      start_issue "$nr" || continue
      run_one "$nr" &
      started="$started $nr"
    done
    wait
    for nr in $started; do record "$nr"; done
  fi
}

kill_tree() {
  local c
  for c in $(pgrep -P "$1" 2>/dev/null); do kill_tree "$c"; done
  kill "$1" 2>/dev/null
}

on_signal() {
  local c
  trap - TERM INT
  mlog "Gestoppt: laufende Issues werden beendet"
  for c in $(pgrep -P $$ 2>/dev/null); do kill_tree "$c"; done
  awk -F'\t' -v OFS='\t' -v t="$(ts)" '$2 == "läuft" { $2 = "gestoppt"; $4 = t } { print }' \
    "$STATE/status.tsv" >"$STATE/status.tsv.tmp" && mv "$STATE/status.tsv.tmp" "$STATE/status.tsv"
  exit 130
}

print_status_table() {
  [ -f "$STATE/status.tsv" ] || { echo "(noch kein Lauf)"; return; }
  { printf 'Issue\tZustand\tDetail\tZeit\n'; sed 's/^/#/' "$STATE/status.tsv"; } | column -t -s "$TAB"
}

summary() {
  local s count
  mlog "================================================================"
  mlog "Nachtlauf fertig: $(ts) (Start $RUN_STARTED)"
  for s in auf-main erledigt pr-offen abgebrochen gescheitert übersprungen; do
    count=$(awk -F'\t' -v s="$s" '$2 == s' "$STATE/status.tsv" | wc -l | tr -d ' ')
    [ "$count" -gt 0 ] && mlog "  $(printf '%-13s' "$s:") $count → $(awk -F'\t' -v s="$s" '$2 == s { printf "#%s ", $1 }' "$STATE/status.tsv")"
  done
  for s in ${B_MANUAL[@]+"${B_MANUAL[@]}"}; do mlog "  von Hand:     $s"; done
  mlog "================================================================"
  print_status_table | tee -a "$STATE/logs/master.log"
  mlog "Wiederholen setzt fort: $here/night-batch.sh --start $BATCH"
  ! awk -F'\t' '$2 != "auf-main" && $2 != "erledigt" && $2 != "pr-offen" { bad = 1 } END { exit !bad }' \
    "$STATE/status.tsv"
}

cleanup() {
  [ -n "$MAINLOG" ] && rm -f "$MAINLOG"
  if [ -n "$STATE" ] && [ "$(cat "$STATE/night-batch.pid" 2>/dev/null)" = "$$" ]; then
    rm -f "$STATE/night-batch.pid"
  fi
  [ -n "$COPY_DIR" ] && rm -rf "$COPY_DIR"
  [ -n "$RUN" ] && find "$RUN" -name '*.jsonl' -delete 2>/dev/null
  return 0
}

# --- Befehle ------------------------------------------------------------------------------------------------------

cmd_run() {
  # Aus einer Kopie laufen: bash liest ein Skript stückweise, und ein `git pull` mitten in der Nacht würde
  # sonst den laufenden Lauf zerschiessen. Auf macOS hält caffeinate den Rechner wach.
  if [ -z "${FLOW_NIGHT_COPIED:-}" ]; then
    local copy wrap=""
    copy=$(mktemp -d "${TMPDIR:-/tmp}/night-batch.XXXXXX")
    cp "$here/night-batch.sh" "$here/lib.sh" "$copy/"
    if [ "$(uname -s)" = Darwin ] && [ -z "${FLOW_NO_CAFFEINATE:-}" ] && command -v caffeinate >/dev/null; then
      wrap="caffeinate -i"
    fi
    FLOW_NIGHT_COPIED=1 FLOW_NIGHT_ORIGIN="$here" exec $wrap bash "$copy/night-batch.sh" "$BATCH"
  fi
  COPY_DIR=$here
  here=${FLOW_NIGHT_ORIGIN:-$here}

  load "$BATCH"
  trap cleanup EXIT
  mkdir -p "$STATE/logs" "$STATE/worktrees"
  if pid=$(running_pid); then die "für $B_REPO läuft schon ein Nachtlauf (PID $pid)"; fi
  echo $$ >"$STATE/night-batch.pid"
  trap on_signal TERM INT
  RUN="$STATE/run"
  rm -rf "$RUN"
  mkdir -p "$RUN"
  RUN_STARTED=$(ts)

  mlog "================================================================"
  mlog "Nachtlauf gestartet: $RUN_STARTED (PID $$)"
  mlog "Batch:      $BATCH"
  mlog "Repository: $B_REPO (Hauptzweig $BASE)${B_EPIC:+, Epic #$B_EPIC}"
  mlog "Pool:       $B_POOL, Merge: $B_MERGE"
  mlog "Ablage:     $STATE"
  mlog "================================================================"

  refresh_main
  if ! check_batch >"$RUN/check.txt"; then
    cat "$RUN/check.txt" | tee -a "$STATE/logs/master.log"
    mlog "Abbruch: Die Batch-Datei hat Fehler (night-batch.sh --dry-run $BATCH)"
    exit 2
  fi

  : >"$STATE/status.tsv"
  local i c w
  for i in "${!I_NR[@]}"; do set_status "${I_NR[$i]}" offen "${W_TITLE[${I_WAVE[$i]}]}"; done

  if [ -n "$(git -C "$B_REPO" status --porcelain)" ]; then
    mlog "WARN: Working Tree von $B_REPO ist nicht sauber; die Worktrees kommen trotzdem von origin/$BASE"
  fi
  for c in ${B_WARM[@]+"${B_WARM[@]}"}; do
    mlog "Warm-up: $c"
    (cd "$B_REPO" && bash -c "$c") >>"$STATE/logs/master.log" 2>&1 || mlog "  (Warm-up gescheitert, weiter)"
  done

  for w in "${!W_TITLE[@]}"; do run_wave "$w"; done
  summary
}

cmd_start() {
  load "$BATCH"
  trap cleanup EXIT
  local pid
  if pid=$(running_pid); then die "für $B_REPO läuft schon ein Nachtlauf (PID $pid)"; fi
  refresh_main
  check_batch || die "die Batch-Datei hat Fehler; nichts gestartet"
  mkdir -p "$STATE/logs"
  nohup bash "$here/night-batch.sh" "$BATCH" >>"$STATE/logs/nohup.out" 2>&1 </dev/null &
  echo "Nachtlauf gestartet (PID $!)."
  echo "  Mitlesen:  tail -f $STATE/logs/master.log"
  echo "  Je Issue:  $STATE/logs/<nr>.log"
  echo "  Stand:     $here/night-batch.sh --status $BATCH"
  echo "  Stoppen:   $here/night-batch.sh --stop $BATCH"
}

cmd_dry_run() {
  load "$BATCH"
  trap cleanup EXIT
  refresh_main
  print_plan
  echo
  if check_batch; then
    echo "ok      Batch-Datei geprüft"
  else
    exit 1
  fi
}

# --status / --stop: Argument ist eine Batch-Datei, ein Repository oder fehlt (Repository im aktuellen Ordner).
resolve_state() {
  local arg=${1:-}
  if [ -f "$arg" ]; then
    parse_batch "$arg"
    B_REPO=$(git -C "${B_REPO:-.}" rev-parse --show-toplevel 2>/dev/null) || die "kein Git-Repository: $B_REPO"
  else
    B_REPO=$(git -C "${arg:-.}" rev-parse --show-toplevel 2>/dev/null) || die "kein Git-Repository: ${arg:-.}"
  fi
  STATE=$(state_dir "$B_REPO")
}

cmd_status() {
  local pid
  resolve_state "${1:-}"
  echo "Ablage: $STATE"
  if pid=$(running_pid); then echo "Läuft:  ja (PID $pid)"; else echo "Läuft:  nein"; fi
  echo
  print_status_table
  if [ -f "$STATE/logs/master.log" ]; then
    echo
    echo "Letzte Zeilen von $STATE/logs/master.log:"
    tail -5 "$STATE/logs/master.log"
  fi
}

cmd_stop() {
  local pid
  resolve_state "${1:-}"
  pid=$(running_pid) || { echo "Kein Nachtlauf aktiv für $B_REPO."; return 0; }
  kill -TERM "$pid" && echo "Nachtlauf (PID $pid) wird beendet; Stand: --status"
}

case "${1:-}" in
  --start) BATCH=${2:-}; [ -n "$BATCH" ] || { usage; exit 64; }; BATCH=$(cd "$(dirname "$BATCH")" && pwd)/$(basename "$BATCH"); cmd_start ;;
  --dry-run) BATCH=${2:-}; [ -n "$BATCH" ] || { usage; exit 64; }; cmd_dry_run ;;
  --status) cmd_status "${2:-}" ;;
  --stop) cmd_stop "${2:-}" ;;
  -h | --help | '') usage; [ -n "${1:-}" ] ;;
  -*) usage; exit 64 ;;
  *) BATCH=$(cd "$(dirname "$1")" && pwd)/$(basename "$1"); cmd_run ;;
esac
