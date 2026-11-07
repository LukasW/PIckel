import { Feature, NewFeature } from '../models/feature.model';

/** Driven Port: woher die Features eines PI kommen (heute REST). */
export abstract class FeaturesRepository {
  abstract list(programIncrementId: string): Promise<Feature[]>;
  abstract add(programIncrementId: string, feature: NewFeature): Promise<Feature>;
}
