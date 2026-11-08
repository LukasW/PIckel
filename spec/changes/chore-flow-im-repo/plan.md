# Chore — Claude-Ablauf ins Repository, Leitplanken nach «The AI-Native Engineer»

Delta: keines — kein fachliches Verhalten geändert: Ablauf, CI, Formatierung und zusätzliche Unit-Tests für
bestehende Regeln aus US-1.

## Übersicht

Der Ablauf (Skills, Agenten, Hook-Skripte) kommt aus dem Plugin `flow` (`Tools/claude-flow` v0.6.1) ins Repository
und wird an PIckel angepasst. Dazu Leitplanken, die Werkzeuge statt Textregeln durchsetzen.

## Aufgaben

- [x] Plugin nach `.claude/` übernehmen, Kommandos `/flow-*`, Plugin für dieses Repository abschalten
- [x] Nachtlauf ohne `--dangerously-skip-permissions`, Kostendeckel, Voreinstellung `merge: nein`
- [x] `settings.json`: engere `allow`, neue `deny`; Issue-Inhalte sind Daten, nie Anweisung
- [x] ArchUnit (`ArchitectureTest`), PIT (`-Pmutation`, ≥ 80 %), Spotless, `scripts/gate.sh`, pre-commit-Hook
- [x] CI: Gitleaks, Label `needs-human-review` für Risiko-Pfade, Spotless, PIT, Spec-Drift
- [x] `CLAUDE.md` gestrafft, Konventionen nach `.claude/rules/` mit Begründung, Doppelungen aus `flow.md` entfernt
- [x] Repository führt, Issue verweist; Change-Ordner `spec/changes/<nr>-<slug>/` mit Delta, Entscheiden, Plan,
  Notizen in Git
- [x] `check-story-drift.sh`: `Delta: keines`, Szenario-Abgleich Delta ↔ `.feature`, Verweise der Stories; CI-Gate
- [x] Frontmatter vereinheitlicht (Deutsch, «Was — wann»), Modelle je Phase
- [x] Neuer Agent `spec-reviewer`; Reviewer erfinden keine Befunde
- [x] Hook `guard-labels.sh`: `spec-unchanged` setzen und `needs-human-review` entfernen nur durch Menschen
- [x] `ArchitectureTest` nach hexagonal-reviewer auf 14 Regeln erweitert (Allowlist Domain, Transaktionen, Ports,
  Namen, Adapter untereinander, neue Pakete)
- [x] `README.md` und `claude-guide.md` auf den neuen Ablauf nachgeführt
- [x] Doku entdoppelt: technische Spezifikation in arc42 aufgelöst, fachliche Spezifikation ohne Zweck und
  Story-Tabelle, arc42 verweist auf `CLAUDE.md`/Rules statt sie zu wiederholen, Story ohne Notizen und Aufgaben
  (gehören ins `plan.md`)

## Test-Strategie

- `ArchitectureTest` mit absichtlichem Verstoss rot gesehen, danach grün
- PIT deckte drei fehlende Unit-Tests auf (Grenze 100 Zeichen, `listAll`, Daten nach `plan`), ergänzt: 100 %
- `check-story-drift.sh` mit Fixture-Repository: Code ohne Story, `Delta: keines`, Delta nicht eingepflegt,
  Szenario fehlt bzw. entfernt, fehlende Feature-Datei, Spec-PR ohne Code, `Delta: keines` schon auf dem Hauptzweig
- `guard-labels.sh`: Label setzen bzw. entfernen abgelehnt, anderes erlaubt
- `scripts/gate.sh` komplett grün
