package ch.grudligstrasse.pickel.domain.model;

/** Zu einer ID gibt es im PI kein Feature. */
public class FeatureNotFoundException extends RuntimeException {

    public FeatureNotFoundException() {
        super("Das Feature gibt es nicht.");
    }
}
