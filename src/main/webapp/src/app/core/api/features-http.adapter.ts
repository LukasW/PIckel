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
}
