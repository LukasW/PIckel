#!/usr/bin/env bash
# Meldet Drift zwischen Code, Stories, Deltas und Feature-Dateien. Verglichen wird der ganze Branch mit der
# Abzweigung vom Hauptzweig, samt ungestagten und neuen Dateien.
#
# Aufruf: im Handoff (`/flow-implement` Schritt 12), in `/flow-ship` und als CI-Gate («Spec-Drift» in ci-pr.yml).
# Exit 1, wenn etwas nachzuholen ist; Exit 0 sonst und in Repositories ohne `stories:` im Kopf der flow.md.
# Ordner aus dem Kopf der flow.md:
#   stories: spec/user-stories   Ort der Story-Dateien
#   deltas: spec/changes         Change-Ordner je Issue (`shared/story-delta.md`); ohne den Schalter keine Delta-Prüfung
#
# Code ist eine Datei mit einer der Endungen unten ausserhalb der Testordner; Tests erkennt `flow_is_test`.
set -u
. "$(dirname "$0")/lib.sh"

root=$(flow_root)
cd "$root" || exit 0
[ -f .claude/flow.md ] || exit 0

stories=$(flow_option "$root" stories)
stories=${stories%/}
if [ -z "$stories" ]; then
  echo "Keine Stories in flow.md (stories:), nichts zu prüfen."
  exit 0
fi
deltas=$(flow_option "$root" deltas)
deltas=${deltas%/}
features_dir=src/test/resources/features

base=$(flow_merge_base "$(flow_base_branch "$root")") || exit 0

code_files="" story_files="" delta_files="" plan_files=""
while IFS= read -r path; do
  [ -n "$path" ] || continue
  case "$path" in
    "$stories"/*) story_files+="$path"$'\n'; continue ;;
  esac
  if [ -n "$deltas" ]; then
    case "$path" in
      "$deltas"/*/delta.md) delta_files+="$path"$'\n'; continue ;;
      "$deltas"/*/plan.md) plan_files+="$path"$'\n'; continue ;;
      "$deltas"/*) continue ;;
    esac
  fi
  if flow_is_test "$path"; then
    code_files+="$path"$'\n'
    continue
  fi
  case "$path" in
    *.java|*.kt|*.kts|*.scala|*.groovy|*.ts|*.tsx|*.js|*.jsx|*.mjs|*.cjs|*.vue|*.svelte|*.py|*.go|*.rb|*.cs|*.php|*.rs|*.swift|*.sql)
      code_files+="$path"$'\n'
      ;;
  esac
done < <({
  git -c core.quotepath=off diff --name-only "$base"
  git -c core.quotepath=off ls-files --others --exclude-standard
} | sort -u)

# Plan des Issues zum Branch (feature/<nr>-…), auch wenn er schon auf dem Hauptzweig liegt (Nachtlauf). In CI ist
# der Checkout ein Merge-Ref; dort nennt GITHUB_HEAD_REF den Branch.
branch=${GITHUB_HEAD_REF:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null)}
issue_nr=$(printf '%s' "$branch" | sed -nE 's#^[a-z]+/([0-9]+)-.*#\1#p')
if [ -n "$deltas" ] && [ -n "$issue_nr" ]; then
  for plan in "$deltas/$issue_nr"-*/plan.md; do
    [ -f "$plan" ] && plan_files+="$plan"$'\n'
  done
fi

# Erste Einträge einer Liste, mit «…» bei mehr.
first() {
  local list total
  list=$(printf '%s' "$1" | sed '/^$/d')
  total=$(printf '%s\n' "$list" | wc -l | tr -d ' ')
  printf '%s' "$(printf '%s\n' "$list" | head -3 | paste -sd ',' - | sed 's/,/, /g')"
  [ "$total" -gt 3 ] && printf ', … (%s Dateien)' "$total"
  return 0
}

# Ein plan.md mit «Delta: keines — <Grund>» begründet Code ohne Story-Änderung (Refactoring, Build, Bugfix).
no_delta_declared() {
  local plan
  while IFS= read -r plan; do
    [ -f "$plan" ] || continue
    grep -qE '^Delta: keines[[:space:]]*(—|-|–)[[:space:]]*[^[:space:]]' "$plan" && return 0
  done < <(printf '%s' "$plan_files")
  return 1
}

