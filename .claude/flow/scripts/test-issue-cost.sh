#!/usr/bin/env bash
# Prüft issue-cost.sh gegen synthetische Transkripte: Zuordnung über den Branch, Subagenten, doppelte Zeilen derselben
# Anfrage, Preise je Token-Art. Läuft in scripts/gate.sh und CI.
set -u
root="$(cd "$(dirname "$0")/../../.." && pwd)"
S="$root/.claude/flow/scripts/issue-cost.sh"
fails=0
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# line <session> <branch> <request-id> <modell> <input> <output> <cache-write-5m> <cache-write-1h> <cache-read> [speed]
line() {
  jq -cn --arg s "$1" --arg b "$2" --arg r "$3" --arg m "$4" \
    --argjson i "$5" --argjson o "$6" --argjson w5 "$7" --argjson w1 "$8" --argjson cr "$9" --arg sp "${10:-standard}" \
    '{type:"assistant", sessionId:$s, gitBranch:$b, requestId:$r, timestamp:"2026-11-07T10:00:00.000Z",
      message:{id:("msg_"+$r), model:$m, usage:{input_tokens:$i, output_tokens:$o, cache_read_input_tokens:$cr,
        cache_creation_input_tokens:($w5+$w1), cache_creation:{ephemeral_5m_input_tokens:$w5, ephemeral_1h_input_tokens:$w1},
        speed:$sp}}}'
}

check() {
  local name="$1" want="$2" got="$3"
  if printf '%s' "$got" | grep -qF -- "$want"; then echo "ok   $name"; else
    echo "FAIL $name: erwartet «$want»"; printf '%s\n' "$got" | sed 's/^/     /'; fails=$((fails + 1)); fi
}

p="$tmp/projects/-repo"
mkdir -p "$p/sess-a/subagents" "$tmp/projects/-wt-42"
{
  # Opus 5.5: 1 Mio. Input ($4) + 1 Mio. Output ($20); dieselbe Anfrage steht zweimal im Transkript
  line sess-a feature/42-pi-anlegen r1 claude-opus-5-5 1000000 1000000 0 0 0
  line sess-a feature/42-pi-anlegen r1 claude-opus-5-5 1000000 1000000 0 0 0
  # Opus 5.5: 1 Mio. Cache-Read ($0.20) + 1 Mio. Cache-Write 1 h ($8) + 1 Mio. Cache-Write 5 min ($5)
  line sess-a feature/42-pi-anlegen r2 claude-opus-5-5 0 0 1000000 1000000 1000000
  # Anderes Issue mit gleichem Präfix und der Hauptzweig zählen nicht
  line sess-a feature/420-anderes r3 claude-opus-5-5 9000000 0 0 0 0
  line sess-a main r4 claude-opus-5-5 9000000 0 0 0 0
  # Platzhalter-Antworten von Claude Code ohne API-Aufruf
  line sess-a feature/42-pi-anlegen r5 '<synthetic>' 0 0 0 0 0
} >"$p/sess-a.jsonl"
# Subagent: Sonnet 5.5, 1 Mio. Output ($10)
line sess-a feature/42-pi-anlegen r6 claude-sonnet-5-5 0 1000000 0 0 0 >"$p/sess-a/subagents/agent-1.jsonl"
{
  # Zweite Session im Worktree des Nachtlaufs; Fast Mode kostet das Doppelte (1 Mio. Input Opus 5.5: $8)
  line sess-b auto/42 r7 claude-opus-5-5 1000000 0 0 0 0 fast
  # Haiku 5.5 über 100K Prompt-Tokens: höhere Stufe, 200K Input zu $0.50/Mio. = $0.10
  line sess-b hotfix/42-pi-anlegen r8 claude-haiku-5-5 200000 0 0 0 0
  # Unbekanntes Modell: Token zählen, Kosten nicht
  line sess-b feature/42-pi-anlegen r9 claude-zukunft-9 1000 0 0 0 0
  echo 'keine gültige JSON-Zeile'
} >"$tmp/projects/-wt-42/sess-b.jsonl"

out=$(FLOW_CLAUDE_PROJECTS="$tmp/projects" bash "$S" 42 2>&1)
rc=$?
check "Exit 0 bei Treffern" "rc=0;" "rc=$rc;"
check "Opus 5.5: doppelte Zeile einmal, Cache-Preise, Fast Mode" "| claude-opus-5-5 | 3 | 2'000'000 | 1'000'000 | 2'000'000 | 1'000'000 | 45.20 |" "$out"
check "Subagent mitgezählt" "| claude-sonnet-5-5 | 1 | 0 | 1'000'000 | 0 | 0 | 10.00 |" "$out"
check "Haiku 5.5 über 100K" "| claude-haiku-5-5 | 1 | 200'000 | 0 | 0 | 0 | 0.10 |" "$out"
check "Unbekanntes Modell ohne Preis" "| claude-zukunft-9 | 1 | 1'000 | 0 | 0 | 0 | ? |" "$out"
check "Total ohne fremde Issues" "| **Total** | **6** | | | | | **55.30** |" "$out"
check "Sessions gezählt" "2 Sessions" "$out"
check "Hinweis auf fehlenden Preis" "claude-zukunft-9" "$(printf '%s' "$out" | grep -i 'ohne Preis')"
if printf '%s' "$out" | grep -q '<synthetic>'; then echo "FAIL <synthetic> gezählt"; fails=$((fails + 1)); else echo "ok   <synthetic> ignoriert"; fi

out=$(FLOW_CLAUDE_PROJECTS="$tmp/projects" bash "$S" 7 2>&1)
rc=$?
check "Exit 1 ohne Treffer" "rc=1;" "rc=$rc;"

out=$(bash "$S" abc 2>&1)
rc=$?
check "Exit 2 bei ungültiger Nummer" "rc=2;" "rc=$rc;"

[ "$fails" = 0 ] || { echo "[ERROR] issue-cost: $fails Fälle falsch"; exit 1; }
echo "issue-cost: alle Fälle ok"
