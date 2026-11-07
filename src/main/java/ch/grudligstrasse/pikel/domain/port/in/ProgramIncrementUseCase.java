package ch.grudligstrasse.pikel.domain.port.in;

import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Use Cases rund um Program Increments. */
public interface ProgramIncrementUseCase {

    /** Alle PIs, aufsteigend nach Startdatum. */
    List<ProgramIncrement> listAll();

    /**
     * Legt ein neues PI an.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidProgramIncrementException bei ungültigen Angaben
     */
    ProgramIncrement plan(String name, LocalDate startDate, LocalDate endDate, Integer iterationLengthWeeks);

    /**
     * Liefert ein PI.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es die ID nicht gibt
     */
    ProgramIncrement get(UUID id);
}
