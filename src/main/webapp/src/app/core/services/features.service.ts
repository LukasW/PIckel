import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Feature, NewFeature } from '../models/feature.model';
import { FeaturesRepository } from '../ports/features.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: Backlog eines PI laden, Features erfassen, bearbeiten und löschen; hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class FeaturesService {
  private readonly repository = inject(FeaturesRepository);

  // Der Server sortiert nach WSJF-Rang (US-9); eine zweite Sortierung hier wäre ein zweiter Ort für dieselbe Regel.
  private readonly items = signal<Feature[]>([]);
  readonly features = this.items.asReadonly();
  readonly loading = signal(false);
  readonly error = signal<string | undefined>(undefined);

  async load(programIncrementId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(undefined);
    try {
      this.items.set(await this.repository.list(programIncrementId));
    } catch (e: unknown) {
      this.error.set(toMessage(e));
    } finally {
      this.loading.set(false);
    }
  }

  /** Liefert `true` bei Erfolg und lädt dann das Backlog neu, sonst steht die Meldung in `error`. */
  add(programIncrementId: string, feature: NewFeature): Promise<boolean> {
    return this.changeAndReload(programIncrementId, () =>
      this.repository.add(programIncrementId, feature),
    );
  }

  /** Wie `add`: `true` bei Erfolg und das Backlog wird neu geladen, sonst steht die Meldung in `error`. */
  update(programIncrementId: string, id: string, feature: NewFeature): Promise<boolean> {
    return this.changeAndReload(programIncrementId, () =>
      this.repository.update(programIncrementId, id, feature),
    );
  }

  /** Wie `add`: `true` bei Erfolg und das Backlog wird neu geladen, sonst steht die Meldung in `error`. */
  remove(programIncrementId: string, id: string): Promise<boolean> {
    return this.changeAndReload(programIncrementId, () =>
      this.repository.remove(programIncrementId, id),
    );
  }

  private async changeAndReload(
    programIncrementId: string,
    change: () => Promise<unknown>,
  ): Promise<boolean> {
    this.error.set(undefined);
    try {
      await change();
      await this.load(programIncrementId);
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
