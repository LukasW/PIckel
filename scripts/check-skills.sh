#!/usr/bin/env bash
# Prüft die Skills unter .agents/skills/ gegen die Agent-Skills-Spezifikation (https://agentskills.io/specification)
# und dass Claude Code jeden Skill über einen Symlink unter .claude/skills/ findet.
#
# Erlaubt sind die Felder der Spezifikation und, als bewusste Ausnahme, die Claude-Code-Felder, ohne die der Ablauf
# nicht funktioniert (docs/adr/0002-agent-skills-standard.md). Andere Clients ignorieren sie oder lehnen sie ab;
# skills-ref validate meldet sie als «Unexpected fields».
set -uo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root" || exit 1

spec_fields="name description license compatibility metadata allowed-tools"
claude_fields="argument-hint disable-model-invocation model"
max_lines=500
errors=0

fail() { echo "[ERROR] $1"; errors=$((errors + 1)); }

for dir in .agents/skills/*/; do
  dir="${dir%/}"
  skill="$(basename "$dir")"
  file="$dir/SKILL.md"
  [ -f "$file" ] || { fail "$dir: SKILL.md fehlt"; continue; }

  # Frontmatter: zwischen der ersten und der zweiten Zeile «---»
  frontmatter="$(awk 'NR==1 && $0!="---" {exit} NR>1 && $0=="---" {exit} NR>1 {print}' "$file")"
  [ -n "$frontmatter" ] || { fail "$file: Frontmatter fehlt"; continue; }

  value() { printf '%s\n' "$frontmatter" | sed -n "s/^$1:[[:space:]]*//p" | head -1 | sed 's/^"\(.*\)"$/\1/'; }

  while IFS= read -r key; do
    case " $spec_fields $claude_fields " in
      *" $key "*) ;;
      *) fail "$file: Feld «${key}» ist weder in der Spezifikation noch als Claude-Code-Feld zugelassen" ;;
    esac
  done < <(printf '%s\n' "$frontmatter" | sed -n 's/^\([A-Za-z_-][A-Za-z0-9_-]*\):.*/\1/p')

  name="$(value name)"
  if [ -z "$name" ]; then
    fail "$file: name fehlt"
  else
    [ "$name" = "$skill" ] || fail "$file: name «${name}» entspricht nicht dem Ordnernamen «${skill}»"
    [ "${#name}" -le 64 ] || fail "$file: name länger als 64 Zeichen"
    [[ "$name" =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]] \
      || fail "$file: name nur aus a-z, 0-9 und einzelnen Bindestrichen, nicht am Anfang oder Ende"
  fi

  description="$(value description)"
  [ -n "$description" ] || fail "$file: description fehlt"
  [ "${#description}" -le 1024 ] || fail "$file: description länger als 1024 Zeichen (${#description})"

  compatibility="$(value compatibility)"
  [ "${#compatibility}" -le 500 ] || fail "$file: compatibility länger als 500 Zeichen (${#compatibility})"

  lines="$(wc -l <"$file" | tr -d ' ')"
  [ "$lines" -lt "$max_lines" ] || fail "$file: $lines Zeilen, die Spezifikation empfiehlt unter $max_lines"

  # Relative Verweise im Skill (z. B. references/x.md) müssen existieren
  while IFS= read -r target; do
    [ -e "$dir/$target" ] || fail "$file: Verweis «${target}» zeigt ins Leere"
  done < <(grep -oE '\]\([^)#:]+\)' "$file" | sed 's/^](\(.*\))$/\1/' | grep -v '^/')

  link=".claude/skills/$skill"
  [ -L "$link" ] && [ "$(readlink "$link")" = "../../.agents/skills/$skill" ] \
    || fail "$link: Symlink auf ../../.agents/skills/$skill fehlt, Claude Code findet den Skill nicht"
done

for link in .claude/skills/*; do
  [ -e "$link" ] || [ -L "$link" ] || continue
  if [ ! -L "$link" ]; then
    fail "$link: Skills gehören nach .agents/skills/, unter .claude/skills/ nur Symlinks"
  elif [ ! -e "$link" ]; then
    fail "$link: Symlink zeigt ins Leere"
  fi
done

if [ "$errors" -gt 0 ]; then
  echo "Skills: $errors Fehler"
  exit 1
fi
echo "Skills: $(find .agents/skills -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ') geprüft, ok"
