import { Feature, NewFeature } from '../models/feature.model';

/** Driven Port: woher die Features eines PI kommen (heute REST). */
export abstract class FeaturesRepository {
  abstract list(programIncrementId: string): Promise<Feature[]>;
  abstract add(programIncrementId: string, feature: NewFeature): Promise<Feature>;
  abstract update(programIncrementId: string, id: string, feature: NewFeature): Promise<Feature>;
  abstract remove(programIncrementId: string, id: string): Promise<void>;
}
