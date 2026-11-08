package ch.grudligstrasse.pickel.domain.model;

/** Ein PI verletzt eine fachliche Regel (z. B. Enddatum vor Startdatum). */
public class InvalidProgramIncrementException extends RuntimeException {

    public InvalidProgramIncrementException(String message) {
        super(message);
    }
}
