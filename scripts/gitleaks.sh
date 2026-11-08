#!/usr/bin/env bash
# Führt Gitleaks in der festgelegten Version aus, für den CI-Job «Secret-Scan (Gitleaks)» und scripts/ci-local.sh.
#
#   scripts/gitleaks.sh <gitleaks-argumente>     z. B. scripts/gitleaks.sh git --redact --log-opts="A..B" .
#
# Das Binary kommt angemeldet über `gh release download` (GH_TOKEN bzw. gh-Anmeldung), nicht über den anonymen
# Release-Download: Fällt dieser aus (HTTP 504, Issue #40), scheiterte der Pflicht-Check, ohne zu scannen. Die
# Prüfsumme hält das Binary fest; ein abweichendes Archiv bricht ab. Zwischenablage: ${GITLEAKS_CACHE}, sonst
# ~/.cache/pikel/gitleaks/<version>.
set -euo pipefail

version=8.24.3
# Prüfsummen aus gitleaks_8.24.3_checksums.txt des Releases
declare -A sha256=(
  [linux_x64]=9991e0b2903da4c8f6122b5c3186448b927a5da4deef1fe45271c3793f4ee29c
  [linux_arm64]=5f2edbe1f49f7b920f9e06e90759947d3c5dfc16f752fb93aaafc17e9d14cf07
  [darwin_x64]=41c44ae8ad1d6eef57d4526ad0fd67d8129eee9a856f55c2b3b9395fd3d9ec0f
  [darwin_arm64]=b90f13bb8c90ab72083d9b0c842e39dafb82c0e5c3f872f407366b7a58909013
)

die() { echo "[ERROR] gitleaks.sh: $*" >&2; exit 1; }

case "$(uname -s)" in
  Linux) os=linux ;;
  Darwin) os=darwin ;;
  *) die "Betriebssystem $(uname -s) nicht unterstützt" ;;
esac
case "$(uname -m)" in
  x86_64 | amd64) arch=x64 ;;
  aarch64 | arm64) arch=arm64 ;;
  *) die "Architektur $(uname -m) nicht unterstützt" ;;
esac
platform="${os}_${arch}"

dir="${GITLEAKS_CACHE:-$HOME/.cache/pikel/gitleaks/$version}"
bin="$dir/gitleaks"

if [ ! -x "$bin" ]; then
  command -v gh >/dev/null || die "gh fehlt; es lädt das Binary angemeldet"
  asset="gitleaks_${version}_${platform}.tar.gz"
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  gh release download "v$version" --repo gitleaks/gitleaks --pattern "$asset" --dir "$tmp" ||
    die "Download von $asset fehlgeschlagen"
  actual=$( (sha256sum "$tmp/$asset" 2>/dev/null || shasum -a 256 "$tmp/$asset") | cut -d' ' -f1)
  [ "$actual" = "${sha256[$platform]}" ] || die "Prüfsumme von $asset weicht ab: $actual"
  mkdir -p "$dir"
  tar -xzf "$tmp/$asset" -C "$dir" gitleaks
  # exec ersetzt die Shell, der EXIT-Trap liefe nicht mehr
  rm -rf "$tmp"
  trap - EXIT
fi

exec "$bin" "$@"
