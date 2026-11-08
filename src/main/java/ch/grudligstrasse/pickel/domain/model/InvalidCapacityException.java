package ch.grudligstrasse.pickel.domain.model;

/** Eine Kapazität verletzt eine fachliche Regel (z. B. negative Punkte oder die IP-Iteration). */
public class InvalidCapacityException extends RuntimeException {

    public InvalidCapacityException(String message) {
        super(message);
    }
}
