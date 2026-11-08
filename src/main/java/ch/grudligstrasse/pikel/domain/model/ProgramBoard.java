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
 * @param milestones aufsteigend nach Datum; je ein Marker über der Iteration, in die das Datum fällt
 */
public record ProgramBoard(
        ProgramIncrement programIncrement,
        List<Team> teams,
        List<Iteration> iterations,
        List<Feature> backlog,
        List<Milestone> milestones) {

    public ProgramBoard {
        Objects.requireNonNull(programIncrement, "programIncrement");
        teams = List.copyOf(teams);
        iterations = List.copyOf(iterations);
        backlog = List.copyOf(backlog);
        milestones = List.copyOf(milestones);
    }

    /**
     * Nummer der Iteration, über der der Milestone steht. Das Datum liegt im PI, weil {@link Milestone#create} und
     * {@link Milestone#update} es prüfen und sich der Zeitraum eines PI heute nicht ändern lässt; wer das ändert,
     * muss die Milestones mitprüfen.
     */
    public int iterationNumberOf(Milestone milestone) {
        return programIncrement.iterationOf(milestone.date()).number();
    }
}
