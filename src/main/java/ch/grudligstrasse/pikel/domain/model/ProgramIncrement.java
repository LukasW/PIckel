package ch.grudligstrasse.pikel.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Ein Program Increment (PI) des Agile Release Train: ein benannter Zeitraum, für den geplant wird.
 */
public record ProgramIncrement(UUID id, String name, LocalDate startDate, LocalDate endDate) {

    public static final int MAX_NAME_LENGTH = 100;

    public ProgramIncrement {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new InvalidProgramIncrementException("Der Name darf nicht leer sein.");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidProgramIncrementException(
                    "Der Name darf höchstens " + MAX_NAME_LENGTH + " Zeichen lang sein.");
        }
        if (startDate == null || endDate == null) {
            throw new InvalidProgramIncrementException("Start- und Enddatum sind Pflicht.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new InvalidProgramIncrementException("Das Enddatum muss nach dem Startdatum liegen.");
        }
    }

    /** Legt ein neues PI mit frischer ID an. */
    public static ProgramIncrement plan(String name, LocalDate startDate, LocalDate endDate) {
        return new ProgramIncrement(UUID.randomUUID(), name, startDate, endDate);
    }
}
