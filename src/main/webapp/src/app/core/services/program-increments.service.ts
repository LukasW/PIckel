import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { PlanProgramIncrement, ProgramIncrement } from '../models/program-increment.model';
import { ProgramIncrementsRepository } from '../ports/program-increments.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: PIs laden und anlegen; hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class ProgramIncrementsService {
  private readonly repository = inject(ProgramIncrementsRepository);

  private readonly items = signal<ProgramIncrement[]>([]);
  readonly programIncrements = computed(() =>
    [...this.items()].sort((a, b) => a.startDate.localeCompare(b.startDate)),
  );
  readonly loading = signal(false);
  readonly error = signal<string | undefined>(undefined);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(undefined);
    try {
      this.items.set(await this.repository.list());
    } catch (e: unknown) {
      this.error.set(toMessage(e));
    } finally {
      this.loading.set(false);
    }
  }

  /** Legt ein PI an; liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  async plan(request: PlanProgramIncrement): Promise<boolean> {
    this.error.set(undefined);
    try {
      const created = await this.repository.plan(request);
      this.items.update((list) => [...list, created]);
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
