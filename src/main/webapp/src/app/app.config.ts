import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { ProgramIncrementsRepository } from './core/ports/program-increments.repository';
import { ProgramIncrementsHttpAdapter } from './core/api/program-increments-http.adapter';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    { provide: ProgramIncrementsRepository, useExisting: ProgramIncrementsHttpAdapter },
  ],
};
