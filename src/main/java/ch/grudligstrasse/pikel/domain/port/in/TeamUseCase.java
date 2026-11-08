package ch.grudligstrasse.pikel.domain.port.in;

import ch.grudligstrasse.pikel.domain.model.Team;
import java.util.List;
import java.util.UUID;

/** Use Cases rund um die Teams des ART. */
public interface TeamUseCase {

    /** Alle Teams, alphabetisch nach Name. */
    List<Team> listAll();

    /**
     * Erfasst ein neues Team.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.InvalidTeamException bei ungültigem Namen
     * @throws ch.grudligstrasse.pikel.domain.model.DuplicateTeamNameException wenn der Name schon vergeben ist
     */
    Team add(String name);

    /**
     * Benennt ein Team um, mit denselben Regeln wie beim Erfassen.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.TeamNotFoundException wenn es die ID nicht gibt
     */
    Team rename(UUID id, String name);

    /**
     * Entfernt ein Team für alle PIs.
     *
     * @throws ch.grudligstrasse.pikel.domain.model.TeamNotFoundException wenn es die ID nicht gibt
     */
    void remove(UUID id);
}
