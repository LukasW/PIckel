package ch.grudligstrasse.pickel.domain.model;

import java.util.List;
import java.util.UUID;

/** Das Feature {@code needingFeatureId} braucht das Feature {@code neededFeatureId}; beide gehören zum selben PI. */
public record FeatureDependency(UUID needingFeatureId, UUID neededFeatureId) {

    /**
     * Prüft, ob {@code needing} das Feature {@code needed} brauchen darf, gegeben die bestehenden Abhängigkeiten.
     * Längere Zyklen erkennt PIckel in M1 bewusst nicht, nur die direkte Umkehr.
     *
     * @throws InvalidFeatureException bei Selbstbezug, anderem PI, doppelter oder direkt umgekehrter Abhängigkeit
     */
    public static FeatureDependency between(Feature needing, Feature needed, List<FeatureDependency> existing) {
        if (needing.id().equals(needed.id())) {
            throw new InvalidFeatureException("Ein Feature kann nicht von sich selbst abhängen.");
        }
        if (!needing.programIncrementId().equals(needed.programIncrementId())) {
            throw new InvalidFeatureException("Beide Features müssen zum selben PI gehören.");
        }
        FeatureDependency dependency = new FeatureDependency(needing.id(), needed.id());
        if (existing.contains(dependency)) {
            throw new InvalidFeatureException("Diese Abhängigkeit gibt es schon.");
        }
        if (existing.contains(new FeatureDependency(needed.id(), needing.id()))) {
            throw new InvalidFeatureException("Die beiden Features hängen schon umgekehrt voneinander ab.");
        }
        return dependency;
    }
}
