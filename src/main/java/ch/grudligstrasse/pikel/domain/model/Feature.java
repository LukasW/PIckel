package ch.grudligstrasse.pikel.domain.model;

import java.util.Objects;
import java.util.Optional;
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
        OptionalInt estimate,
        Optional<Scheduling> scheduling) {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 5000;
    public static final int MAX_OWNER_LENGTH = 100;
    public static final String INVALID_WSJF_RANK = "Der WSJF-Rang muss eine ganze Zahl ab 1 sein.";
    public static final String INVALID_ESTIMATE = "Die Schätzung muss eine ganze Zahl ab 0 sein.";
    public static final String IP_ITERATION_NOT_SCHEDULABLE = "In der IP-Iteration wird keine Feature-Arbeit geplant.";
    public static final String ITERATION_NOT_IN_PROGRAM_INCREMENT = "Die Iteration gehört nicht zu diesem PI.";

    public Feature {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(programIncrementId, "programIncrementId");
        Objects.requireNonNull(wsjfRank, "wsjfRank");
        Objects.requireNonNull(estimate, "estimate");
        Objects.requireNonNull(scheduling, "scheduling");
        if (title == null || title.isBlank()) {
            throw new InvalidFeatureException("Der Titel darf nicht leer sein.");
        }
        title = stripWithin(title, MAX_TITLE_LENGTH, "Der Titel");
        description = stripWithin(description, MAX_DESCRIPTION_LENGTH, "Die Beschreibung");
        owner = stripWithin(owner, MAX_OWNER_LENGTH, "Der Owner");
        if (wsjfRank.isPresent() && wsjfRank.getAsInt() < 1) {
            throw new InvalidFeatureException(INVALID_WSJF_RANK);
        }
        if (estimate.isPresent() && estimate.getAsInt() < 0) {
            throw new InvalidFeatureException(INVALID_ESTIMATE);
        }
    }

    /** Ein Feature im Backlog, also ohne Einplanung. */
    public Feature(
            UUID id,
            UUID programIncrementId,
            String title,
            String description,
            String owner,
            OptionalInt wsjfRank,
            OptionalInt estimate) {
        this(id, programIncrementId, title, description, owner, wsjfRank, estimate, Optional.empty());
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
                toOptional(wsjfRank),
                toOptional(estimate));
    }

    private static OptionalInt toOptional(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    /** Dasselbe Feature (ID und PI bleiben) mit neuen Angaben und denselben Regeln wie beim Erfassen. */
    public Feature revise(String title, String description, Integer wsjfRank, String owner, Integer estimate) {
        return new Feature(
                id,
                programIncrementId,
                title,
                description,
                owner,
                toOptional(wsjfRank),
                toOptional(estimate),
                scheduling);
    }

    /**
     * Dasselbe Feature in der Zelle Team × Iteration; eine bestehende Einplanung wird ersetzt.
     *
     * @throws InvalidFeatureException wenn das PI nicht das des Features ist, die Iteration nicht zum PI gehört oder
     *     die IP-Iteration ist
     */
    public Feature schedule(ProgramIncrement programIncrement, UUID teamId, int iterationNumber) {
        if (!programIncrement.id().equals(programIncrementId)) {
            throw new InvalidFeatureException(ITERATION_NOT_IN_PROGRAM_INCREMENT);
        }
        Iteration iteration = programIncrement
                .iteration(iterationNumber)
                .orElseThrow(() -> new InvalidFeatureException(ITERATION_NOT_IN_PROGRAM_INCREMENT));
        if (iteration.innovationAndPlanning()) {
            throw new InvalidFeatureException(IP_ITERATION_NOT_SCHEDULABLE);
        }
        return withScheduling(Optional.of(new Scheduling(Objects.requireNonNull(teamId, "teamId"), iterationNumber)));
    }

    /** Dasselbe Feature zurück im Backlog; ohne Einplanung bleibt es unverändert. */
    public Feature unschedule() {
        return withScheduling(Optional.empty());
    }

    /** Ob das Feature auf dem Program Board in einer Zelle liegt. */
    public boolean scheduled() {
        return scheduling.isPresent();
    }

    private Feature withScheduling(Optional<Scheduling> newScheduling) {
        return new Feature(id, programIncrementId, title, description, owner, wsjfRank, estimate, newScheduling);
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
