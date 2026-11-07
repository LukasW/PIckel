package ch.grudligstrasse.pikel.domain.port.out;

import ch.grudligstrasse.pikel.domain.model.Feature;
import java.util.List;
import java.util.UUID;

/** Persistenz für Features. */
public interface FeatureRepository {

    List<Feature> findByProgramIncrement(UUID programIncrementId);

    void save(Feature feature);
}
