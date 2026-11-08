package ch.grudligstrasse.pickel.domain.model;

/** Ein Feature verletzt eine fachliche Regel; die Meldung ist für die Oberfläche bestimmt. */
public class InvalidFeatureException extends RuntimeException {

    public InvalidFeatureException(String message) {
        super(message);
    }
}
