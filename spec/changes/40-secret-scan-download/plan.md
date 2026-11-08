# Task #40 — Secret-Scan in CI unabhängig vom anonymen Release-Download

Delta: keines — CI und Skripte, kein fachliches Verhalten einer Story betroffen

## Übersicht

`gitleaks/gitleaks-action` lädt das Gitleaks-Binary anonym über `github.com/…/releases/download/…`. Seit 2026-11-08
antwortet GitHub darauf mit HTTP 504 (Runner und Devcontainer gleich), der Pflicht-Check «Secret-Scan (Gitleaks)»
scheitert, ohne zu scannen (PR #38, Run 42470419997). Angemeldet über die API kommt dasselbe Asset.

Neu lädt ein eigenes Skript `scripts/gitleaks.sh` das Binary in einer festen Version angemeldet mit
`gh release download`, prüft die SHA-256-Prüfsumme und führt Gitleaks aus. CI und `scripts/ci-local.sh` nutzen
dasselbe Skript; damit läuft der lokale Ersatz auch im Devcontainer (arm64) ohne vorinstalliertes Gitleaks.

## Zu erstellende Dateien

- [x] `scripts/gitleaks.sh` — Download (angemeldet, Version 8.24.3, Prüfsumme je Plattform), Zwischenablage, `exec`

## Zu ändernde Dateien

- [x] `.github/workflows/ci-pr.yml` — Job «Secret-Scan (Gitleaks)»: Skript statt Action, Commits `base.sha..head.sha`
  des PR, Recht `pull-requests: write` entfällt
- [x] `scripts/ci-local.sh` — Skript statt vorausgesetztem `gitleaks`
- [x] `.claude/flow.md` — Tabelle «Ohne GitHub Actions»: Verweis auf das Skript statt `brew install gitleaks`

## Architektur-Entscheide

- **Eigenes Skript statt Action:** Die Action bietet keinen angemeldeten Download. Verworfen: Action mit Retry (hilft
  nicht bei längerem Ausfall); Docker-Image `zricethezav/gitleaks` (anderer Bezugsweg mit eigenem Ausfallrisiko,
  im Devcontainer ohne Docker-in-Docker nicht für `ci-local.sh` nutzbar); Binary ins Repository (Binärdatei je
  Plattform, Update von Hand ohne Prüfung)
- **Prüfsumme im Skript:** hält das Binary fest wie der Commit-Pin die Action; ein Versionswechsel ändert Version
  und Prüfsummen zusammen
- **Gescannter Bereich:** `base.sha..head.sha` des PR, wie die Action im PR-Kontext
- **Ohne PR-Kommentare:** Die Action kommentierte Funde am PR; der Fund steht jetzt im Job-Log (`--redact`). Dafür
  braucht der Job nur noch `contents: read`
- Der Pre-Commit-Hook (`.githooks/pre-commit`) bleibt beim optional installierten Gitleaks: Ein Hook lädt nichts aus
  dem Netz

## Aufgaben

- [x] 1. `scripts/gitleaks.sh` — erledigt, wenn es frisch lädt, aus der Ablage läuft, bei falscher Prüfsumme
  abbricht und einen Fund mit Exit ≠ 0 meldet (von Hand, siehe Test-Strategie)
- [x] 2. Workflow und `ci-local.sh` umstellen
- [ ] 3. CI des PR: Job «Secret-Scan (Gitleaks)» grün — erledigt, wenn er im PR durchläuft (der PR prüft sich mit
  seiner eigenen Fassung des Workflows)
- [x] 4. `scripts/gate.sh --fast` grün

## Test-Strategie

Kein automatischer Test: Das Verhalten hängt am Netz und an GitHub; ein Test dafür wäre selbst flackernd. Von Hand
im Devcontainer (arm64) geprüft, Ergebnis in `notes.md`:

- frische Ablage → Download, Scan der letzten fünf Commits, «no leaks found», Exit 0
- zweiter Lauf → aus der Ablage, ohne Download
- falsche Prüfsumme → Abbruch mit Meldung, Exit 1, keine Ablage angelegt
- Wegwerf-Repository mit einem Token → «leaks found: 1», Exit 1

Der Beleg für CI ist der Job im PR selbst.

## Offene Fragen

keine
