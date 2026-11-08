package ch.grudligstrasse.pikel.adapter.in.rest;

import ch.grudligstrasse.pikel.domain.model.FeatureDependency;
import java.util.UUID;

/** Eine Abhängigkeit für die Oberfläche: das Feature {@code needingFeatureId} braucht {@code neededFeatureId}. */
public record FeatureDependencyDto(UUID needingFeatureId, UUID neededFeatureId) {

    public static FeatureDependencyDto from(FeatureDependency dependency) {
        return new FeatureDependencyDto(dependency.needingFeatureId(), dependency.neededFeatureId());
    }
}
