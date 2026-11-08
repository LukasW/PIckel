import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/program-increments/program-increments.component').then(
        (m) => m.ProgramIncrementsComponent,
      ),
    title: 'Program Increments – PIkel',
  },
  {
    path: 'program-increments/:id',
    loadComponent: () =>
      import('./features/program-increment-detail/program-increment-detail.component').then(
        (m) => m.ProgramIncrementDetailComponent,
      ),
    title: 'Program Increment – PIkel',
  },
  {
    path: 'program-increments/:id/board',
    loadComponent: () =>
      import('./features/program-board/program-board.component').then(
        (m) => m.ProgramBoardComponent,
      ),
    title: 'Program Board – PIkel',
    // Das Board braucht alle Iterationen nebeneinander, App lässt dafür die Lesebreite weg (US-12).
    data: { fullWidth: true },
  },
  {
    path: 'teams',
    loadComponent: () => import('./features/teams/teams.component').then((m) => m.TeamsComponent),
    title: 'Teams – PIkel',
  },
  { path: '**', redirectTo: '' },
];
