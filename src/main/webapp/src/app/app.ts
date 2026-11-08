import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  ActivatedRouteSnapshot,
  NavigationEnd,
  Router,
  RouterLink,
  RouterOutlet,
} from '@angular/router';
import { filter, map } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="border-b border-border bg-surface">
      <nav class="mx-auto flex items-center gap-4 px-4 py-3" [class.max-w-4xl]="!fullWidth()">
        <a routerLink="/" class="text-lg font-semibold">PIkel</a>
        <span class="text-sm text-muted">PI Planning</span>
        <a routerLink="/teams" class="ml-auto text-sm text-accent hover:underline">Teams</a>
      </nav>
    </header>
    <main class="mx-auto px-4 py-6" [class.max-w-4xl]="!fullWidth()">
      <router-outlet />
    </main>
  `,
})
export class App {
  private readonly router = inject(Router);

  /**
   * Ob die aktive Route `data: { fullWidth: true }` trägt. Gelesen am tiefsten Kind, denn `App` sitzt an der
   * Wurzelroute und sähe über `ActivatedRoute` die Daten der Seite nicht.
   */
  protected readonly fullWidth = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => deepestChild(this.router.routerState.snapshot.root).data['fullWidth'] === true),
    ),
    { initialValue: false },
  );
}

function deepestChild(route: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
  return route.firstChild ? deepestChild(route.firstChild) : route;
}
