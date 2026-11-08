package ch.grudligstrasse.pickel.domain.port.out;

import ch.grudligstrasse.pickel.domain.model.Capacity;
import java.util.List;
import java.util.UUID;

/** Persistenz für die Kapazität; gespeichert werden nur erfasste Werte. */
public interface CapacityRepository {

    List<Capacity> findByProgramIncrement(UUID programIncrementId);

    /** Legt die Kapazität an oder überschreibt die für dasselbe PI, Team und dieselbe Iteration. */
    void save(UUID programIncrementId, Capacity capacity);
}
