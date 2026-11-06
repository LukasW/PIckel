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
  { path: '**', redirectTo: '' },
];
