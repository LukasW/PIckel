#!/usr/bin/env bash
# Lokaler Ersatz für ci-pr.yml, wenn GitHub Actions nicht läuft (Kontingent aufgebraucht, Ausfall). Führt dieselben
# Prüfungen wie die Pflicht-Checks aus und meldet sie bei Erfolg als Commit-Status mit deren Namen am Head des PR.
# Branch Protection nimmt sie an, weil die Pflicht-Checks keine Quelle vorschreiben (ADR 0004).
#
#   scripts/ci-local.sh            prüfen, Label setzen, bei Grün die vier Status melden
#   scripts/ci-local.sh --dry-run  nur prüfen, nichts an GitHub melden
#
# Weigert sich, wenn CI für diesen Commit tatsächlich gelaufen ist (rot oder noch aktiv): Ein roter CI-Lauf wird
# behoben, nicht lokal überstimmt. Direktes Melden eines Status ohne dieses Skript blockiert guard-labels.sh.
set -uo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root" || exit 1
dry=false
[ "${1:-}" = "--dry-run" ] && dry=true
logdir="${TMPDIR:-/tmp}"
# Namen der Pflicht-Checks in der Branch Protection = Job-Namen in ci-pr.yml
checks=("Unit Tests" "Integration + Smoke" "Spec-Drift" "Secret-Scan (Gitleaks)")
# Gleiche Liste wie der Job «Risiko-Pfade» in ci-pr.yml
risk_paths='^(src/main/resources/db/|\.github/|\.claude/|\.agents/|CLAUDE\.md$|pom\.xml$|src/main/webapp/package(-lock)?\.json$)'

die() { echo "[ERROR] ci-local: $*" >&2; exit 1; }

# --- Vorbedingungen: genau der gepushte Commit des PR wird geprüft -------------------------------------------------
base=$(sed -n 's/^base-branch:[[:space:]]*//p' .claude/flow.md | head -1)
base=${base:-main}
branch=$(git symbolic-ref --quiet --short HEAD) || die "kein Branch ausgecheckt"
[ "$branch" != "$base" ] || die "auf $base gibt es keinen PR"
[ -z "$(git status --porcelain)" ] || die "uncommittete oder neue Dateien; erst committen und pushen"
git fetch -q origin "$base" "$branch" || die "git fetch fehlgeschlagen"
sha=$(git rev-parse HEAD)
[ "$sha" = "$(git rev-parse "origin/$branch")" ] || die "HEAD ist nicht gepusht (origin/$branch weicht ab)"

repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner) || die "gh nicht angemeldet oder kein Remote"
pr_json=$(gh pr view "$branch" --json number,headRefOid,labels) || die "kein PR zu $branch"
pr=$(jq -r .number <<<"$pr_json")
[ "$(jq -r .headRefOid <<<"$pr_json")" = "$sha" ] || die "Head von PR #$pr ist nicht HEAD"

# --- Ist CI für diesen Commit gelaufen? -----------------------------------------------------------------------------
# Ein Job mit ausgeführten Schritten ist echt gelaufen; ohne Kontingent bricht GitHub Jobs ab, bevor ein Schritt läuft.
ran_red="" running="" not_started=""
for run in $(gh api "repos/$repo/actions/runs?head_sha=$sha&per_page=50" --jq '.workflow_runs[].id'); do
  jobs=$(gh api "repos/$repo/actions/runs/$run/jobs" --jq '.jobs[] | [.name, .status, (.conclusion // ""), (.steps | length)] | @tsv')
  while IFS=$'\t' read -r name status conclusion steps; do
    [ -n "$name" ] || continue
    if [ "$status" = in_progress ]; then
      running+="  $name"$'\n'
    elif [ "$status" = completed ] && [ "$steps" -gt 0 ] && [ "$conclusion" = failure ]; then
      ran_red+="  $name"$'\n'
    elif [ "$status" = completed ] && [ "$steps" -eq 0 ]; then
      not_started+="  $name"$'\n'
    fi
  done <<<"$jobs"
done
[ -z "$running" ] || die "CI läuft gerade für $sha; abwarten:"$'\n'"$running"
[ -z "$ran_red" ] || die "CI ist für $sha gelaufen und rot; Fehler beheben (/flow-fix-build), nicht lokal überstimmen:"$'\n'"$ran_red"

# --- Prüfungen ------------------------------------------------------------------------------------------------------
step() {
  local name="$1"; shift
  local log="$logdir/pickel-ci-local-$name.log"
  printf '%-12s ' "$name"
  if "$@" >"$log" 2>&1; then
    echo "ok"
  else
    echo "ROT  ($log)"
    grep -E '\[ERROR\]|FAIL|✗|leaks found|Finding|Drift|fehlt' "$log" | head -40
    exit 1
  fi
}

# Risiko-Pfade: wie in CI das Label setzen, damit /flow-ship die Freigabe einholt
risky=$(git diff --name-only "origin/$base...HEAD" | grep -E "$risk_paths" || true)
if [ -n "$risky" ]; then
  echo "Risiko-Pfade geändert:"; printf '  %s\n' $risky
  if ! $dry; then
    gh label create needs-human-review --color B60205 \
      --description "Risiko-Pfad geändert, Merge nur nach menschlichem Review" >/dev/null 2>&1 || true
    gh pr edit "$pr" --add-label needs-human-review >/dev/null || die "Label needs-human-review nicht gesetzt"
  fi
fi

step secrets scripts/gitleaks.sh git --redact --no-banner --log-opts="origin/$base..HEAD" .

# Das Label zum Übersteuern setzt nur ein Mensch; es gilt hier wie im CI-Job «Spec-Drift»
override=$(printf 'spec-%s' unchanged)
if jq -e --arg l "$override" '.labels | map(.name) | index($l)' <<<"$pr_json" >/dev/null; then
  echo "drift        übersteuert mit Label $override"
else
  step drift bash .claude/flow/scripts/check-story-drift.sh
fi

# Unit Tests und Integration + Smoke samt Image-Build
step gate scripts/gate.sh

# --- Melden ---------------------------------------------------------------------------------------------------------
if $dry; then
  echo "Lokal grün (--dry-run, nichts gemeldet)."
  exit 0
fi
# Während der Prüfungen könnte jemand gepusht haben; gemeldet wird nur der geprüfte Commit, wenn er noch Head ist
[ "$(gh pr view "$pr" --json headRefOid --jq .headRefOid)" = "$sha" ] || die "PR #$pr hat einen neueren Head; neu starten"
for ctx in "${checks[@]}"; do
  gh api -X POST "repos/$repo/statuses/$sha" -f state=success -f context="$ctx" \
    -f description="lokal geprüft: scripts/ci-local.sh" >/dev/null || die "Status «$ctx» nicht gemeldet"
done
echo "Lokal grün; Status für PR #$pr ($sha) gemeldet: ${checks[*]}"
if [ -n "$not_started" ]; then
  echo "Hinweis: CI-Jobs ohne Kontingent abgebrochen; ihre roten Checks können den Merge trotzdem blockieren:"
  printf '%s' "$not_started"
  echo "  Workflow abschalten (gh workflow disable ci-pr.yml), einen leeren Commit pushen und das Skript neu starten."
fi
