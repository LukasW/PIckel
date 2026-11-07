# ADR 0004: Pflicht-Checks auch lokal erfüllbar

**Status:** Angenommen
**Datum:** 2026-11-07

## Kontext

Branch Protection auf `main` verlangt vier Checks aus `ci-pr.yml` (Unit Tests, Integration + Smoke, Spec-Drift,
Secret-Scan) und gilt auch für Admins. Ist das Kontingent für GitHub Actions aufgebraucht oder Actions gestört,
kommt kein PR mehr auf `main`, obwohl `scripts/gate.sh` lokal dieselben Prüfungen ausführt. Der Ablauf
(`/flow-task` bis `/flow-ship`) darf daran nicht hängen.

## Entscheid

- `scripts/ci-local.sh` prüft den gepushten Head eines PR lokal (Gitleaks, Spec-Drift, `scripts/gate.sh` samt
  Image-Build), setzt bei Risiko-Pfaden das Label `needs-human-review` und meldet bei Grün die vier Checks als
  Commit-Status mit denselben Namen
- Die Pflicht-Checks in der Branch Protection schreiben keine Quelle mehr vor (statt nur GitHub Actions jede App
  und Commit-Status), sonst zählt der lokale Status nicht
- Nur das Skript meldet Status: `guard-labels.sh` blockiert direkte Schreibzugriffe auf `…/statuses/…`, mit Test in
  `test-guard-labels.sh`. Das Skript weigert sich, wenn CI für den Commit echt gelaufen ist (rot oder aktiv)

## Konsequenzen

- Wer Schreibrechte hat, könnte einen Status auch ausserhalb von Claude setzen; das ist in einem Ein-Personen-Repo
  dasselbe Vertrauen wie das Übersteuer-Label beim Spec-Drift
- Lokal heisst: auf dieser Maschine, mit ihren Versionen von Java, Node und Podman. Abweichungen zu `ubuntu-latest`
  fallen erst auf, wenn CI wieder läuft
- Bricht GitHub Jobs ohne Kontingent ab, stehen rote Checks am Commit; dann den Workflow abschalten und neu pushen
  (`.claude/flow.md` → «Ohne GitHub Actions»)
- Neue Pflicht-Checks in `ci-pr.yml` brauchen ihre lokale Entsprechung in `ci-local.sh`
