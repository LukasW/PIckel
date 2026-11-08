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

    /**
     * Ersetzt Business-Kontext und PI Objectives des ART.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es die ID nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidProgramIncrementException bei zu langem Text
     */
    ProgramIncrement updateBusinessContext(UUID id, String businessContext, String artObjectives);

    /**
     * Fügt dem PI einen Link hinzu.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es die ID nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidProgramIncrementException bei ungültigem Titel oder URL
     */
    ProgramIncrement addLink(UUID id, String title, String url);

    /**
     * Entfernt einen Link vom PI.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException wenn es die ID nicht gibt
     * @throws ch.grudligstrasse.pikel.domain.model.PiLinkNotFoundException wenn das PI den Link nicht hat
     */
    ProgramIncrement removeLink(UUID id, UUID linkId);
}
