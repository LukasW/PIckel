package ch.grudligstrasse.pickel.domain.model;

import java.util.UUID;

/**
 * Die Einplanung eines Features: die Zelle Team × Iteration auf dem Program Board. Die Iteration wird über ihre
 * Nummer im PI genannt (siehe {@link Iteration}).
 */
public record Scheduling(UUID teamId, int iterationNumber) {

    public static final String MISSING_CELL = "Team und Iteration sind Pflicht.";

    /** Die Zelle aus Angaben von aussen; fehlt eine, ist das ein ungültiges Einplanen. */
    public static Scheduling of(UUID teamId, Integer iterationNumber) {
        if (teamId == null || iterationNumber == null) {
            throw new InvalidFeatureException(MISSING_CELL);
        }
        return new Scheduling(teamId, iterationNumber);
    }
}
