import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Board } from '../models/board.model';
import { BoardRepository } from '../ports/board.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: das Board eines PI laden (US-12); hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class BoardService {
  private readonly repository = inject(BoardRepository);

  // Reihenfolgen (Teams alphabetisch, Backlog nach WSJF) kommen vom Server; hier wird nicht umsortiert.
  private readonly current = signal<Board | undefined>(undefined);
  readonly board = this.current.asReadonly();
  readonly loading = signal(false);
  readonly error = signal<string | undefined>(undefined);

  /** Zählt die Ladeaufträge; nur die Antwort auf den letzten zählt. */
  private latestRequest = 0;

  async load(programIncrementId: string): Promise<void> {
    const request = ++this.latestRequest;
    // Das Board eines anderen PI soll nicht stehen bleiben, während das neue lädt oder scheitert.
    this.current.set(undefined);
    this.loading.set(true);
    this.error.set(undefined);
    try {
      const board = await this.repository.get(programIncrementId);
      if (request === this.latestRequest) {
        this.current.set(board);
      }
    } catch (e: unknown) {
      if (request === this.latestRequest) {
        this.error.set(toMessage(e));
      }
    } finally {
      if (request === this.latestRequest) {
        this.loading.set(false);
      }
    }
  }
}

function toMessage(e: unknown): string {
  if (e instanceof HttpErrorResponse && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return GENERIC_ERROR;
}
