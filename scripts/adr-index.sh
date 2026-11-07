#!/usr/bin/env bash
# Erzeugt docs/adr/README.md, den Index der Architekturentscheide, aus den Köpfen der ADR-Dateien. Agenten lesen
# zuerst den Index und öffnen nur die passenden ADRs; ein von Hand gepflegter Index veraltet (arc42 → Abschnitt 9).
#
#   scripts/adr-index.sh          Index neu schreiben
#   scripts/adr-index.sh --check  nur prüfen: Exit 1, wenn eine ADR den Kopf verletzt oder der Index veraltet ist
#
# Erwarteter Kopf jeder Datei docs/adr/NNNN-<slug>.md:
#   # ADR NNNN: <Titel>
#   (Leerzeile)
#   **Status:** <Vorgeschlagen | Angenommen | Verworfen | Ersetzt durch ADR NNNN>
#   **Datum:** JJJJ-MM-TT
set -uo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
dir="$root/docs/adr"
index="$dir/README.md"
check=false
[ "${1:-}" = "--check" ] && check=true
errors=0

fail() { echo "[ERROR] $1" >&2; errors=$((errors + 1)); }

render() {
  cat <<'EOF'
# Architekturentscheide (ADR)

<!-- Generiert von scripts/adr-index.sh, nicht von Hand ändern. -->

Eine Zeile pro Entscheid. Zuerst hier den passenden Entscheid suchen, dann nur diese ADR lesen. Was eine angenommene
ADR ausschliesst, ist entschieden; eine Änderung braucht eine neue ADR, die die alte ersetzt.

| ADR | Titel | Status | Datum |
|-----|-------|--------|-------|
EOF
  for file in "$dir"/[0-9][0-9][0-9][0-9]-*.md; do
    [ -f "$file" ] || continue
    name="$(basename "$file")"
    nr="${name%%-*}"
    title_line="$(sed -n '1p' "$file")"
    status="$(sed -n 's/^\*\*Status:\*\*[[:space:]]*//p' "$file" | head -1)"
    date="$(sed -n 's/^\*\*Datum:\*\*[[:space:]]*//p' "$file" | head -1)"

    case "$title_line" in
      "# ADR $nr: "?*) title="${title_line#"# ADR $nr: "}" ;;
      *) fail "docs/adr/$name: erste Zeile muss «# ADR $nr: <Titel>» sein"; continue ;;
    esac
    [ -n "$status" ] || fail "docs/adr/$name: Zeile «**Status:** …» fehlt"
    [[ "$date" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || fail "docs/adr/$name: Zeile «**Datum:** JJJJ-MM-TT» fehlt"
    # Pipes würden die Tabelle zerreissen
    printf '| [%s](%s) | %s | %s | %s |\n' "$nr" "$name" "${title//|/\\|}" "${status//|/\\|}" "$date"
  done
  cat <<'EOF'

Neue ADR: nächste freie Nummer gegen `origin/main` prüfen, Datei `NNNN-<slug>.md` mit dem Kopf aus
`scripts/adr-index.sh` anlegen. Den Index führt der Claude-Hook `.claude/hooks/adr-index.sh` nach; ohne Claude
`scripts/adr-index.sh` ausführen. `scripts/gate.sh`, pre-commit und CI prüfen, dass er aktuell ist.
EOF
  # render läuft in einer Subshell; der Fehlerzähler kommt nur über den Exit-Code zurück
  [ "$errors" -eq 0 ]
}

numbers="$(ls "$dir" | sed -n 's/^\([0-9]\{4\}\)-.*\.md$/\1/p' | sort | uniq -d)"
[ -z "$numbers" ] || fail "docs/adr: Nummer mehrfach vergeben: $(echo $numbers)"

content="$(render)" || errors=$((errors + 1))
[ "$errors" -eq 0 ] || exit 1

if $check; then
  if [ "$content" != "$(cat "$index" 2>/dev/null)" ]; then
    echo "[ERROR] docs/adr/README.md ist veraltet; scripts/adr-index.sh ausführen" >&2
    exit 1
  fi
  echo "ADR-Index: aktuell"
else
  printf '%s\n' "$content" >"$index"
  echo "ADR-Index: docs/adr/README.md geschrieben"
fi
