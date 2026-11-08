package ch.grudligstrasse.pickel.domain.model;

/** Ein Team verletzt eine fachliche Regel (z. B. leerer Name). */
public class InvalidTeamException extends RuntimeException {

    public InvalidTeamException(String message) {
        super(message);
    }
}
