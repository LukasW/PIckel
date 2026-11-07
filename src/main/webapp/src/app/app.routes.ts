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
  { path: '**', redirectTo: '' },
];
