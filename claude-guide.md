# Claude Code Guide — PIkel

Wie arbeite ich mit Claude Code in PIkel. Ergänzt `CLAUDE.md` (Architektur, Konventionen). Der Ablauf kommt aus
dem Plugin `flow` (`Tools/claude-flow`), wie in Brainy; was PIkel anders macht, steht in `.claude/flow.md`.

> **TL;DR**: `/flow:task` → `/flow:plan` (optional) → `/flow:implement` → `/flow:ship`. Alles in einem:
> `/flow:fast <beschreibung>`. Reviewer schlägt ein Hook vor.

## Setup auf einen Blick

```
Plugin flow (Tools/claude-flow, über .claude/settings.json vorgeschlagen, gepinnt auf ein Tag)
├── skills/        /flow:task, /flow:grill-me, /flow:plan, /flow:implement, /flow:ship, /flow:fast, /flow:fix-build
├── agents/        test-author (Tests vor dem Code), test-reviewer
└── scripts/       Hooks: Schweizer Schreibweise, abgeschwächte Tests, .env-Schutz

.claude/
├── flow.md                       # Projektangaben: Build, Stories, Tests, Reviewer, DoD, Anhalten
├── agents/
│   ├── hexagonal-reviewer.md     # Java: Ports/Adapters, Abhängigkeitsrichtung
│   ├── angular-signals-reviewer.md  # Angular: Signals, OnPush, Control Flow, Tokens
│   └── bdd-cucumber-author.md    # Gherkin und Tag-Routing
├── hooks/
│   └── reviewer-reminder.sh      # PostToolUse → schlägt Reviewer vor
├── settings.json                 # Permissions, Hook, Plugin (eingecheckt)
└── settings.local.json           # persönliche Overrides (ignoriert)
```

**Einrichten:** Beim ersten Start schlägt Claude Code den Marketplace `grudligstrasse` und das Plugin vor. Von Hand:
`/plugin marketplace add https://scm.grudligstrasse.ch/Tools/claude-flow.git`, dann
`/plugin install flow@grudligstrasse`. Für Issues und PRs `gh auth login` (oder GitHub-MCP).

## Wo steht was

| Frage | Datei |
|-------|-------|
| Was soll PIkel fachlich können? | `spec/fachliche-spezifikation.md`, `spec/user-stories/` |
| Wie ist es gebaut? | `spec/technische-spezifikation.md`, `docs/adr/` |
| Welche Regeln gelten im Code? | `CLAUDE.md` |
| Wie wird ein Issue umgesetzt? | `plan/` (aus `/flow:plan`, lokal) |
| Was macht der Ablauf hier anders? | `.claude/flow.md` |

## Standard-Workflow

| Situation | Kommando |
|-----------|----------|
| Nur eine Idee | `/flow:task <beschreibung>` → `/flow:implement <nr>` |
| Issue vorhanden | `/flow:implement 12` (grössere Features vorher `/flow:plan 12`) |
| Issue unscharf | `/flow:grill-me 12`, dann `/flow:plan 12` |
| Kleine Änderung | `/flow:fast <beschreibung>` |
| PR mergen und aufräumen | `/flow:ship` |
| Build rot | `/flow:fix-build` |

Eine Session pro Issue: nach `/flow:plan` und nach `/flow:ship` mit `/clear` neu beginnen.

## Reviewer-Matrix

| Pfad | Reviewer | Prüft |
|------|----------|-------|
| `src/main/java/ch/grudligstrasse/pikel/**` | `hexagonal-reviewer` | Ports/Adapters, Abhängigkeitsrichtung, JPA-Platzierung |
| `src/main/webapp/src/app/**` | `angular-signals-reviewer` | Signals, OnPush, `@if`/`@for`, Tokens, Ports im Frontend |
| `*.feature` | `bdd-cucumber-author` | Tag-Routing, Gherkin |
| `src/test/**`, `*.spec.ts` | `flow:test-reviewer` | Absicht aus der Spezifikation, Fehlerfinder, Abschwächungen |

Aufruf: «Review die letzten Änderungen mit hexagonal-reviewer und angular-signals-reviewer.»

## Weiterentwicklung des Setups

- Neue Reviewer nach dem Muster in `.claude/agents/` (Frontmatter `name`, `description`, `tools`, `model`, dann
  Regeln und Report-Format); Pfad-Regel in `reviewer-reminder.sh` und `.claude/flow.md` → «Reviewer» nachziehen
- Sobald Auth kommt: `auth-security-reviewer` aus Brainy übernehmen
- Sobald ein Browser-Test nötig ist: Cucumber.js + Playwright für `@E2E` wie in Brainy einrichten

## Was NICHT tun

- ❌ Fachlogik in REST-Resources oder Angular-Komponenten
- ❌ Framework-Imports in `domain/`
- ❌ `HttpClient` ausserhalb von `core/api/`
- ❌ Tailwind-Paletten statt Tokens
- ❌ `ß` — `ss` (der Hook meldet es)
- ❌ Einen Test abschwächen, damit der Build grün wird
