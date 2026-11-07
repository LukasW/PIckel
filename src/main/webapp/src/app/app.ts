import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="border-b border-border bg-surface">
      <nav class="mx-auto flex max-w-4xl items-center gap-4 px-4 py-3">
        <a routerLink="/" class="text-lg font-semibold">PIkel</a>
        <span class="text-sm text-muted">PI Planning</span>
        <a routerLink="/teams" class="ml-auto text-sm text-accent hover:underline">Teams</a>
      </nav>
    </header>
    <main class="mx-auto max-w-4xl px-4 py-6">
      <router-outlet />
    </main>
  `,
})
export class App {}
