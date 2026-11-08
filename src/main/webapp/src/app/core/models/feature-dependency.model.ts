/** Das Feature `needingFeatureId` braucht das Feature `neededFeatureId`; beide gehören zum selben PI. */
export interface FeatureDependency {
  needingFeatureId: string;
  neededFeatureId: string;
}
