package ch.grudligstrasse.pikel.domain.port.in;

import ch.grudligstrasse.pikel.domain.model.Feature;
import java.util.List;
import java.util.UUID;

/** Driving Port: das Feature-Backlog eines PI. */
public interface FeatureUseCase {

    /**
     * Das Backlog des PI, aufsteigend nach WSJF-Rang (ohne Rang am Ende), bei gleichem Rang nach Titel.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     */
    List<Feature> listBacklog(UUID programIncrementId);

    /**
     * Erfasst ein Feature im PI; Rang und Schätzung sind optional (null = fehlt).
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidFeatureException bei ungültigen Angaben
     */
    Feature add(
            UUID programIncrementId,
            String title,
            String description,
            Integer wsjfRank,
            String owner,
            Integer estimate);
}
