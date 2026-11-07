package ch.grudligstrasse.pikel.domain.model;

/** Zu einer ID gibt es kein PI. */
public class ProgramIncrementNotFoundException extends RuntimeException {

    public ProgramIncrementNotFoundException() {
        super("Das PI gibt es nicht.");
    }
}
