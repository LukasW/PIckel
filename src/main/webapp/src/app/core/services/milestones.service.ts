import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Milestone, MilestoneInput } from '../models/milestone.model';
import { MilestonesRepository } from '../ports/milestones.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: Milestones eines PI laden, erfassen, ändern und löschen (US-15); hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class MilestonesService {
  private readonly repository = inject(MilestonesRepository);

  private readonly items = signal<readonly Milestone[]>([]);
  readonly milestones = this.items.asReadonly();
  readonly error = signal<string | undefined>(undefined);

  async load(programIncrementId: string): Promise<void> {
    // Die Milestones eines anderen PI sollen nicht stehen bleiben, während das neue lädt.
    this.items.set([]);
    this.error.set(undefined);
    try {
      this.items.set(await this.repository.list(programIncrementId));
    } catch (e: unknown) {
      this.error.set(toMessage(e));
    }
  }

  /** Liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  add(programIncrementId: string, input: MilestoneInput): Promise<boolean> {
    return this.change(async () => {
      const saved = await this.repository.add(programIncrementId, input);
      this.items.update((all) => sorted([...all, saved]));
    });
  }

  /** Liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  update(programIncrementId: string, milestoneId: string, input: MilestoneInput): Promise<boolean> {
    return this.change(async () => {
      const saved = await this.repository.update(programIncrementId, milestoneId, input);
      this.items.update((all) => sorted(all.map((m) => (m.id === saved.id ? saved : m))));
    });
  }

  /** Liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  remove(programIncrementId: string, milestoneId: string): Promise<boolean> {
    return this.change(async () => {
      await this.repository.remove(programIncrementId, milestoneId);
      this.items.update((all) => all.filter((m) => m.id !== milestoneId));
    });
  }

  // Die Liste wird nachgeführt statt neu geladen: ein Aufruf weniger, und die Reihenfolge gilt wie auf dem Server.
  private async change(action: () => Promise<void>): Promise<boolean> {
    this.error.set(undefined);
    try {
      await action();
      return true;
    } catch (e: unknown) {
      this.error.set(toMessage(e));
      return false;
    }
  }
}

/** Wie der Server: aufsteigend nach Datum, bei gleichem Datum nach Name. */
function sorted(milestones: Milestone[]): Milestone[] {
  return milestones.sort((a, b) => a.date.localeCompare(b.date) || a.name.localeCompare(b.name));
}

function toMessage(e: unknown): string {
  if (e instanceof HttpErrorResponse && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return GENERIC_ERROR;
}
