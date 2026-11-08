#!/usr/bin/env bash
# PreToolUse-Hook (Bash, Read, Grep, Glob): lehnt in Repositories mit `guard-env: true` in der flow.md den Zugriff
# auf eine .env ab. `deny` in settings.json sperrt nur Read in Claude Code, nicht `cat .env` über Bash, und ob die
# Copilot CLI diese Regeln beachtet, ist nicht gemessen (ADR 0007).
# - Bash: Befehle, die eine .env nennen. Erlaubt bleiben `git ls-files .env` (sagt nur, ob sie versioniert ist) und
#   Ausschlüsse wie --exclude='.env*' oder -g '!.env*'. Bewusst grob: Auch Text, der die .env nur nennt (Heredoc,
#   `git commit -m`), wird abgelehnt. Solchen Text mit Write/Edit schreiben bzw. `git commit -F <datei>` nehmen
# - Lese- und Suchwerkzeuge: ein Pfadfeld, dessen letzter Teil eine .env nennt, oder ein Muster, das auf sie passt.
#   Eine Suche über einen ganzen Ordner ohne Muster wird nicht abgelehnt; dort schützen .gitignore und die Ausschlüsse
# - Beide: Der Bash-Befehl wird geprüft, wie die Shell ihn sieht (flow_shell_segments: ohne Anführungszeichen,
#   Backslashes, Zeilenfortsetzungen und Umleitungszeichen). Wörter mit Platzhalter, deren letzter Teil mit .
#   beginnt oder env enthält (.en*, {.env,x}, *env), werden wie Muster gegen die Namen abgeglichen; Pfade, die im cwd
#   der Eingabe existieren, werden über Symlinks aufgelöst (bei Platzhaltern jeder Treffer), in Bash wie in den
#   Pfadfeldern der Lese-Werkzeuge.
# Bedrohungsmodell (ADR 0007): Der Wächter fängt Versehen und naheliegende Umgehungen ab, er ist keine Sandbox. Durch
# kommen ein Skript, eine Variable, eval mit zusammengesetztem Text, eine Pipe, die den Namen erst zur Laufzeit bildet
# (ls -a | grep env | xargs cat), harte Links und Kopien der Datei, ein Symlink relativ zu einem anderen Ordner als
# cwd (cd sub && cat link) sowie eine Suche über einen ganzen Ordner (grep -r .). Bewusste Fehlalarme: Regex-Muster
# mit Punkt am Anfang (grep -E '.*'), find -name '.*'.
# env.example (ohne Punkt) ist eine Vorlage ohne Werte. Ablehnung über flow_deny: Der Aufruf wird nicht ausgeführt.
set -u
. "$(dirname "$0")/lib.sh"
root=$(flow_root)
flow_require "$root" guard-env true
flow_need_jq guard-env

reason="guard-env: Der Aufruf nennt die .env mit echten Zugangsdaten und wird nicht ausgeführt. Nie lesen oder durchsuchen; eine Vorlage ohne Werte heisst env.example."
payload=$(flow_payload)
tool=$(printf '%s' "$payload" | jq -r '.tool_name // "Bash"' 2>/dev/null) || tool=Bash

case "$tool" in
  Bash|Read|Grep|Glob) ;;
  *) exit 0 ;; # .github/hooks/flow.json kennt keinen Matcher; Schreiben schützt guard-env nicht
esac

# Ordner, gegen den relative Pfade aufgelöst werden: cwd der Eingabe, sonst das aktuelle Verzeichnis
dir=$(printf '%s' "$payload" | jq -r --arg pwd "$PWD" 'if type == "object" and (.cwd | type) == "string" and .cwd != "" then .cwd else $pwd end' 2>/dev/null) || dir=$PWD
[ -n "$dir" ] || dir=$PWD
shopt -s nocasematch

# Name einer Geheimnis-Datei (letzter Pfadteil), ohne Gross-/Kleinschreibung
is_env_name() {
  [[ "$1" =~ ^\.env(\.[[:alnum:]_-]+)*$ ]]
}

# Passt das Muster ($1, letzter Pfadteil) auf einen typischen Namen? Klammern {a,b} werden aufgelöst, auch mehrere
# und verschachtelte (höchstens 256 Kandidaten), Bereiche {a..c} aus einzelnen Zeichen werden zu [a-c]. Mit $2 = bash
# zählen nur Kandidaten, die mit . beginnen oder env enthalten: Ein Platzhalter am Anfang trifft in der Shell keinen
# Punktnamen (cat *), in find -name oder grep --include aber schon (*env).
glob_hits_env() {
  local c name
  local -a todo=("$1") candidates=() alts
  while [ "${#todo[@]}" -gt 0 ] && [ "${#candidates[@]}" -lt 256 ]; do
    c=${todo[0]}
    todo=("${todo[@]:1}")
    if [[ "$c" =~ ^(.*)\{([^{}])\.\.([^{}])\}(.*)$ ]]; then
      todo+=("${BASH_REMATCH[1]}[${BASH_REMATCH[2]}-${BASH_REMATCH[3]}]${BASH_REMATCH[4]}")
    elif [[ "$c" =~ ^(.*)\{([^{}]*,[^{}]*)\}(.*)$ ]]; then
      IFS=, read -ra alts <<<"${BASH_REMATCH[2]}"
      [[ "${BASH_REMATCH[2]}" == *, ]] && alts+=("")
      for name in "${alts[@]}"; do todo+=("${BASH_REMATCH[1]}$name${BASH_REMATCH[3]}"); done
    else
      candidates+=("$c")
    fi
  done
  for c in "${candidates[@]}"; do
    [ "${2:-}" = bash ] && [[ "$c" != .* && "$c" != *env* ]] && continue
    for name in .env .env.local .env.production .env.production.local .env.development .env.development.local .env.test; do
      # shellcheck disable=SC2053 # $c ist bewusst ein Muster
      [[ "$name" == $c ]] && return 0
    done
  done
  return 1
}

