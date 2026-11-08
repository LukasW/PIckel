package ch.grudligstrasse.pickel.domain.port.in;

import ch.grudligstrasse.pickel.domain.model.ProgramBoard;
import java.util.UUID;

/** Driving Port: das Program Board eines PI. */
public interface BoardUseCase {

    /**
     * Das Board des PI mit allen Teams, den Iterationen und dem nicht eingeplanten Backlog.
     *
     * @throws ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException wenn es das PI nicht gibt
     */
    ProgramBoard get(UUID programIncrementId);
}
