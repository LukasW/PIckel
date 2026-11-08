package ch.grudligstrasse.pikel.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Ein Termin im PI (Release, Messe, Audit) mit Name und Datum; das Datum liegt im Zeitraum des PI. Den Zeitraum
 * prüfen {@link #create} und {@link #update}, weil der Milestone sein PI nur über die ID kennt.
 */
public record Milestone(UUID id, UUID programIncrementId, String name, LocalDate date) {

    public static final int MAX_NAME_LENGTH = 100;

    public Milestone {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(programIncrementId, "programIncrementId");
        if (name == null || name.isBlank()) {
            throw new InvalidMilestoneException("Der Name darf nicht leer sein.");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidMilestoneException("Der Name darf höchstens " + MAX_NAME_LENGTH + " Zeichen lang sein.");
        }
        if (date == null) {
            throw new InvalidMilestoneException("Das Datum ist Pflicht.");
        }
    }

    /**
     * Legt einen Milestone mit frischer ID im PI an.
     *
     * @throws InvalidMilestoneException bei leerem oder zu langem Namen, ohne Datum oder Datum ausserhalb des PI
     */
    public static Milestone create(ProgramIncrement programIncrement, String name, LocalDate date) {
        return withinProgramIncrement(
                programIncrement, new Milestone(UUID.randomUUID(), programIncrement.id(), name, date));
    }

    /**
     * Derselbe Milestone mit neuem Namen und Datum; geprüft wie beim Anlegen.
     *
     * @throws InvalidMilestoneException wie {@link #create}
     */
    public Milestone update(ProgramIncrement programIncrement, String newName, LocalDate newDate) {
        return withinProgramIncrement(programIncrement, new Milestone(id, programIncrementId, newName, newDate));
    }

    private static Milestone withinProgramIncrement(ProgramIncrement programIncrement, Milestone milestone) {
        if (milestone.date().isBefore(programIncrement.startDate())
                || milestone.date().isAfter(programIncrement.endDate())) {
            throw new InvalidMilestoneException("Das Datum muss im Zeitraum des PI liegen.");
        }
        return milestone;
    }
}
