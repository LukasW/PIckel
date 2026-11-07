# Chore — Devcontainer und Ablauf nach US-5

Delta: keines — Entwicklungsumgebung und Ablauf, kein fachliches Verhalten.

Anlass: «Lernen» in `/flow-ship` nach #5 (PR #21).

## Aufgaben

- [x] `.devcontainer/devcontainer.json`: `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` in `containerEnv` —
  erledigt, wenn `./mvnw verify` im Devcontainer nach einem Rebuild grün ist
- [x] `/flow-implement` Schritt 14: PR-Titel im Format der Commit-Message mit Issue-Nummer statt Issue-Titel
- [x] `/flow-plan` → «Test-Strategie»: Punkte auf Ebene Integration/Akzeptanz brauchen ein Szenario im Delta
- [ ] ArchUnit-Regel gegen `UUID` als `@PathParam`: eigenes Issue (Code)

## Test-Strategie

- Von Hand nach dem Rebuild: `./mvnw verify -B -DskipFrontendTests=true` ohne manuell gesetzte Variable
  (mit derselben Variable von Hand gesetzt war `scripts/gate.sh` in #5 grün)
- Die Skill-Änderungen prüft `scripts/gate.sh` (Schritt «skills»)
