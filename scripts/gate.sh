#!/usr/bin/env bash
# Lokales Gate: dieselben Prüfungen wie CI, in einer Kette. Agent und Mensch lassen es vor jedem Push grün laufen,
# damit der Agent sein eigener erster Reviewer ist und CI nicht als Fehlersuche dient.
#
#   scripts/gate.sh          Skills (agentskills.io), ADR-Index, Format (Prettier, Spotless), npm audit, Unit inkl.
#                            ArchUnit, Vitest, Mutation, CucumberIT, SmokeIT, Container-Image (Podman oder Docker)
#   scripts/gate.sh --fast   ohne Integration (keine Container nötig), wie der Unit-Job in CI
#
# Ausgabe je Schritt in ${TMPDIR:-/tmp}/pikel-gate-<schritt>.log; angezeigt werden nur Fehlerzeilen.
set -uo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root" || exit 1
fast=false
[ "${1:-}" = "--fast" ] && fast=true
logdir="${TMPDIR:-/tmp}"

step() {
  local name="$1"; shift
  local log="$logdir/pikel-gate-$name.log"
  printf '%-12s ' "$name"
  if "$@" >"$log" 2>&1; then
    echo "ok"
  else
    echo "ROT  ($log)"
    grep -E '\[ERROR\]|FAIL|✗|Tests run:.*(Failures|Errors): [1-9]|below threshold|Violation|\[warn\]' "$log" | head -40
    exit 1
  fi
}

step skills    scripts/check-skills.sh
step adrs      scripts/adr-index.sh --check
step guards    bash -c 'bash .claude/flow/scripts/test-guard-labels.sh && bash .claude/flow/scripts/test-guard-push.sh'
step prettier  bash -c 'cd src/main/webapp && npm run format:check'
step audit     bash -c 'cd src/main/webapp && npm audit --omit=dev --audit-level=high'
step spotless  ./mvnw -B spotless:check
step unit      ./mvnw -B test
step mutation  ./mvnw -B test-compile -Pmutation
if ! $fast; then
  step integration ./mvnw -B verify -DskipFrontendTests=true
  # Wie der Job «Integration + Smoke»: ein kaputtes Dockerfile fällt vor dem ersten Deployment auf
  engine=$(command -v podman || command -v docker) || { echo "image        ROT  (weder podman noch docker)"; exit 1; }
  step image "$engine" build -f src/main/docker/Dockerfile.jvm -t pikel-jvm:gate .
fi
echo "Gate grün."
