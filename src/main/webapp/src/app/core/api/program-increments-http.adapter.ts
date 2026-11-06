import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlanProgramIncrement, ProgramIncrement } from '../models/program-increment.model';
import { ProgramIncrementsRepository } from '../ports/program-increments.repository';

const BASE_URL = '/api/v1/program-increments';

@Injectable({ providedIn: 'root' })
export class ProgramIncrementsHttpAdapter extends ProgramIncrementsRepository {
  private readonly http = inject(HttpClient);

  list(): Promise<ProgramIncrement[]> {
    return firstValueFrom(this.http.get<ProgramIncrement[]>(BASE_URL));
  }

  plan(request: PlanProgramIncrement): Promise<ProgramIncrement> {
    return firstValueFrom(this.http.post<ProgramIncrement>(BASE_URL, request));
  }
}
