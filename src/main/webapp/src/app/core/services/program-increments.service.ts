import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { PlanProgramIncrement, ProgramIncrement } from '../models/program-increment.model';
import { ProgramIncrementsRepository } from '../ports/program-increments.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: PIs laden und anlegen; hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class ProgramIncrementsService {
  private readonly repository = inject(ProgramIncrementsRepository);

  // Der Server sortiert nach Startdatum (US-1); eine zweite Sortierung hier wäre ein zweiter Ort für dieselbe Regel.
  private readonly items = signal<ProgramIncrement[]>([]);
  readonly programIncrements = this.items.asReadonly();
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

  /**
   * Lädt ein PI mit seinen Iterationen; liefert `undefined` und setzt `error`, wenn es das PI nicht gibt.
   */
  async get(id: string): Promise<ProgramIncrement | undefined> {
    this.error.set(undefined);
    try {
      return await this.repository.get(id);
    } catch (e: unknown) {
      this.error.set(toMessage(e));
      return undefined;
    }
  }

  /**
   * Legt ein PI an und lädt danach die Liste in der Reihenfolge des Servers; liefert `true` bei Erfolg, sonst steht
   * die Meldung in `error`.
   */
  async plan(request: PlanProgramIncrement): Promise<boolean> {
    this.error.set(undefined);
    try {
      await this.repository.plan(request);
      await this.load();
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
