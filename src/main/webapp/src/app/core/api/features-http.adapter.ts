import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Feature, NewFeature } from '../models/feature.model';
import { FeaturesRepository } from '../ports/features.repository';

const url = (programIncrementId: string): string =>
  `/api/v1/program-increments/${programIncrementId}/features`;

@Injectable({ providedIn: 'root' })
export class FeaturesHttpAdapter extends FeaturesRepository {
  private readonly http = inject(HttpClient);

  list(programIncrementId: string): Promise<Feature[]> {
    return firstValueFrom(this.http.get<Feature[]>(url(programIncrementId)));
  }

  add(programIncrementId: string, feature: NewFeature): Promise<Feature> {
    return firstValueFrom(this.http.post<Feature>(url(programIncrementId), feature));
  }

  update(programIncrementId: string, id: string, feature: NewFeature): Promise<Feature> {
    return firstValueFrom(this.http.put<Feature>(`${url(programIncrementId)}/${id}`, feature));
  }

  schedule(
    programIncrementId: string,
    id: string,
    teamId: string,
    iterationNumber: number,
  ): Promise<Feature> {
    return firstValueFrom(
      this.http.put<Feature>(`${url(programIncrementId)}/${id}/scheduling`, {
        teamId,
        iterationNumber,
      }),
    );
  }

  unschedule(programIncrementId: string, id: string): Promise<Feature> {
    return firstValueFrom(this.http.delete<Feature>(`${url(programIncrementId)}/${id}/scheduling`));
  }

  async importCsv(programIncrementId: string, csv: string): Promise<number> {
    const result = await firstValueFrom(
      this.http.post<{ imported: number }>(`${url(programIncrementId)}/import`, csv, {
        headers: { 'Content-Type': 'text/csv; charset=UTF-8' },
      }),
    );
    return result.imported;
  }

  async remove(programIncrementId: string, id: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${url(programIncrementId)}/${id}`));
  }
}
