import { Capacity } from '../models/capacity.model';

/** Driven Port: woher die Kapazitäten eines PI kommen (heute REST). */
export abstract class CapacitiesRepository {
  /** Jede Zelle Team × Entwicklungs-Iteration, auch ohne Eintrag (Punkte 0). */
  abstract list(programIncrementId: string): Promise<Capacity[]>;
  abstract set(
    programIncrementId: string,
    teamId: string,
    iterationNumber: number,
    points: number,
  ): Promise<Capacity>;
}
