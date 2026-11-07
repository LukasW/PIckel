package ch.grudligstrasse.pikel.domain.port.out;

import ch.grudligstrasse.pikel.domain.model.Feature;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistenz für Features. */
public interface FeatureRepository {

    List<Feature> findByProgramIncrement(UUID programIncrementId);

    Optional<Feature> findOne(UUID id);

    void save(Feature feature);

    /** Löscht das Feature; {@code false}, wenn es die ID nicht gibt. */
    boolean remove(UUID id);
}
