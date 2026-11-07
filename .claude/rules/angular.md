---
paths:
  - "src/main/webapp/**"
---

# Angular-Konventionen

- **Standalone ist Default, nie `standalone: true` setzen**, keine `NgModule` — ab Angular 19 ist das Flag
  überflüssig und verwirrt nur
- **`ChangeDetectionStrategy.OnPush`, Selector-Prefix `app-`** — die App ist zoneless; ohne OnPush und Signals
  aktualisiert sich die Ansicht nicht verlässlich
- **State als Signals** (`signal`, `computed`, `linkedSignal`, `effect`), kein `BehaviorSubject` für Component-State
- **`input()`/`input.required()`/`output()`/`model()`** statt `@Input()`/`@Output()`; **`inject()`** statt
  Constructor-Injection — einheitlich mit Signals und ohne Decorator-Magie
- **`@if`/`@for` (mit `track`)/`@switch`/`@let`** statt `*ngIf`/`*ngFor`/`ngClass`/`ngStyle`
- **Reactive Forms; strict TypeScript, kein `any`** (`unknown`, wenn unsicher)
- **`HttpClient` nur in `core/api/`**, keine Fachlogik in Komponenten — Komponenten testen dann gegen ein
  Mock-Repository statt gegen HTTP
- **Design-Tokens statt Tailwind-Paletten:** `bg-surface`, `bg-background`, `text-foreground`, `text-muted`,
  `border-border`, `bg-accent`, `text-danger` (definiert in `src/styles.css`). Nicht `bg-blue-600` usw., weil
  Hell-/Dunkelmodus und spätere Themes nur über die Tokens funktionieren
- **Prettier** (Breite 100, Single Quotes): `npm run format` vor dem Commit; `format:check` ist CI-Gate
- **Vitest-Testnamen mit Suffix `(US-<nr>)`**, damit `check`-Skripte und Reviewer den Test der Story zuordnen
