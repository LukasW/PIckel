package ch.grudligstrasse.pickel.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Ein Program Increment (PI) des Agile Release Train: ein benannter Zeitraum, für den geplant wird.
 */
public record ProgramIncrement(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        int iterationLengthWeeks,
        String businessContext,
        String artObjectives,
        List<PiLink> links) {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int DEFAULT_ITERATION_LENGTH_WEEKS = 2;
    public static final int MIN_ITERATION_LENGTH_WEEKS = 1;
    public static final int MAX_ITERATION_LENGTH_WEEKS = 4;
    /** Mindestens eine Entwicklungs- und die IP-Iteration. */
    public static final int MIN_ITERATIONS = 2;
    /** Höchstlänge von Business-Kontext und PI Objectives des ART. */
    public static final int MAX_TEXT_LENGTH = 10_000;
    // Mit Tausender-Leerschlag wie in der Oberfläche; daher nicht aus MAX_TEXT_LENGTH zusammengesetzt.
    private static final String TEXT_TOO_LONG_MESSAGE = "Der Text darf höchstens 10 000 Zeichen lang sein.";

    private static final int DAYS_PER_WEEK = 7;
    private static final String ITERATION_NAME_PREFIX = "Iteration ";
    private static final String IP_ITERATION_NAME = "IP";

    public ProgramIncrement {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new InvalidProgramIncrementException("Der Name darf nicht leer sein.");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidProgramIncrementException(
                    "Der Name darf höchstens " + MAX_NAME_LENGTH + " Zeichen lang sein.");
        }
        if (startDate == null || endDate == null) {
            throw new InvalidProgramIncrementException("Start- und Enddatum sind Pflicht.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new InvalidProgramIncrementException("Das Enddatum muss nach dem Startdatum liegen.");
        }
        // Zuerst die Länge: Die Mindestdauer hängt von ihr ab.
        if (iterationLengthWeeks < MIN_ITERATION_LENGTH_WEEKS || iterationLengthWeeks > MAX_ITERATION_LENGTH_WEEKS) {
            throw new InvalidProgramIncrementException("Die Iterationslänge muss zwischen " + MIN_ITERATION_LENGTH_WEEKS
                    + " und " + MAX_ITERATION_LENGTH_WEEKS + " Wochen liegen.");
        }
        if (iterationCount(startDate, endDate, iterationLengthWeeks) < MIN_ITERATIONS) {
            throw new InvalidProgramIncrementException("Der Zeitraum reicht nicht für mindestens zwei Iterationen.");
        }
        // Texte ohne Trimmen: Zeilenumbrüche am Rand gehören zum Text, den der RTE erfasst hat.
        businessContext = requireTextWithinLimit(businessContext);
        artObjectives = requireTextWithinLimit(artObjectives);
        links = links == null ? List.of() : List.copyOf(links);
    }

    /** Ein PI ohne Business-Kontext, PI Objectives und Links. */
    public ProgramIncrement(UUID id, String name, LocalDate startDate, LocalDate endDate, int iterationLengthWeeks) {
        this(id, name, startDate, endDate, iterationLengthWeeks, "", "", List.of());
    }

    private static String requireTextWithinLimit(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new InvalidProgramIncrementException(TEXT_TOO_LONG_MESSAGE);
        }
        return text;
    }

    /** Dasselbe PI mit neuem Business-Kontext und neuen PI Objectives; {@code null} gilt als leerer Text. */
    public ProgramIncrement withBusinessContext(String newBusinessContext, String newArtObjectives) {
        return new ProgramIncrement(
                id, name, startDate, endDate, iterationLengthWeeks, newBusinessContext, newArtObjectives, links);
    }

    /** Dasselbe PI mit einem Link mehr (am Ende der Liste). */
    public ProgramIncrement withLink(PiLink link) {
        List<PiLink> newLinks = new ArrayList<>(links);
        newLinks.add(Objects.requireNonNull(link, "link"));
        return new ProgramIncrement(
                id, name, startDate, endDate, iterationLengthWeeks, businessContext, artObjectives, newLinks);
    }

    /**
     * Dasselbe PI ohne den Link mit dieser ID.
     *
     * @throws PiLinkNotFoundException wenn das PI keinen solchen Link hat
     */
    public ProgramIncrement withoutLink(UUID linkId) {
        List<PiLink> newLinks =
                links.stream().filter(link -> !link.id().equals(linkId)).toList();
        if (newLinks.size() == links.size()) {
            throw new PiLinkNotFoundException();
        }
        return new ProgramIncrement(
                id, name, startDate, endDate, iterationLengthWeeks, businessContext, artObjectives, newLinks);
    }

    /**
     * Legt ein neues PI mit frischer ID an. Ohne Iterationslänge ({@code null}) gelten
     * {@link #DEFAULT_ITERATION_LENGTH_WEEKS} Wochen.
     */
    public static ProgramIncrement plan(
            String name, LocalDate startDate, LocalDate endDate, Integer iterationLengthWeeks) {
        return new ProgramIncrement(
                UUID.randomUUID(),
                name,
                startDate,
                endDate,
                iterationLengthWeeks == null ? DEFAULT_ITERATION_LENGTH_WEEKS : iterationLengthWeeks);
    }

    /** Die Iterationen ab dem Startdatum lückenlos bis zum Enddatum; die letzte ist die IP-Iteration. */
    public List<Iteration> iterations() {
        int count = iterationCount(startDate, endDate, iterationLengthWeeks);
        int lengthDays = iterationLengthWeeks * DAYS_PER_WEEK;
        List<Iteration> iterations = new ArrayList<>(count);
        for (int number = 1; number <= count; number++) {
            LocalDate start = startDate.plusDays((long) (number - 1) * lengthDays);
            boolean last = number == count;
            LocalDate end = last ? endDate : start.plusDays(lengthDays - 1L);
            String iterationName = last ? IP_ITERATION_NAME : ITERATION_NAME_PREFIX + number;
            iterations.add(new Iteration(number, iterationName, start, end, last));
        }
        return List.copyOf(iterations);
    }

    /**
     * Die Iteration, in deren Zeitraum das Datum liegt.
     *
     * @throws IllegalArgumentException wenn das Datum ausserhalb des PI liegt
     */
    public Iteration iterationOf(LocalDate date) {
        return iterations().stream()
                .filter(iteration -> !date.isBefore(iteration.startDate()) && !date.isAfter(iteration.endDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Das Datum liegt ausserhalb des PI."));
    }

    /** Die Iteration mit dieser Nummer (ab 1); leer, wenn es sie im PI nicht gibt. */
    public Optional<Iteration> iteration(int number) {
        return iterations().stream()
                .filter(iteration -> iteration.number() == number)
                .findFirst();
    }

    /** Anzahl Iterationen: Zeitraum inklusiv geteilt durch die Länge, aufgerundet; ein Rest wird zur kürzeren IP. */
    private static int iterationCount(LocalDate startDate, LocalDate endDate, int iterationLengthWeeks) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long lengthDays = (long) iterationLengthWeeks * DAYS_PER_WEEK;
        return (int) ((days + lengthDays - 1) / lengthDays);
    }
}
