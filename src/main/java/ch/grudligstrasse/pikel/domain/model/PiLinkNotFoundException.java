package ch.grudligstrasse.pikel.domain.model;

/** Das PI hat keinen Link mit dieser ID. */
public class PiLinkNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Den Link gibt es nicht.";

    public PiLinkNotFoundException() {
        super(MESSAGE);
    }
}
