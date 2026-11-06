#!/usr/bin/env bash
# PreToolUse-Hook (Bash): lehnt in Repositories mit `guard-env: true` in der flow.md Befehle ab, die eine .env
# nennen. `deny` in settings.json sperrt nur Read, nicht `cat .env` über Bash.
# Erlaubt bleiben `git ls-files .env` (sagt nur, ob sie versioniert ist) und Ausschlüsse wie --exclude='.env*'
# oder -g '!.env*'. env.example (ohne Punkt) ist eine Vorlage ohne Werte.
# Exit 2 = Befehl wird nicht ausgeführt, die Meldung geht an Claude.
# Bewusst grob: Auch Text, der die .env nur nennt (Heredoc, `git commit -m`), wird abgelehnt. Solchen Text mit
# Write/Edit schreiben bzw. `git commit -F <datei>` nehmen.
set -u
. "$(dirname "$0")/lib.sh"
command -v jq >/dev/null 2>&1 || exit 0

root=$(flow_root)
flow_require "$root" guard-env true

cmd=$(jq -r '.tool_input.command // empty')
[ -n "$cmd" ] || exit 0

rest=$(printf '%s' "$cmd" | sed -E \
  -e "s/git ls-files[[:space:]]+(--error-unmatch[[:space:]]+)?\.env([[:space:]]|$)/ /g" \
  -e "s/--exclude(-dir)?=['\"]?\.env[^[:space:]'\"]*['\"]?//g" \
  -e "s/-g[[:space:]]+['\"]?!\.env[^[:space:]'\"]*['\"]?//g" \
  -e "s/--glob[[:space:]=]+['\"]?!\.env[^[:space:]'\"]*['\"]?//g")

if printf '%s' "$rest" | grep -Eq "(^|[[:space:]/\"'=<])\.env(\.[[:alnum:]_-]+)?([[:space:]\"';|&)>]|$)"; then
  echo "guard-env: Der Befehl nennt die .env mit echten Zugangsdaten und wird nicht ausgeführt. Nie lesen oder durchsuchen; eine Vorlage ohne Werte heisst env.example." >&2
  exit 2
fi
exit 0
