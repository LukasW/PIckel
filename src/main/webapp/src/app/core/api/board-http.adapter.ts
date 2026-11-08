import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Board } from '../models/board.model';
import { BoardRepository } from '../ports/board.repository';

@Injectable({ providedIn: 'root' })
export class BoardHttpAdapter extends BoardRepository {
  private readonly http = inject(HttpClient);

  get(programIncrementId: string): Promise<Board> {
    return firstValueFrom(
      this.http.get<Board>(`/api/v1/program-increments/${programIncrementId}/board`),
    );
  }
}
