import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  BusinessContext,
  NewPiLink,
  PlanProgramIncrement,
  ProgramIncrement,
} from '../models/program-increment.model';
import { ProgramIncrementsRepository } from '../ports/program-increments.repository';

const BASE_URL = '/api/v1/program-increments';

@Injectable({ providedIn: 'root' })
export class ProgramIncrementsHttpAdapter extends ProgramIncrementsRepository {
  private readonly http = inject(HttpClient);

  list(): Promise<ProgramIncrement[]> {
    return firstValueFrom(this.http.get<ProgramIncrement[]>(BASE_URL));
  }

  get(id: string): Promise<ProgramIncrement> {
    return firstValueFrom(this.http.get<ProgramIncrement>(`${BASE_URL}/${id}`));
  }

  plan(request: PlanProgramIncrement): Promise<ProgramIncrement> {
    return firstValueFrom(this.http.post<ProgramIncrement>(BASE_URL, request));
  }

  updateBusinessContext(id: string, request: BusinessContext): Promise<ProgramIncrement> {
    return firstValueFrom(
      this.http.put<ProgramIncrement>(`${BASE_URL}/${id}/business-context`, request),
    );
  }

  addLink(id: string, request: NewPiLink): Promise<ProgramIncrement> {
    return firstValueFrom(this.http.post<ProgramIncrement>(`${BASE_URL}/${id}/links`, request));
  }

  removeLink(id: string, linkId: string): Promise<ProgramIncrement> {
    return firstValueFrom(this.http.delete<ProgramIncrement>(`${BASE_URL}/${id}/links/${linkId}`));
  }
}
