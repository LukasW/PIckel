import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Capacity } from '../models/capacity.model';
import { CapacitiesRepository } from '../ports/capacities.repository';

const BASE_URL = '/api/v1/program-increments';

@Injectable({ providedIn: 'root' })
export class CapacitiesHttpAdapter extends CapacitiesRepository {
  private readonly http = inject(HttpClient);

  list(programIncrementId: string): Promise<Capacity[]> {
    return firstValueFrom(
      this.http.get<Capacity[]>(`${BASE_URL}/${programIncrementId}/capacities`),
    );
  }

  set(
    programIncrementId: string,
    teamId: string,
    iterationNumber: number,
    points: number,
  ): Promise<Capacity> {
    return firstValueFrom(
      this.http.put<Capacity>(
        `${BASE_URL}/${programIncrementId}/capacities/${teamId}/${iterationNumber}`,
        { points },
      ),
    );
  }
}
