#!/usr/bin/env bash
# PreToolUse-Hook (Bash): kein Push auf den Hauptzweig und kein Force-Push. Ersetzt Branch Protection, solange GitHub
# sie für dieses private Repository nicht anbietet; die Präfix-Regeln in settings.json allein lassen sich mit
# `HEAD:refs/heads/main`, `+refspec` oder einem Push ohne Refspec auf main umgehen.
# Geprüft wird der Befehl so, wie die Shell ihn sieht (flow_shell_segments): Ein Segment ist ein Push, wenn ein Wort
# mit Basisname git und danach das Wort push vorkommt, egal was dazwischen steht (-c k=v, --no-pager, -C dir).
# Auch `git-push …` ist ein Push, `git stash push` nicht.
# Abgelehnt: Ziel main (auch über HEAD, @, heads/main, $(…) auf main, Glob-Refspec, Refspec «:» oder einen Upstream
# auf main ohne Refspec), Force, --mirror, --delete, --all, --branches, auch abgekürzt (--del) oder gebündelt (-fu).
# Bedrohungsmodell (ADR 0007): Der Wächter fängt Versehen und naheliegende Umgehungen ab, er ist keine Sandbox. Durch
# kommen ein Skript, eine Variable ($G push, HEAD:$(echo main), $'main'), eval mit zusammengesetztem Text, eine Pipe
# (echo main | xargs git push origin), ein Git-Alias und Konfiguration (-c remote.origin.push=…). Bewusste
# Fehlalarme (fail closed): Text in Anführungszeichen zählt wie ein Befehl, `git commit -m "push origin main"` wird
# abgelehnt (Ausweg `git commit -F <datei>`); auf main auch `git log --grep push`.
# Ablehnung über flow_deny: Befehl wird nicht ausgeführt, die Meldung geht an Claude Code bzw. Copilot.
set -u
. "$(dirname "$0")/lib.sh"
root=$(flow_root)
[ -f "$root/.claude/flow.md" ] || exit 0
flow_need_jq guard-push

payload=$(flow_payload)
# .github/hooks/flow.json kennt keinen Matcher: nur Terminal-Aufrufe prüfen; ohne tool_name gilt Bash (fail closed)
[ "$(printf '%s' "$payload" | jq -r '.tool_name // "Bash"' 2>/dev/null || echo Bash)" = Bash ] || exit 0
cmd=$(flow_tool_command "$payload") \
  || flow_deny "guard-push: Kein Befehl in der Eingabe erkannt; ohne Befehl lässt der Wächter nichts durch (ADR 0007)."
segments=$(flow_shell_segments "$cmd")

main=$(flow_base_branch "$root")
current=$(git -C "$root" symbolic-ref --quiet --short HEAD 2>/dev/null)
deny() {
  flow_deny "guard-push: $1. Änderungen kommen per PR auf $main; Force-Push nur durch einen Menschen."
}

# Jedes Segment mit git … push einzeln prüfen
while IFS= read -r segment; do
  [ -n "$segment" ] || continue
  # shellcheck disable=SC2086 # Wörter des Befehls
  set -f
  set -- $segment
  set +f
  git_seen=0 is_push=0 previous=""
  while [ $# -gt 0 ]; do
    if [ "${1##*/}" = git-push ]; then
      is_push=1
    elif [ "$git_seen" = 0 ]; then
      [ "${1##*/}" = git ] && git_seen=1
    elif [ "$1" = push ] && [ "$previous" != stash ]; then
      is_push=1
    fi
    previous=$1
    shift
    [ "$is_push" = 1 ] && break
  done
  [ "$is_push" = 1 ] || continue
  remote="" refspecs=0
  for word in "$@"; do
    case "$word" in
      --*)
        # Git nimmt eindeutig abgekürzte Langoptionen an (--del, --force-w): jedes Präfix ab drei Zeichen zählt
        name=${word%%=*}
        if [ "${#name}" -ge 3 ]; then
          for forbidden in --force --force-with-lease --force-if-includes --mirror --delete --all --branches; do
            case "$forbidden" in
              "$name"*) deny "Option $word ist nicht erlaubt" ;;
            esac
          done
        fi
        continue ;;
      -*)
        # Gebündelte Kurzoptionen (-fu, -ud)
        case "$word" in
          *[fd]*) deny "Option $word ist nicht erlaubt" ;;
        esac
        continue ;;
    esac
    if [ -z "$remote" ]; then
      remote=$word
      continue
    fi
    refspecs=$((refspecs + 1))
    case "$word" in
      +*) deny "Force-Push über «${word}»" ;;
      :) deny "Refspec «:» schiebt alle passenden Zweige, auch $main" ;;
    esac
    target=${word##*:}
    # Rest einer Ersetzung $(…) oder einer Variablen: das Ziel ist erst zur Laufzeit bekannt und meist der aktuelle
    # Zweig ($(git branch --show-current)); wie HEAD behandelt
    case "$target" in
      *\$*) target=HEAD ;;
    esac
    case "$target" in
      *[*?[]*)
        # shellcheck disable=SC2053 # $target ist bewusst ein Muster
        if [[ "refs/heads/$main" == $target || "$main" == $target ]]; then deny "Glob-Refspec «${word}» trifft $main"; fi
        continue ;;
    esac
    target=${target#refs/heads/}
    target=${target#heads/}
    case "$target" in HEAD | @) target=$current ;; esac
    [ "$target" = "$main" ] && deny "Push auf $main («${word}»)"
  done
  if [ "$refspecs" -eq 0 ]; then
    [ "$current" = "$main" ] && deny "Push ohne Refspec auf dem Zweig $main"
    # Ohne Refspec bestimmt Git das Ziel aus push.default und dem Upstream; kann es @{push} nicht auflösen, gilt nur
    # der aktuelle Zweig
    push_ref=$(git -C "$root" rev-parse --symbolic-full-name '@{push}' 2>/dev/null)
    case "$push_ref" in
      refs/remotes/*)
        # refs/remotes/<remote>/<zweig>, Remote ohne /
        pushed=${push_ref#refs/remotes/}
        [ "${pushed#*/}" = "$main" ] && deny "Push ohne Refspec auf $main (@{push} ist $push_ref)" ;;
    esac
  fi
done <<<"$segments"
exit 0
