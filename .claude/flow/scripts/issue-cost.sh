#!/usr/bin/env bash
# Kosten der Umsetzung eines Issues aus den lokalen Transkripten von Claude Code, als Markdown für einen
# Issue-Kommentar. Zugeordnet wird über den Git-Branch jeder Antwort (feature/<nr>-…, hotfix/<nr>-…, auto/<nr> des
# Nachtlaufs), deshalb zählen alle Sessions und Subagenten über /clear hinweg, die Planung auf dem Hauptzweig nicht.
#
# Claude Code schreibt keinen Betrag ins Transkript, nur Token je Anfrage. Der Betrag ist darum eine Schätzung zu
# API-Listenpreisen; mit einem Abo ist er ein theoretischer Wert. Websuche und andere Server-Tools fehlen.
#
# Aufruf: issue-cost.sh <issue-nr>   Exit 0 mit Markdown auf stdout, 1 ohne Treffer, 2 bei falschem Aufruf.
# Umgebung: FLOW_CLAUDE_PROJECTS (Ablage der Transkripte, ${CLAUDE_CONFIG_DIR:-~/.claude}/projects)
set -uo pipefail

nr=${1:-}
case "$nr" in '' | *[!0-9]*) echo "Aufruf: issue-cost.sh <issue-nr>" >&2; exit 2 ;; esac
dir=${FLOW_CLAUDE_PROJECTS:-${CLAUDE_CONFIG_DIR:-$HOME/.claude}/projects}
[ -d "$dir" ] || { echo "issue-cost: keine Transkripte unter $dir" >&2; exit 1; }

branch_re="^[a-z]+/$nr(-.*)?\$"

# Preise in USD je Million Token, Stand 2026-10 (Anthropic API): Input, Output, Cache-Read. Cache-Write kostet das
# 1,25-Fache des Inputs (5 Minuten) bzw. das Doppelte (1 Stunde), Fast Mode das Doppelte von allem. Neue Modelle hier
# nachtragen; ein fehlendes erscheint im Kommentar ohne Preis.
read -r -d '' prices <<'EOF'
{
  "claude-fable-5-1": [10, 50, 0.25], "claude-mythos-5-1": [10, 50, 0.25],
  "claude-fable-5": [10, 50, 1], "claude-mythos-5": [10, 50, 1],
  "claude-opus-5-5": [4, 20, 0.20],
  "claude-opus-5": [5, 25, 0.50], "claude-opus-4-8": [5, 25, 0.50], "claude-opus-4-7": [5, 25, 0.50],
  "claude-opus-4-6": [5, 25, 0.50],
  "claude-sonnet-5-5": [2, 10, 0.20], "claude-sonnet-5": [2, 10, 0.20], "claude-sonnet-4-6": [3, 15, 0.30],
  "claude-haiku-5-5": [0.10, 0.50, 0.01], "claude-haiku-4-5": [1, 5, 0.10]
}
EOF
# Claude Haiku 5.5 kostet über 100K Prompt-Tokens je Anfrage das Fünffache
haiku_tier='{"model": "claude-haiku-5-5", "above": 100000, "factor": 5}'

files=$(grep -rlE --include='*.jsonl' "\"gitBranch\":\"[a-z]+/$nr[-\"]" "$dir" 2>/dev/null)
[ -n "$files" ] || { echo "issue-cost: keine Transkripte zu Issue #$nr unter $dir" >&2; exit 1; }

