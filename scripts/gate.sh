#!/usr/bin/env bash
# Lokales Gate: dieselben Prüfungen wie CI, in einer Kette. Agent und Mensch lassen es vor jedem Push grün laufen,
# damit der Agent sein eigener erster Reviewer ist und CI nicht als Fehlersuche dient.
#
#   scripts/gate.sh          Format (Prettier, Spotless), Unit inkl. ArchUnit, Vitest, Mutation, CucumberIT, SmokeIT
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

step prettier  bash -c 'cd src/main/webapp && npm run format:check'
step spotless  ./mvnw -B spotless:check
step unit      ./mvnw -B test
step mutation  ./mvnw -B test-compile -Pmutation
if ! $fast; then
  step integration ./mvnw -B verify -DskipFrontendTests=true
fi
echo "Gate grün."
