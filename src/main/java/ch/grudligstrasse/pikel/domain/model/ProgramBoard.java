package ch.grudligstrasse.pikel.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Das Program Board eines PI: Teams als Zeilen, Iterationen als Spalten und das Backlog der noch nicht eingeplanten
 * Features.
 *
 * @param teams alphabetisch nach Name
 * @param iterations zeitlich, die IP-Iteration zuletzt
 * @param backlog nicht eingeplante Features in Backlog-Reihenfolge
 */
public record ProgramBoard(
        ProgramIncrement programIncrement, List<Team> teams, List<Iteration> iterations, List<Feature> backlog) {

    public ProgramBoard {
        Objects.requireNonNull(programIncrement, "programIncrement");
        teams = List.copyOf(teams);
        iterations = List.copyOf(iterations);
        backlog = List.copyOf(backlog);
    }
}
