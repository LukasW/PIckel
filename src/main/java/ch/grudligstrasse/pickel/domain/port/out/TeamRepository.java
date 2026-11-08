package ch.grudligstrasse.pickel.domain.port.out;

import ch.grudligstrasse.pickel.domain.model.Team;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistenz für Teams. */
public interface TeamRepository {

    List<Team> findAllTeams();

    Optional<Team> findOne(UUID id);

    /** Das Team mit diesem Namen, ohne Rücksicht auf Gross- und Kleinschreibung. */
    Optional<Team> findByNameIgnoreCase(String name);

    /** Legt das Team an oder überschreibt das mit derselben ID. */
    void save(Team team);

    /** Löscht das Team; liefert {@code false}, wenn es die ID nicht gab. */
    boolean remove(UUID id);
}