# Zeigt der Pfad ($1, relativ zu $dir) über Symlinks auf eine Geheimnis-Datei?
resolves_to_env() {
  local p="$1" target
  case "$p" in /*) ;; *) p="$dir/$p" ;; esac
  [ -L "$p" ] || [ -e "$p" ] || return 1
  target=$(readlink -f -- "$p" 2>/dev/null) || return 1
  is_env_name "${target##*/}"
}

if [ "$tool" != Bash ]; then
  unreadable="guard-env: Eingabe von $tool nicht lesbar; ohne Prüfung lässt der Wächter nichts durch (ADR 0007)."
  printf '%s' "$payload" | jq -e '(.tool_input // {}) | type == "object"' >/dev/null 2>&1 || flow_deny "$unreadable"
  # Letzter Pfadteil der Pfadfelder (wörtlich) und der Musterfelder (glob, bei Glob auch pattern)
  last='flatten[] | select(type == "string") | sub("/+$"; "") | split("/") | last'
  names=$(printf '%s' "$payload" | jq -r "(.tool_input // {}) | [.file_path, .path, .paths, .notebook_path, .filePath] | $last" 2>/dev/null) \
    || flow_deny "$unreadable"
  while IFS= read -r n; do
    is_env_name "$n" && flow_deny "$reason"
  done <<<"$names"
  # Pfadfelder über Symlinks auflösen
  paths=$(printf '%s' "$payload" | jq -r '(.tool_input // {}) | [.file_path, .path, .paths, .notebook_path, .filePath] | flatten[] | select(type == "string" and . != "")' 2>/dev/null) \
    || flow_deny "$unreadable"
  while IFS= read -r p; do
    [ -n "$p" ] && resolves_to_env "$p" && flow_deny "$reason"
  done <<<"$paths"
  # Ein Muster mit Platzhalter, das auf eine .env passt, liest in Grep ihren Inhalt: Das glob-Feld übersteuert in
  # ripgrep .gitignore und versteckte Dateien. Abgeglichen wird gegen typische Namen, ohne Gross-/Kleinschreibung
  # (glob_hits_env).
  patterns=$(printf '%s' "$payload" | jq -r ".tool_name as \$t | (.tool_input // {}) | [.glob, (if \$t == \"Glob\" then .pattern else empty end)] | $last" 2>/dev/null) \
    || flow_deny "$unreadable"
  while IFS= read -r p; do
    [ -n "$p" ] && glob_hits_env "$p" && flow_deny "$reason"
  done <<<"$patterns"
  exit 0
fi

cmd=$(flow_tool_command "$payload") \
  || flow_deny "guard-env: Kein Befehl in der Eingabe erkannt; ohne Befehl lässt der Wächter nichts durch (ADR 0007)."

norm=$(flow_shell_segments "$cmd")
rest=$(printf '%s\n' "$norm" | sed -E \
  -e "s/^git ls-files[[:space:]]+(--error-unmatch[[:space:]]+)?\.env([[:space:]]|$)/ /" \
  -e "s/--exclude(-dir)?=\.env[^[:space:]]*//g" \
  -e "s/-g[[:space:]]+!\.env[^[:space:]]*//g" \
  -e "s/--glob[[:space:]=]+!\.env[^[:space:]]*//g" \
  -e "s/(^|[[:space:]]):[!^]\.env[^[:space:]]*//g")

# Vor dem Namen jedes Zeichen ausser Buchstabe, Ziffer, _ und . (@.env für curl, $'.env', ${F:-.env}), danach jedes
# ausser Buchstabe, Ziffer, _, . und -
if printf '%s' "$rest" | grep -Eqi "(^|[^[:alnum:]_.])\.env(\.[[:alnum:]_-]+)*([^[:alnum:]_.-]|$)"; then
  flow_deny "$reason"
fi

# Platzhalter-Wörter und Symlinks; Wörter ohne Platzhalter-Expansion
set -f
# shellcheck disable=SC2206 # Wörter des normalisierten Befehls
words=($rest)
set +f
for w in "${words[@]}"; do
  case "$w" in
    *[*?[{]*)
      glob_hits_env "${w##*[/=]}" bash && flow_deny "$reason"
      while IFS= read -r m; do
        [ -n "$m" ] && resolves_to_env "$m" && flow_deny "$reason"
      done < <(cd "$dir" 2>/dev/null && compgen -G "$w")
      ;;
    *) resolves_to_env "$w" && flow_deny "$reason" ;;
  esac
done
exit 0
