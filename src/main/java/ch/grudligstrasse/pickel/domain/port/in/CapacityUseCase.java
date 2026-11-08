package ch.grudligstrasse.pickel.domain.port.in;

import ch.grudligstrasse.pickel.domain.model.Capacity;
import java.util.List;
import java.util.UUID;

/** Use Cases rund um die Kapazität der Teams in einem PI. */
public interface CapacityUseCase {

    /**
     * Die Kapazität jedes Teams in jeder Entwicklungs-Iteration des PI; ohne Eintrag 0. Aufsteigend nach
     * Iterationsnummer, innerhalb einer Iteration in der Reihenfolge der Teamliste.
     *
     * @throws ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     */
    List<Capacity> list(UUID programIncrementId);

    /**
     * Erfasst die Kapazität eines Teams in einer Iteration und überschreibt einen früheren Wert.
     *
     * @throws ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     * @throws ch.grudligstrasse.pickel.domain.model.TeamNotFoundException wenn es das Team nicht gibt
     * @throws ch.grudligstrasse.pickel.domain.model.InvalidCapacityException bei ungültigen Punkten oder Iteration
     */
    Capacity set(UUID programIncrementId, UUID teamId, int iterationNumber, Integer points);
}
