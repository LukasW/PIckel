#!/usr/bin/env bash
# PreToolUse-Hook (Bash): kein Push auf den Hauptzweig und kein Force-Push. Ersetzt Branch Protection, solange GitHub
# sie für dieses private Repository nicht anbietet; die Präfix-Regeln in settings.json allein lassen sich mit
# `HEAD:refs/heads/main`, `+refspec` oder einem Push ohne Refspec auf main umgehen.
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
printf '%s' "$cmd" | grep -Eq '(^|[^[:alnum:]_-])git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push([[:space:]]|$)' || exit 0

main=$(flow_base_branch "$root")
current=$(git -C "$root" symbolic-ref --quiet --short HEAD 2>/dev/null)
deny() {
  flow_deny "guard-push: $1. Änderungen kommen per PR auf $main; Force-Push nur durch einen Menschen."
}

# Jedes `git push …` bis zum nächsten ; & | einzeln prüfen
while IFS= read -r segment; do
  [ -n "$segment" ] || continue
  remote="" refspecs=0
  # shellcheck disable=SC2086 # Wörter des Befehls
  set -f
  set -- $segment
  set +f
  while [ $# -gt 0 ] && [ "$1" != push ]; do shift; done
  shift
  for word in "$@"; do
    word=${word//[\"\']/}
    case "$word" in
      --force | --force=* | -f | --force-with-lease | --force-with-lease=* | --force-if-includes | --mirror | --delete | -d)
        deny "Option $word ist nicht erlaubt" ;;
      -*) continue ;;
    esac
    if [ -z "$remote" ]; then
      remote=$word
      continue
    fi
    refspecs=$((refspecs + 1))
    case "$word" in
      +*) deny "Force-Push über «${word}»" ;;
    esac
    target=${word##*:}
    target=${target#refs/heads/}
    [ "$target" = HEAD ] && target=$current
    [ "$target" = "$main" ] && deny "Push auf $main («${word}»)"
  done
  if [ "$refspecs" -eq 0 ] && [ "$current" = "$main" ]; then
    deny "Push ohne Refspec auf dem Zweig $main"
  fi
done < <(printf '%s\n' "$cmd" | tr ';&|' '\n\n\n' | grep -E 'git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push')
exit 0
