package ch.grudligstrasse.pikel.domain.port.out;

import ch.grudligstrasse.pikel.domain.model.Milestone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistenz für Milestones. */
public interface MilestoneRepository {

    /** Aufsteigend nach Datum, bei gleichem Datum nach Name. */
    List<Milestone> findByProgramIncrement(UUID programIncrementId);

    /** Nur, wenn der Milestone zu diesem PI gehört. */
    Optional<Milestone> findOne(UUID programIncrementId, UUID milestoneId);

    /** Legt den Milestone an oder übernimmt den Stand eines bestehenden. */
    void save(Milestone milestone);

    void delete(UUID milestoneId);
}
