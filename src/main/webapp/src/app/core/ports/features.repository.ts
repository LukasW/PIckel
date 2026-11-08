import { Feature, NewFeature } from '../models/feature.model';

/** Driven Port: woher die Features eines PI kommen (heute REST). */
export abstract class FeaturesRepository {
  abstract list(programIncrementId: string): Promise<Feature[]>;
  abstract add(programIncrementId: string, feature: NewFeature): Promise<Feature>;
  abstract update(programIncrementId: string, id: string, feature: NewFeature): Promise<Feature>;
  /** Liefert die Anzahl der importierten Features. */
  abstract importCsv(programIncrementId: string, csv: string): Promise<number>;
  /** Plant das Feature in die Zelle Team × Iteration (Nummer im PI) ein. */
  abstract schedule(
    programIncrementId: string,
    id: string,
    teamId: string,
    iterationNumber: number,
  ): Promise<Feature>;
  /** Nimmt das Feature zurück ins Backlog. */
  abstract unschedule(programIncrementId: string, id: string): Promise<Feature>;
  abstract remove(programIncrementId: string, id: string): Promise<void>;
}
