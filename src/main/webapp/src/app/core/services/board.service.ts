import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Board, DependencyLink } from '../models/board.model';
import { Feature } from '../models/feature.model';
import { BoardRepository } from '../ports/board.repository';
import { FeaturesRepository } from '../ports/features.repository';

const GENERIC_ERROR = 'Das hat nicht geklappt. Bitte versuche es nochmals.';

/** Use Case: das Board eines PI laden (US-12); hält den Zustand als Signals. */
@Injectable({ providedIn: 'root' })
export class BoardService {
  private readonly repository = inject(BoardRepository);
  private readonly featuresRepository = inject(FeaturesRepository);

  // Reihenfolgen (Teams alphabetisch, Backlog nach WSJF) kommen vom Server; hier wird nicht umsortiert.
  private readonly current = signal<Board | undefined>(undefined);
  readonly board = this.current.asReadonly();
  /** Abhängigkeiten, bei denen beide Features eingeplant sind: Sie bekommen eine Linie (US-17). */
  readonly dependencyLinks = computed<readonly DependencyLink[]>(() => {
    const board = this.current();
    if (!board) {
      return [];
    }
    const scheduled = new Set(board.scheduled.map((f) => f.id));
    return board.dependencies
      .filter((d) => scheduled.has(d.neededFeatureId) && scheduled.has(d.needingFeatureId))
      .map(({ neededFeatureId, needingFeatureId }) => ({ neededFeatureId, needingFeatureId }));
  });

  /** Je Feature die Anzahl Abhängigkeiten ohne Linie, in beide Richtungen; abgeleitet wie die Linien. */
  private readonly unlinkedCounts = computed(() => {
    const counts = new Map<string, number>();
    const board = this.current();
    if (!board) {
      return counts;
    }
    const scheduled = new Set(board.scheduled.map((f) => f.id));
    for (const d of board.dependencies) {
      if (scheduled.has(d.neededFeatureId) && scheduled.has(d.needingFeatureId)) {
        continue;
      }
      for (const id of [d.neededFeatureId, d.needingFeatureId]) {
        counts.set(id, (counts.get(id) ?? 0) + 1);
      }
    }
    return counts;
  });
  readonly loading = signal(false);
  readonly error = signal<string | undefined>(undefined);
  /** Meldung, wenn das Einplanen oder Zurücknehmen scheitert (US-13); das Board steht dann wie vorher. */
  readonly actionError = signal<string | undefined>(undefined);

  /** Zählt die Ladeaufträge; nur die Antwort auf den letzten zählt. */
  private latestRequest = 0;

  /** Anzahl Abhängigkeiten des Features (beide Richtungen), für die keine Linie gezeichnet ist (US-17). */
  unlinkedDependencyCount(featureId: string): number {
    return this.unlinkedCounts().get(featureId) ?? 0;
  }

  /** Plant ein Feature in die Zelle ein: Das Board ändert sich sofort, bei einem Fehler kehrt es zurück (US-13). */
  async schedule(feature: Feature, teamId: string, iterationNumber: number): Promise<void> {
    await this.applyOptimistically(
      feature,
      { ...feature, scheduled: true, teamId, iterationNumber },
      () =>
        this.featuresRepository.schedule(
          feature.programIncrementId,
          feature.id,
          teamId,
          iterationNumber,
        ),
    );
  }

  /** Nimmt ein Feature zurück ins Backlog, sofort sichtbar und mit Rücknahme bei einem Fehler (US-13). */
  async unschedule(feature: Feature): Promise<void> {
    const { teamId: _teamId, iterationNumber: _iterationNumber, ...rest } = feature;
    await this.applyOptimistically(feature, { ...rest, scheduled: false }, () =>
      this.featuresRepository.unschedule(feature.programIncrementId, feature.id),
    );
  }

  private async applyOptimistically(
    original: Feature,
    placed: Feature,
    save: () => Promise<Feature>,
  ): Promise<void> {
    const request = this.latestRequest;
    this.actionError.set(undefined);
    this.place(placed);
    try {
      await save();
    } catch (e: unknown) {
      // Nur diese Karte kehrt zurück: Eine Aktion, die inzwischen gelungen ist, bleibt stehen. Ein inzwischen
      // geladenes anderes PI bleibt unberührt.
      if (request === this.latestRequest) {
        this.place(original);
      }
      this.actionError.set(toMessage(e));
    }
  }

  /** Legt die Karte ins Backlog oder zu den Eingeplanten, an ihren Platz nach WSJF-Rang. */
  private place(feature: Feature): void {
    const board = this.current();
    if (!board) {
      return;
    }
    const backlog = board.backlog.filter((f) => f.id !== feature.id);
    const scheduled = board.scheduled.filter((f) => f.id !== feature.id);
    this.current.set(
      feature.scheduled
        ? { ...board, backlog, scheduled: insertByRank(scheduled, feature) }
        : { ...board, backlog: insertByRank(backlog, feature), scheduled },
    );
  }

  async load(programIncrementId: string): Promise<void> {
    const request = ++this.latestRequest;
    // Das Board eines anderen PI soll nicht stehen bleiben, während das neue lädt oder scheitert.
    this.current.set(undefined);
    this.loading.set(true);
    this.error.set(undefined);
    this.actionError.set(undefined);
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

/** Wie die Reihenfolge des Servers: nach WSJF-Rang (ohne Rang am Ende), bei Gleichstand nach Titel. */
function insertByRank(features: readonly Feature[], added: Feature): readonly Feature[] {
  const rank = (f: Feature): number => f.wsjfRank ?? Number.MAX_SAFE_INTEGER;
  const before = (f: Feature): boolean =>
    rank(f) < rank(added) ||
    (rank(f) === rank(added) &&
      f.title.localeCompare(added.title, undefined, { sensitivity: 'base' }) <= 0);
  const index = features.findIndex((f) => !before(f));
  return index === -1
    ? [...features, added]
    : [...features.slice(0, index), added, ...features.slice(index)];
}

function toMessage(e: unknown): string {
  if (e instanceof HttpErrorResponse && typeof e.error?.message === 'string') {
    return e.error.message;
  }
  return GENERIC_ERROR;
}
