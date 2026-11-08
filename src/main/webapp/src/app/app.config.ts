import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { ProgramIncrementsRepository } from './core/ports/program-increments.repository';
import { ProgramIncrementsHttpAdapter } from './core/api/program-increments-http.adapter';
import { TeamsRepository } from './core/ports/teams.repository';
import { TeamsHttpAdapter } from './core/api/teams-http.adapter';
import { FeaturesRepository } from './core/ports/features.repository';
import { FeaturesHttpAdapter } from './core/api/features-http.adapter';
import { CapacitiesRepository } from './core/ports/capacities.repository';
import { CapacitiesHttpAdapter } from './core/api/capacities-http.adapter';
import { BoardRepository } from './core/ports/board.repository';
import { BoardHttpAdapter } from './core/api/board-http.adapter';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch()),
    { provide: ProgramIncrementsRepository, useExisting: ProgramIncrementsHttpAdapter },
    { provide: TeamsRepository, useExisting: TeamsHttpAdapter },
    { provide: FeaturesRepository, useExisting: FeaturesHttpAdapter },
    { provide: CapacitiesRepository, useExisting: CapacitiesHttpAdapter },
    { provide: BoardRepository, useExisting: BoardHttpAdapter },
  ],
};
