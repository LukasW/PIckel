package ch.grudligstrasse.pickel.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Ein Team des Agile Release Train; gilt für alle PIs (ADR 0005). Der Name wird getrimmt gespeichert. */
public record Team(UUID id, String name) {

    public static final int MAX_NAME_LENGTH = 50;

    public Team {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new InvalidTeamException("Der Teamname darf nicht leer sein.");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidTeamException("Der Teamname darf höchstens " + MAX_NAME_LENGTH + " Zeichen lang sein.");
        }
    }

    /** Ein neues Team mit eigener ID. */
    public static Team create(String name) {
        return new Team(UUID.randomUUID(), name);
    }

    /** Dasselbe Team unter neuem Namen, mit denselben Regeln wie beim Erfassen. */
    public Team rename(String newName) {
        return new Team(id, newName);
    }
}
