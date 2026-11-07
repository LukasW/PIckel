import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Team } from '../models/team.model';
import { TeamsRepository } from '../ports/teams.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: Teams laden, erfassen, umbenennen, entfernen; hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class TeamsService {
  private readonly repository = inject(TeamsRepository);

  // Der Server sortiert alphabetisch (US-6); eine zweite Sortierung hier wäre ein zweiter Ort für dieselbe Regel.
  private readonly items = signal<Team[]>([]);
  readonly teams = this.items.asReadonly();
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

  /** Liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  add(name: string): Promise<boolean> {
    return this.change(() => this.repository.add(name));
  }

  /** Liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  rename(id: string, name: string): Promise<boolean> {
    return this.change(() => this.repository.rename(id, name));
  }

  /** Liefert `true` bei Erfolg, sonst steht die Meldung in `error`. */
  remove(id: string): Promise<boolean> {
    return this.change(() => this.repository.remove(id));
  }

  /** Führt eine Änderung aus und lädt danach die Liste in der Reihenfolge des Servers. */
  private async change(action: () => Promise<unknown>): Promise<boolean> {
    this.error.set(undefined);
    try {
      await action();
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
