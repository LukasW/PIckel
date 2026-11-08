package ch.grudligstrasse.pickel.domain.model;

/** Zu einer ID gibt es kein Team. */
public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException() {
        super("Das Team gibt es nicht.");
    }
}
