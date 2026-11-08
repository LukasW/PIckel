package ch.grudligstrasse.pikel.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Die Kapazität eines Teams in einer Entwicklungs-Iteration eines PI, in Story Points (ADR 0006). Die Iteration ist
 * über ihre Nummer im PI bestimmt; das PI hält der Aufrufer.
 */
public record Capacity(UUID teamId, int iterationNumber, int points) {

    private static final String INVALID_POINTS_MESSAGE = "Die Kapazität muss eine ganze Zahl ab 0 sein.";

    public Capacity {
        Objects.requireNonNull(teamId, "teamId");
        if (points < 0) {
            throw new InvalidCapacityException(INVALID_POINTS_MESSAGE);
        }
    }

    /**
     * Prüft Punkte und Iteration gegen das PI und legt die Kapazität an.
     *
     * @param points {@code null} gilt als ungültig
     * @throws InvalidCapacityException bei Punkten unter 0 oder ohne Wert, bei der IP-Iteration oder wenn das PI
     *     die Iteration nicht hat
     */
    public static Capacity plan(ProgramIncrement programIncrement, UUID teamId, int iterationNumber, Integer points) {
        Iteration iteration = programIncrement.iterations().stream()
                .filter(candidate -> candidate.number() == iterationNumber)
                .findFirst()
                .orElseThrow(() -> new InvalidCapacityException("Die Iteration gibt es nicht."));
        if (iteration.innovationAndPlanning()) {
            throw new InvalidCapacityException("In der IP-Iteration wird keine Kapazität geplant.");
        }
        if (points == null) {
            throw new InvalidCapacityException(INVALID_POINTS_MESSAGE);
        }
        return new Capacity(teamId, iterationNumber, points);
    }
}
