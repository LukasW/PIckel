# Architekturentscheide (ADR)

<!-- Generiert von scripts/adr-index.sh, nicht von Hand ändern. -->

Eine Zeile pro Entscheid. Zuerst hier den passenden Entscheid suchen, dann nur diese ADR lesen. Was eine angenommene
ADR ausschliesst, ist entschieden; eine Änderung braucht eine neue ADR, die die alte ersetzt.

| ADR | Titel | Status | Datum |
|-----|-------|--------|-------|
| [0001](0001-walking-skeleton.md) | Walking Skeleton mit dem Stack von Brainy | Angenommen | 2026-11-06 |
| [0002](0002-agent-skills-standard.md) | Skills nach dem Agent-Skills-Standard | Angenommen | 2026-11-07 |
| [0003](0003-fehlervertrag.md) | Einheitlicher Fehlervertrag `{ "message": … }`, Problem Details später | Angenommen | 2026-11-07 |
| [0004](0004-ci-lokal.md) | Pflicht-Checks auch lokal erfüllbar | Angenommen | 2026-11-07 |

Neue ADR: nächste freie Nummer gegen `origin/main` prüfen, Datei `NNNN-<slug>.md` mit dem Kopf aus
`scripts/adr-index.sh` anlegen. Den Index führt der Claude-Hook `.claude/hooks/adr-index.sh` nach; ohne Claude
`scripts/adr-index.sh` ausführen. `scripts/gate.sh`, pre-commit und CI prüfen, dass er aktuell ist.
