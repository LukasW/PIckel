import { FeatureDependency } from '../models/feature-dependency.model';

/** Driven Port: woher die Abhängigkeiten zwischen den Features eines PI kommen (heute REST). */
export abstract class FeatureDependenciesRepository {
  abstract list(programIncrementId: string): Promise<FeatureDependency[]>;
  abstract add(
    programIncrementId: string,
    needingFeatureId: string,
    neededFeatureId: string,
  ): Promise<FeatureDependency>;
  abstract remove(
    programIncrementId: string,
    needingFeatureId: string,
    neededFeatureId: string,
  ): Promise<void>;
}
