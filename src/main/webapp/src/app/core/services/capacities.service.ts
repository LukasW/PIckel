import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Capacity } from '../models/capacity.model';
import { CapacitiesRepository } from '../ports/capacities.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: Kapazitäten eines PI laden und erfassen; hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class CapacitiesService {
  private readonly repository = inject(CapacitiesRepository);

  private readonly items = signal<readonly Capacity[]>([]);
  readonly capacities = this.items.asReadonly();
  readonly error = signal<string | undefined>(undefined);

  async load(programIncrementId: string): Promise<void> {
    this.error.set(undefined);
    try {
      this.items.set(await this.repository.list(programIncrementId));
    } catch (e: unknown) {
      this.error.set(toMessage(e));
    }
  }

  /** Liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  async set(
    programIncrementId: string,
    teamId: string,
    iterationNumber: number,
    points: number,
  ): Promise<boolean> {
    this.error.set(undefined);
    try {
      const saved = await this.repository.set(programIncrementId, teamId, iterationNumber, points);
      // Nur die Zelle ersetzen: Ein Neuladen der ganzen Tabelle überschriebe Eingaben in anderen Zellen.
      this.items.update((cells) => {
        const index = cells.findIndex(
          (c) => c.teamId === saved.teamId && c.iterationNumber === saved.iterationNumber,
        );
        return index < 0 ? [...cells, saved] : cells.map((c, i) => (i === index ? saved : c));
      });
      return true;
    } catch (e: unknown) {
      this.error.set(toMessage(e));
      return false;
    }
  }
}

function toMessage(e: unknown): string {
  if (e instanceof HttpErrorResponse && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return GENERIC_ERROR;
}
