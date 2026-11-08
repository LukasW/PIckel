import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Team } from '../models/team.model';
import { TeamsRepository } from '../ports/teams.repository';

const BASE_URL = '/api/v1/teams';

@Injectable({ providedIn: 'root' })
export class TeamsHttpAdapter extends TeamsRepository {
  private readonly http = inject(HttpClient);

  list(): Promise<Team[]> {
    return firstValueFrom(this.http.get<Team[]>(BASE_URL));
  }

  add(name: string): Promise<Team> {
    return firstValueFrom(this.http.post<Team>(BASE_URL, { name }));
  }

  rename(id: string, name: string): Promise<Team> {
    return firstValueFrom(this.http.put<Team>(`${BASE_URL}/${id}`, { name }));
  }

  async remove(id: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }
}
