# Task — Date-Picker-Symbol im Dunkelmodus unsichtbar (Issue #57)

## Übersicht

`src/main/webapp/src/styles.css` setzt bei `prefers-color-scheme: dark` dunkle Design-Tokens, deklariert aber kein
`color-scheme`. Der Browser zeichnet native Bedienelemente deshalb weiter im hellen Schema: Das Kalender-Symbol von
`input type="date"` (PI anlegen, Milestones), die Select-Pfeile und die Scrollbars bleiben dunkel auf dunklem Grund.
`color-scheme: light dark` auf `:root` erlaubt dem Browser beide Schemata, und er wählt das zur Systemeinstellung
passende.

Nur Frontend, nur globale Styles.

## Zu erstellende Dateien

- [ ] keine

## Zu ändernde Dateien

- [ ] `src/main/webapp/src/styles.css` — `:root { color-scheme: light dark; }` ausserhalb der Media Query, Kommentar
  oben um den Grund ergänzen

Delta: keines — Bugfix der Darstellung, keine Story beschreibt das Verhalten

## Architektur-Entscheide

- CSS-Eigenschaft statt `<meta name="color-scheme">` in `index.html`: Sie steht neben den Design-Tokens, an dem Ort,
  der den Dunkelmodus schon steuert. Das Meta-Tag wirkt nur früher beim Laden, das spielt bei einer SPA keine Rolle
- Keine ADR, keine Änderung an `docs/arc42.md`

## Aufgaben

- [x] 1. `color-scheme: light dark` auf `:root` in `styles.css` — erledigt, wenn `scripts/gate.sh --fast` grün ist
  und die Prüfung von Hand das Kalender-Symbol im Dunkelmodus zeigt

## Test-Strategie

- [ ] Kein automatischer Test: Vitest (jsdom) rendert keine nativen Bedienelemente, und ein Test, der nur den
  Text von `styles.css` liest, prüft den Code statt das Verhalten. E2E ist nicht eingerichtet (`CLAUDE.md` →
  «Testpyramide»)
- [ ] Prüfung von Hand: `./mvnw quarkus:dev`, Browser bzw. System auf Dunkelmodus, auf «Program Increments» und bei
  den Milestones ist das Kalender-Symbol sichtbar und öffnet den Picker; im hellen Modus unverändert

## Offene Fragen

- keine
