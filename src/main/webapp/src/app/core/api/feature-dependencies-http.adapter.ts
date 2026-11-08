import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FeatureDependency } from '../models/feature-dependency.model';
import { FeatureDependenciesRepository } from '../ports/feature-dependencies.repository';

const url = (programIncrementId: string): string =>
  `/api/v1/program-increments/${programIncrementId}/dependencies`;

@Injectable({ providedIn: 'root' })
export class FeatureDependenciesHttpAdapter extends FeatureDependenciesRepository {
  private readonly http = inject(HttpClient);

  list(programIncrementId: string): Promise<FeatureDependency[]> {
    return firstValueFrom(this.http.get<FeatureDependency[]>(url(programIncrementId)));
  }

  add(
    programIncrementId: string,
    needingFeatureId: string,
    neededFeatureId: string,
  ): Promise<FeatureDependency> {
    return firstValueFrom(
      this.http.post<FeatureDependency>(url(programIncrementId), {
        needingFeatureId,
        neededFeatureId,
      }),
    );
  }

  async remove(
    programIncrementId: string,
    needingFeatureId: string,
    neededFeatureId: string,
  ): Promise<void> {
    await firstValueFrom(
      this.http.delete<void>(`${url(programIncrementId)}/${needingFeatureId}/${neededFeatureId}`),
    );
  }
}