# Szenario-Titel aller Feature-Dateien, einer je Zeile.
feature_titles() {
  [ -d "$features_dir" ] || return 0
  grep -rhE '^[[:space:]]*(Szenario|Szenariogrundriss|Szenario-Grundriss|Scenario|Scenario Outline):' \
    --include='*.feature' "$features_dir" | sed -E 's/^[^:]*:[[:space:]]*//; s/[[:space:]]+$//'
}

# «### Szenario: <Titel>» je Abschnitt eines Deltas: Ausgabe «<abschnitt><TAB><titel>».
delta_scenarios() {
  awk '
    /^## / { section = substr($0, 4); sub(/[[:space:]]+$/, "", section) }
    /^### Szenario:/ {
      title = $0; sub(/^### Szenario:[[:space:]]*/, "", title); sub(/[[:space:]]+$/, "", title)
      print section "\t" title
    }' "$1"
}

findings=""
if [ -n "$code_files" ] && [ -z "$story_files" ] && [ -z "$delta_files" ] && ! no_delta_declared; then
  findings+="Code oder Tests geändert, aber keine Story, kein Delta und kein plan.md mit «Delta: keines — <Grund>»: $(first "$code_files")"$'\n'
fi
if [ -n "$deltas" ]; then
  if [ -n "$story_files" ] && [ -z "$delta_files" ]; then
    findings+="Story geändert ohne Delta unter $deltas/: $(first "$story_files")"$'\n'
  fi
  # Ein PR ohne Code darf eine Spezifikation vorab bringen (Delta im Entwurf oder bestätigt); eingepflegt wird mit
  # dem Code.
  spec_only=false
  [ -z "$code_files" ] && spec_only=true
  if ! $spec_only && [ -n "$delta_files" ] && [ -z "$story_files" ]; then
    findings+="Delta nicht in die Story eingepflegt (keine Story unter $stories/ geändert): $(first "$delta_files")"$'\n'
  fi
  titles=$(feature_titles)
  while IFS= read -r delta; do
    [ -f "$delta" ] || continue
    # Jeder Abschnitt `# Delta US-…` hat seine eigene Zeile `Status:`; alle müssen eingepflegt sein.
    statuses=$(grep -oE 'Status:[[:space:]]*[A-Za-zäöü]+' "$delta" | sed 's/^Status:[[:space:]]*//')
    open=$(printf '%s\n' "$statuses" | grep -vx eingepflegt | head -1)
    if [ -z "$statuses" ]; then
      findings+="$delta: Zeile «Status:» fehlt"$'\n'
      continue
    fi
    if [ -n "$open" ]; then
      $spec_only || findings+="$delta: Status «${open}», nicht eingepflegt"$'\n'
      continue
    fi
    while IFS=$'\t' read -r section title; do
      [ -n "$title" ] || continue
      present=false
      printf '%s\n' "$titles" | grep -qxF "$title" && present=true
      case "$section" in
        Neu | Geändert)
          $present || findings+="$delta: Szenario «${title}» ($section) fehlt in $features_dir/"$'\n' ;;
        Entfernt)
          $present && findings+="$delta: Szenario «${title}» ist entfernt, steht aber noch in $features_dir/"$'\n' ;;
      esac
    done < <(delta_scenarios "$delta")
  done < <(printf '%s' "$delta_files")
fi

# Jede Story verweist auf eine Feature-Datei, die es gibt.
if [ -d "$stories" ]; then
  while IFS= read -r story; do
    while IFS= read -r ref; do
      [ -f "$ref" ] || findings+="$story verweist auf $ref, die Datei fehlt"$'\n'
    done < <(grep -oE "$features_dir/[^])\` ]+\.feature" "$story" | sort -u)
  done < <(find "$stories" -name '*.md' -type f)
fi

findings=$(printf '%s' "$findings" | sed '/^$/d')
short=$(git rev-parse --short "$base")
if [ -n "$findings" ]; then
  echo "Drift zwischen Code, Stories und Features seit $short – nachholen:"
  echo "$findings"
  exit 1
fi
echo "Keine Drift zwischen Code, Stories und Features seit $short."
exit 0
