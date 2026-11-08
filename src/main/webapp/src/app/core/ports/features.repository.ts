import { Feature, NewFeature } from '../models/feature.model';

/** Driven Port: woher die Features eines PI kommen (heute REST). */
export abstract class FeaturesRepository {
  abstract list(programIncrementId: string): Promise<Feature[]>;
  abstract add(programIncrementId: string, feature: NewFeature): Promise<Feature>;
  abstract update(programIncrementId: string, id: string, feature: NewFeature): Promise<Feature>;
  /** Liefert die Anzahl der importierten Features. */
  abstract importCsv(programIncrementId: string, csv: string): Promise<number>;
  abstract remove(programIncrementId: string, id: string): Promise<void>;
}
