import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { FeatureDependency } from '../models/feature-dependency.model';
import { FeatureDependenciesRepository } from '../ports/feature-dependencies.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: Abhängigkeiten zwischen den Features eines PI laden, erfassen und entfernen; Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class FeatureDependenciesService {
  private readonly repository = inject(FeatureDependenciesRepository);

  private readonly items = signal<FeatureDependency[]>([]);
  readonly dependencies = this.items.asReadonly();
  /** Meldung des letzten gescheiterten Aufrufs, sonst `undefined`. */
  readonly error = signal<string | undefined>(undefined);

  async load(programIncrementId: string): Promise<void> {
    this.error.set(undefined);
    try {
      this.items.set(await this.repository.list(programIncrementId));
    } catch (e: unknown) {
      this.error.set(toMessage(e));
    }
  }

  /** IDs der Features, die das Feature braucht. */
  needs(featureId: string): string[] {
    return this.items()
      .filter((d) => d.needingFeatureId === featureId)
      .map((d) => d.neededFeatureId);
  }

  /** IDs der Features, die das Feature brauchen. */
  neededBy(featureId: string): string[] {
    return this.items()
      .filter((d) => d.neededFeatureId === featureId)
      .map((d) => d.needingFeatureId);
  }

  /** `true` bei Erfolg (dann neu geladen), sonst steht die Meldung des Servers in `error`. */
  add(
    programIncrementId: string,
    needingFeatureId: string,
    neededFeatureId: string,
  ): Promise<boolean> {
    return this.changeAndReload(programIncrementId, () =>
      this.repository.add(programIncrementId, needingFeatureId, neededFeatureId),
    );
  }

  /** Wie `add`. */
  remove(
    programIncrementId: string,
    needingFeatureId: string,
    neededFeatureId: string,
  ): Promise<boolean> {
    return this.changeAndReload(programIncrementId, () =>
      this.repository.remove(programIncrementId, needingFeatureId, neededFeatureId),
    );
  }

  private async changeAndReload(
    programIncrementId: string,
    change: () => Promise<unknown>,
  ): Promise<boolean> {
    this.error.set(undefined);
    try {
      await change();
    } catch (e: unknown) {
      this.error.set(toMessage(e));
      return false;
    }
    await this.load(programIncrementId);
    return true;
  }
}

function toMessage(e: unknown): string {
  if (e instanceof HttpErrorResponse && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return GENERIC_ERROR;
}
