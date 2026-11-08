import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Milestone, MilestoneInput } from '../models/milestone.model';
import { MilestonesRepository } from '../ports/milestones.repository';

const BASE_URL = '/api/v1/program-increments';

@Injectable({ providedIn: 'root' })
export class MilestonesHttpAdapter extends MilestonesRepository {
  private readonly http = inject(HttpClient);

  list(programIncrementId: string): Promise<Milestone[]> {
    return firstValueFrom(
      this.http.get<Milestone[]>(`${BASE_URL}/${programIncrementId}/milestones`),
    );
  }

  add(programIncrementId: string, input: MilestoneInput): Promise<Milestone> {
    return firstValueFrom(
      this.http.post<Milestone>(`${BASE_URL}/${programIncrementId}/milestones`, input),
    );
  }

  update(
    programIncrementId: string,
    milestoneId: string,
    input: MilestoneInput,
  ): Promise<Milestone> {
    return firstValueFrom(
      this.http.put<Milestone>(
        `${BASE_URL}/${programIncrementId}/milestones/${milestoneId}`,
        input,
      ),
    );
  }

  async remove(programIncrementId: string, milestoneId: string): Promise<void> {
    await firstValueFrom(
      this.http.delete<void>(`${BASE_URL}/${programIncrementId}/milestones/${milestoneId}`),
    );
  }
}
