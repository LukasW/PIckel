package ch.grudligstrasse.pikel.domain.port.in;

import ch.grudligstrasse.pikel.domain.model.FeatureDependency;
import java.util.List;
import java.util.UUID;

/** Driving Port: Abhängigkeiten zwischen den Features eines PI. */
public interface FeatureDependencyUseCase {

    /**
     * Alle Abhängigkeiten des PI.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     */
    List<FeatureDependency> list(UUID programIncrementId);

    /**
     * Hält fest, dass das Feature {@code needingFeatureId} das Feature {@code neededFeatureId} braucht.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException wenn ein Feature nicht existiert oder das
     *     brauchende Feature nicht zum PI gehört
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidFeatureException bei Selbstbezug, anderem PI des gebrauchten
     *     Features, doppelter oder direkt umgekehrter Abhängigkeit
     */
    FeatureDependency add(UUID programIncrementId, UUID needingFeatureId, UUID neededFeatureId);

    /**
     * Entfernt die Abhängigkeit; gibt es sie nicht, geschieht nichts (idempotent).
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException wenn das brauchende Feature zu einem
     *     anderen PI gehört
     */
    void remove(UUID programIncrementId, UUID needingFeatureId, UUID neededFeatureId);
}
