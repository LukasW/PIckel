# Chore — Massnahmen aus dem Projekt-Review vom 7. November 2026

Delta: `delta.md` (US-1: Fehlervertrag, fehlende Szenarien, Formular). Ablauf- und Build-Änderungen ohne fachliches
Verhalten.

## Aufgaben

- [x] Fehlervertrag: Bean Validation am Request entfernt, Mapper für fehlenden Inhalt, kaputtes JSON und unpassende
  Werte; jede 400 mit `{ "message": … }` (ADR 0003)
- [x] Kein `Location`-Header mehr bei 201 (kein Endpunkt für ein einzelnes PI)
- [x] US-1: Szenarien für alle AK, «Leerer Name» über REST statt `@Unit`, Story im Format aus `flow.md` (Technische
  Notizen und Aufgaben entfernt, beides steht in ADR 0001 und arc42), `CucumberIT` leert die Tabelle je Szenario
- [x] Frontend: Sortierung nur noch auf dem Server, Neuladen nach dem Anlegen; Feldfehler, Name aus Leerzeichen
  abgelehnt, Daten als TT.MM.JJJJ
- [x] Hooks fail-closed ohne `jq`; `guard-labels.sh` gegen `--method=DELETE`, `PUT …/labels`, `DELETE …/labels`,
  GraphQL; Testskript `test-guard-labels.sh` in Gate und CI
- [x] Nachtlauf: `.claude/settings.night.json` (keine Edits an Leitplanken und Pipeline, kein `npx`/`npm install`),
  `gh pr merge` nur in der Phase `ship`; Token-Empfehlung in `flow.md`
- [x] Devcontainer: `jq` und `core.hooksPath`
- [x] `Dockerfile.jvm` bereinigt, Image-Build in `ci-pr.yml`
- [x] `npm audit` in Gate und CI, Renovate mit OSV-Schwachstellenwarnungen, Prettier auch für CSS
- [x] PIT mit `STRONGER`
- [x] Branch Protection auf `main` (Repository dafür öffentlich gemacht) und `guard-push.sh` mit Test in Gate und
  CI (`notes.md`)
- [ ] Regelwerk nach der zweiten Story ausdünnen — erst dann möglich

## Test-Strategie

- Neue REST-Szenarien zuerst rot gesehen (400 ohne `message`), danach grün
- Vitest: Feldfehler, Leerzeichen-Name, Datumsformat, Neuladen nach dem Anlegen zuerst rot, danach grün
- `test-guard-labels.sh`: 12 Umgehungen abgelehnt, 5 erlaubte Befehle durch, ohne `jq` Exit 2
- `test-guard-push.sh`: 15 Umgehungen abgelehnt, 6 erlaubte Pushes durch
- PIT: 17 Mutanten mit `STRONGER` (vorher 11), alle getötet
- `scripts/gate.sh` komplett grün, Image-Build lokal mit Podman
