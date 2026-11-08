package ch.grudligstrasse.pickel.domain.model;

/** Zu einer ID gibt es im PI keinen Milestone. */
public class MilestoneNotFoundException extends RuntimeException {

    public MilestoneNotFoundException() {
        super("Den Milestone gibt es nicht.");
    }
}
