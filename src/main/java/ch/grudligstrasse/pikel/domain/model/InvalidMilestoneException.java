package ch.grudligstrasse.pikel.domain.model;

/** Ein Milestone verletzt eine fachliche Regel (z. B. leerer Name oder Datum ausserhalb des PI). */
public class InvalidMilestoneException extends RuntimeException {

    public InvalidMilestoneException(String message) {
        super(message);
    }
}
