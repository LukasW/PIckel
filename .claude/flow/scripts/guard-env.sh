#!/usr/bin/env bash
# PreToolUse-Hook (Bash, Read, Grep, Glob): lehnt in Repositories mit `guard-env: true` in der flow.md den Zugriff
# auf eine .env ab. `deny` in settings.json sperrt nur Read in Claude Code, nicht `cat .env` über Bash, und ob die
# Copilot CLI diese Regeln beachtet, ist nicht gemessen (ADR 0007).
# - Bash: Befehle, die eine .env nennen. Erlaubt bleiben `git ls-files .env` (sagt nur, ob sie versioniert ist) und
#   Ausschlüsse wie --exclude='.env*' oder -g '!.env*'. Bewusst grob: Auch Text, der die .env nur nennt (Heredoc,
#   `git commit -m`), wird abgelehnt. Solchen Text mit Write/Edit schreiben bzw. `git commit -F <datei>` nehmen
# - Lese- und Suchwerkzeuge: ein Pfadfeld, dessen letzter Teil eine .env nennt, oder ein Muster, das auf sie passt.
#   Eine Suche über einen ganzen Ordner ohne Muster wird nicht abgelehnt; dort schützen .gitignore und die Ausschlüsse
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

if [ "$tool" != Bash ]; then
  unreadable="guard-env: Eingabe von $tool nicht lesbar; ohne Prüfung lässt der Wächter nichts durch (ADR 0007)."
  printf '%s' "$payload" | jq -e '(.tool_input // {}) | type == "object"' >/dev/null 2>&1 || flow_deny "$unreadable"
  # Letzter Pfadteil der Pfadfelder (wörtlich) und der Musterfelder (glob, bei Glob auch pattern)
  last='flatten[] | select(type == "string") | sub("/+$"; "") | split("/") | last'
  names=$(printf '%s' "$payload" | jq -r "(.tool_input // {}) | [.file_path, .path, .paths, .notebook_path, .filePath] | $last" 2>/dev/null) \
    || flow_deny "$unreadable"
  if printf '%s\n' "$names" | grep -Eqi '^\.env(\.[[:alnum:]_-]+)?$'; then
    flow_deny "$reason"
  fi
  # Ein Muster mit Platzhalter, das auf eine .env passt, liest in Grep ihren Inhalt: Das glob-Feld übersteuert in
  # ripgrep .gitignore und versteckte Dateien. Abgeglichen wird gegen typische Namen, ohne Gross-/Kleinschreibung;
  # Klammern {a,b} werden eine Ebene tief aufgelöst.
  patterns=$(printf '%s' "$payload" | jq -r ".tool_name as \$t | (.tool_input // {}) | [.glob, (if \$t == \"Glob\" then .pattern else empty end)] | $last" 2>/dev/null) \
    || flow_deny "$unreadable"
  shopt -s nocasematch
  while IFS= read -r p; do
    [ -n "$p" ] || continue
    candidates=("$p")
    if [[ "$p" =~ ^(.*)\{([^{}]*)\}(.*)$ ]]; then
      candidates=()
      IFS=, read -ra alts <<<"${BASH_REMATCH[2]}"
      for a in "${alts[@]}"; do candidates+=("${BASH_REMATCH[1]}$a${BASH_REMATCH[3]}"); done
    fi
    for c in "${candidates[@]}"; do
      for name in .env .env.local .env.production; do
        # shellcheck disable=SC2053 # $c ist bewusst ein Muster
        [[ "$name" == $c ]] && flow_deny "$reason"
      done
    done
  done <<<"$patterns"
  exit 0
fi

cmd=$(flow_tool_command "$payload") \
  || flow_deny "guard-env: Kein Befehl in der Eingabe erkannt; ohne Befehl lässt der Wächter nichts durch (ADR 0007)."

rest=$(printf '%s' "$cmd" | sed -E \
  -e "s/git ls-files[[:space:]]+(--error-unmatch[[:space:]]+)?\.env([[:space:]]|$)/ /g" \
  -e "s/--exclude(-dir)?=['\"]?\.env[^[:space:]'\"]*['\"]?//g" \
  -e "s/-g[[:space:]]+['\"]?!\.env[^[:space:]'\"]*['\"]?//g" \
  -e "s/--glob[[:space:]=]+['\"]?!\.env[^[:space:]'\"]*['\"]?//g")

if printf '%s' "$rest" | grep -Eq "(^|[[:space:]/\"'=<])\.env(\.[[:alnum:]_-]+)?([[:space:]\"';|&)>]|$)"; then
  flow_deny "$reason"
fi
exit 0
