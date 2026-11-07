package ch.grudligstrasse.pikel.domain.model;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Ein Feature im Backlog eines PI (ADR 0005). Titel, Beschreibung und Owner werden getrimmt gespeichert; fehlende
 * Beschreibung und fehlender Owner sind der leere Text.
 */
public record Feature(
        UUID id,
        UUID programIncrementId,
        String title,
        String description,
        String owner,
        OptionalInt wsjfRank,
        OptionalInt estimate) {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 5000;
    public static final int MAX_OWNER_LENGTH = 100;

    public Feature {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(programIncrementId, "programIncrementId");
        Objects.requireNonNull(wsjfRank, "wsjfRank");
        Objects.requireNonNull(estimate, "estimate");
        if (title == null || title.isBlank()) {
            throw new InvalidFeatureException("Der Titel darf nicht leer sein.");
        }
        title = stripWithin(title, MAX_TITLE_LENGTH, "Der Titel");
        description = stripWithin(description, MAX_DESCRIPTION_LENGTH, "Die Beschreibung");
        owner = stripWithin(owner, MAX_OWNER_LENGTH, "Der Owner");
        if (wsjfRank.isPresent() && wsjfRank.getAsInt() < 1) {
            throw new InvalidFeatureException("Der WSJF-Rang muss eine ganze Zahl ab 1 sein.");
        }
        if (estimate.isPresent() && estimate.getAsInt() < 0) {
            throw new InvalidFeatureException("Die Schätzung muss eine ganze Zahl ab 0 sein.");
        }
    }

    /** Ein neues Feature mit eigener ID in einem PI; Rang und Schätzung sind optional (null = fehlt). */
    public static Feature create(
            UUID programIncrementId,
            String title,
            String description,
            Integer wsjfRank,
            String owner,
            Integer estimate) {
        return new Feature(
                UUID.randomUUID(),
                programIncrementId,
                title,
                description,
                owner,
                wsjfRank == null ? OptionalInt.empty() : OptionalInt.of(wsjfRank),
                estimate == null ? OptionalInt.empty() : OptionalInt.of(estimate));
    }

    /** Ob das Feature schon auf dem Program Board eingeplant ist; heute nie, die Einplanung kommt mit #13. */
    public boolean scheduled() {
        return false;
    }

    // Fehlender Text ist der leere Text; gemessen wird nach dem Trimmen.
    private static String stripWithin(String text, int max, String field) {
        String stripped = text == null ? "" : text.strip();
        if (stripped.length() > max) {
            throw new InvalidFeatureException(field + " darf höchstens " + max + " Zeichen lang sein.");
        }
        return stripped;
    }
}
