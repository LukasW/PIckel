package ch.grudligstrasse.pikel.domain.port.in;

import ch.grudligstrasse.pikel.domain.model.Milestone;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Use Cases rund um die Milestones eines PI. */
public interface MilestoneUseCase {

    /**
     * Die Milestones des PI, aufsteigend nach Datum, bei gleichem Datum nach Name.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     */
    List<Milestone> list(UUID programIncrementId);

    /**
     * Erfasst einen Milestone im PI.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidMilestoneException bei ungültigen Angaben
     */
    Milestone add(UUID programIncrementId, String name, LocalDate date);

    /**
     * Ändert Name und Datum eines Milestones.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.MilestoneNotFoundException wenn das PI den Milestone nicht hat
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidMilestoneException bei ungültigen Angaben
     */
    Milestone update(UUID programIncrementId, UUID milestoneId, String name, LocalDate date);

    /**
     * Löscht einen Milestone.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.MilestoneNotFoundException wenn das PI den Milestone nicht hat
     */
    void delete(UUID programIncrementId, UUID milestoneId);
}
