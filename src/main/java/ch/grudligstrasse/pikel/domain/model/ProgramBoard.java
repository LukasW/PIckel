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
 * @param scheduled eingeplante Features in Backlog-Reihenfolge; die Zelle steht am Feature
 * @param milestones aufsteigend nach Datum; je ein Marker über der Iteration, in die das Datum fällt
 * @param capacities verfügbare Kapazität je Team und Entwicklungs-Iteration, ohne Eintrag 0; keine Zelle für die
 *     IP-Iteration
 */
public record ProgramBoard(
        ProgramIncrement programIncrement,
        List<Team> teams,
        List<Iteration> iterations,
        List<Feature> backlog,
        List<Feature> scheduled,
        List<Milestone> milestones,
        List<Capacity> capacities) {

    public ProgramBoard {
        Objects.requireNonNull(programIncrement, "programIncrement");
        teams = List.copyOf(teams);
        iterations = List.copyOf(iterations);
        backlog = List.copyOf(backlog);
        scheduled = List.copyOf(scheduled);
        milestones = List.copyOf(milestones);
        capacities = List.copyOf(capacities);
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
