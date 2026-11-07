# Chore — Ablauf ohne GitHub Actions

Delta: keines — Ablauf und Build, kein fachliches Verhalten.

## Aufgaben

- [x] `scripts/ci-local.sh`: Gitleaks, Spec-Drift, `scripts/gate.sh`; Risiko-Pfad-Label; bei Grün die vier
  Pflicht-Checks als Commit-Status; Weigerung bei unsauberem oder ungepushtem Stand und bei echt gelaufener CI
- [x] `scripts/gate.sh`: Image-Build wie im Job «Integration + Smoke»
- [x] `guard-labels.sh`: Commit-Status nur über das Skript, Fälle in `test-guard-labels.sh`
- [x] `/flow-ship`, Nachtlauf, `flow.md` → «Ohne GitHub Actions», ADR 0004
- [ ] `.claude/settings.json`: `Bash(scripts/ci-local.sh:*)` erlauben (Mensch)
- [ ] Branch Protection: Pflicht-Checks ohne vorgeschriebene Quelle (Mensch, nach Freigabe)

## Test-Strategie

- `test-guard-labels.sh`: vier Wege, einen Status zu setzen, abgelehnt; Lesen und das Skript durch
- `scripts/ci-local.sh --dry-run` auf diesem Branch