# jq liefert TSV: «M» je Modell (Anfragen, Token, Kosten oder «?»), «S» einmal (Sessions, Zeitraum, Branches)
rows=$(printf '%s\n' "$files" | tr '\n' '\0' | xargs -0 cat | jq -R -r -n \
  --arg re "$branch_re" --argjson prices "$prices" --argjson tier "$haiku_tier" '
  [inputs | fromjson? | select(type == "object" and .type == "assistant" and (.message.usage | type) == "object")
    | select((.gitBranch // "") | test($re)) | select(.message.model != "<synthetic>")]
  # Claude Code schreibt eine Antwort mit mehreren Blöcken als mehrere Zeilen mit derselben Anfrage
  | unique_by(.requestId // .message.id) as $req
  | ($req | map(
      .message.usage as $u
      | (.message.model | sub("-[0-9]{8}$"; "")) as $m
      | ($u.cache_creation.ephemeral_5m_input_tokens // 0) as $w5
      | ($u.cache_creation.ephemeral_1h_input_tokens // (($u.cache_creation_input_tokens // 0) - $w5)) as $w1
      | {m: $m, i: ($u.input_tokens // 0), o: ($u.output_tokens // 0), w: ($w5 + $w1),
         r: ($u.cache_read_input_tokens // 0), w5: $w5, w1: $w1, fast: ($u.speed == "fast")}
      | . + {cost: ($prices[.m] as $p
          | if $p == null then null else
              (if .m == $tier.model and (.i + .w + .r) > $tier.above then $tier.factor else 1 end)
              * (if .fast then 2 else 1 end)
              * (.i * $p[0] + .o * $p[1] + .r * $p[2] + .w5 * $p[0] * 1.25 + .w1 * $p[0] * 2) / 1000000
            end)})
    | group_by(.m)
    | sort_by(-(map(.cost // 0) | add))
    | .[]
    | ["M", .[0].m, length, (map(.i) | add), (map(.o) | add), (map(.w) | add), (map(.r) | add),
       (if .[0].cost == null then "?" else (map(.cost) | add) end)]
    | @tsv),
    (["S", ($req | map(.sessionId) | unique | length),
      ($req | map(.timestamp // empty) | min // "" | .[0:10]),
      ($req | map(.timestamp // empty) | max // "" | .[0:10]),
      ($req | map(.gitBranch) | unique | join(", "))] | @tsv)
')
[ -n "$(printf '%s\n' "$rows" | grep '^M')" ] || { echo "issue-cost: keine Anfragen zu Issue #$nr" >&2; exit 1; }

printf '%s\n' "$rows" | awk -F'\t' -v nr="$nr" '
  # Tausendertrennzeichen nach Schweizer Schreibweise
  function n(x,   s, out) {
    s = sprintf("%d", x); out = ""
    while (length(s) > 3) { out = "\047" substr(s, length(s) - 2) out; s = substr(s, 1, length(s) - 3) }
    return s out
  }
  $1 == "M" {
    rows = rows sprintf("| %s | %d | %s | %s | %s | %s | %s |\n", $2, $3, n($4), n($5), n($6), n($7),
                        $8 == "?" ? "?" : sprintf("%.2f", $8))
    count += $3; total += ($8 == "?" ? 0 : $8)
    if ($8 == "?") unknown = unknown (unknown == "" ? "" : ", ") $2
  }
  $1 == "S" { sessions = $2; from = $3; to = $4; branches = $5 }
  END {
    print "### Kosten der Umsetzung"
    print ""
    print "| Modell | Anfragen | Input | Output | Cache-Write | Cache-Read | Kosten (USD) |"
    print "|---|---:|---:|---:|---:|---:|---:|"
    printf "%s", rows
    printf "| **Total** | **%d** | | | | | **%.2f** |\n", count, total
    print ""
    printf "%d Sessions inkl. Subagenten auf `%s`, %s", sessions, branches, from
    if (to != from) printf " bis %s", to
    print "."
    print "Schätzung aus den Token der lokalen Transkripte zu API-Listenpreisen (Stand 2026-10), ohne Server-Tools;"
    print "mit einem Abo ein theoretischer Wert. Planung auf dem Hauptzweig ist nicht enthalten."
    if (unknown != "") print "Ohne Preis (in `issue-cost.sh` nachtragen): " unknown "."
  }'
