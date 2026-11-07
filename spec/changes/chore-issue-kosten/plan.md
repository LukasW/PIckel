# Chore — Kosten der Umsetzung am Issue

Delta: keines — Ablauf, kein fachliches Verhalten.

## Aufgaben

- [x] `.claude/flow/scripts/issue-cost.sh <nr>`: Token aller Sessions und Subagenten auf `*/<nr>-*` bzw. `auto/<nr>`
  aus den lokalen Transkripten, je Anfrage einmal, zu API-Listenpreisen; Markdown für den Issue-Kommentar
- [x] `/flow-ship` Schritt 5: Kommentar mit `gh issue comment` (nicht im Nachtlauf)
- [x] `night-batch.sh`: exakte Kosten aus `total_cost_usd` aller Phasen und Versuche als Kommentar, in `record`
- [x] `test-issue-cost.sh` in `scripts/gate.sh` und CI
- [ ] `.claude/settings.json`: `Bash(.claude/flow/scripts/issue-cost.sh:*)` erlauben (Mensch)

## Test-Strategie

- `test-issue-cost.sh`: synthetische Transkripte — Branch-Zuordnung (kein Treffer bei #420 für #42, nicht auf dem
  Hauptzweig), Subagenten, doppelte Zeilen derselben Anfrage, Preise je Token-Art, Fast Mode, Haiku-5.5-Stufe,
  unbekanntes Modell, Exit-Codes
- `post_cost` von Hand mit gefälschtem `gh` (Versuch ohne Ergebnis zählt 0, fremdes Issue nicht)
- `issue-cost.sh 5` gegen die echten Transkripte von #5

## Offen

- Preistabelle im Skript bei neuen Modellen von Hand nachführen; ein fehlendes Modell erscheint im Kommentar
  «ohne Preis»
- Transkripte liegen unter `~/.claude`; ohne Volume im Devcontainer gehen sie beim Neuaufbau verloren
