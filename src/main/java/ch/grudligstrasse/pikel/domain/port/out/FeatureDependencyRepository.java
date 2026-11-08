package ch.grudligstrasse.pikel.domain.port.out;

import ch.grudligstrasse.pikel.domain.model.FeatureDependency;
import java.util.List;
import java.util.UUID;

/** Persistenz für Abhängigkeiten zwischen Features. */
public interface FeatureDependencyRepository {

    /** Alle Abhängigkeiten, deren Features zum PI gehören. */
    List<FeatureDependency> findByProgramIncrement(UUID programIncrementId);

    void save(FeatureDependency dependency);

    /** Entfernt die Abhängigkeit; ohne Wirkung, wenn es sie nicht gibt. */
    void remove(UUID needingFeatureId, UUID neededFeatureId);
}
