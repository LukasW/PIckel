package ch.grudligstrasse.pickel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProgramIncrementTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 4);
    private static final LocalDate END = LocalDate.of(2027, 3, 26);
    private static final Integer TWO_WEEKS = 2;
    private static final String LENGTH_MESSAGE = "Die Iterationslänge muss zwischen 1 und 4 Wochen liegen.";
    private static final String TOO_SHORT_MESSAGE = "Der Zeitraum reicht nicht für mindestens zwei Iterationen.";

    @Test
    void planTrimsName() {
        ProgramIncrement pi = ProgramIncrement.plan("  PI 27.1 ", START, END, null);

        assertEquals("PI 27.1", pi.name());
    }

    @Test
    void planKeepsDatesAndAssignsId() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, null);

        assertNotNull(pi.id());
        assertEquals(START, pi.startDate());
        assertEquals(END, pi.endDate());
    }

    @Test
    void acceptsNameOfMaximumLength() {
        String name = "x".repeat(ProgramIncrement.MAX_NAME_LENGTH);

        assertEquals(name, ProgramIncrement.plan(name, START, END, null).name());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan(" ", START, END, null));
    }

    @Test
    void rejectsTooLongName() {
        String name = "x".repeat(ProgramIncrement.MAX_NAME_LENGTH + 1);

        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan(name, START, END, null));
    }

    @Test
    void rejectsEndDateNotAfterStartDate() {
        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, START, null));

        assertEquals("Das Enddatum muss nach dem Startdatum liegen.", exception.getMessage());
    }

    @Test
    void rejectsNullName() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan(null, START, END, null));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", END, START, null));
    }

    @Test
    void rejectsMissingEndDate() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, null, null));
    }

    @Test
    void rejectsMissingDates() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", null, END, null));
    }

    // US-5

    @Test
    void planWithoutIterationLengthUsesTwoWeeks() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, null);

        assertEquals(2, pi.iterationLengthWeeks());
        assertEquals(6, pi.iterations().size());
    }

    @Test
    void planKeepsChosenIterationLength() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, 3);

        assertEquals(3, pi.iterationLengthWeeks());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void acceptsIterationLengthAtTheLimits(int weeks) {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, weeks);

        assertEquals(weeks, pi.iterationLengthWeeks());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    void rejectsIterationLengthOutsideOneToFourWeeks(int weeks) {
        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, END, weeks));

        assertEquals(LENGTH_MESSAGE, exception.getMessage());
    }

    // Annahme US-5: Die Mindestdauer hängt von der Länge ab, deshalb meldet eine ungültige Länge sich zuerst.
    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    void invalidIterationLengthIsReportedBeforeTooShortPeriod(int weeks) {
        LocalDate oneWeekLater = LocalDate.of(2027, 1, 10);

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class,
                () -> ProgramIncrement.plan("PI 27.1", START, oneWeekLater, weeks));

        assertEquals(LENGTH_MESSAGE, exception.getMessage());
    }

    // Ein PI aus der Datenbank geht durch denselben Konstruktor; ein ungültiges darf auch dort nicht entstehen.
    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    void constructorRejectsIterationLengthOutsideOneToFourWeeks(int weeks) {
        UUID id = UUID.randomUUID();

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class, () -> new ProgramIncrement(id, "PI 27.1", START, END, weeks));

        assertEquals(LENGTH_MESSAGE, exception.getMessage());
    }

    @Test
    void iterationsFollowEachOtherFromStartDateAndLastIsIp() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);

        assertEquals(
                List.of(
                        iteration(1, "Iteration 1", "2027-01-04", "2027-01-17", false),
                        iteration(2, "Iteration 2", "2027-01-18", "2027-01-31", false),
                        iteration(3, "Iteration 3", "2027-02-01", "2027-02-14", false),
                        iteration(4, "Iteration 4", "2027-02-15", "2027-02-28", false),
                        iteration(5, "Iteration 5", "2027-03-01", "2027-03-14", false),
                        iteration(6, "IP", "2027-03-15", "2027-03-26", true)),
                pi.iterations());
    }

    @Test
    void iterationsUseChosenLength() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, 4);

        assertEquals(
                List.of(
                        iteration(1, "Iteration 1", "2027-01-04", "2027-01-31", false),
                        iteration(2, "Iteration 2", "2027-02-01", "2027-02-28", false),
                        iteration(3, "IP", "2027-03-01", "2027-03-26", true)),
                pi.iterations());
    }

    @Test
    void lastIterationIsShorterWhenPeriodHasRemainder() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, LocalDate.parse("2027-02-07"), TWO_WEEKS);

        assertEquals(
                List.of(
                        iteration(1, "Iteration 1", "2027-01-04", "2027-01-17", false),
                        iteration(2, "Iteration 2", "2027-01-18", "2027-01-31", false),
                        iteration(3, "IP", "2027-02-01", "2027-02-07", true)),
                pi.iterations());
    }

    @Test
    void periodThatDividesEvenlyEndsWithFullIpIteration() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, LocalDate.parse("2027-02-28"), TWO_WEEKS);

        assertEquals(
                List.of(
                        iteration(1, "Iteration 1", "2027-01-04", "2027-01-17", false),
                        iteration(2, "Iteration 2", "2027-01-18", "2027-01-31", false),
                        iteration(3, "Iteration 3", "2027-02-01", "2027-02-14", false),
                        iteration(4, "IP", "2027-02-15", "2027-02-28", true)),
                pi.iterations());
    }

    @Test
    void rejectsPeriodShorterThanTwoIterations() {
        LocalDate end = LocalDate.parse("2027-01-10");

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, end, TWO_WEEKS));

        assertEquals(TOO_SHORT_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsPeriodOfExactlyOneIteration() {
        LocalDate end = LocalDate.parse("2027-01-17");

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, end, TWO_WEEKS));

        assertEquals(TOO_SHORT_MESSAGE, exception.getMessage());
    }

    @Test
    void acceptsPeriodOneDayLongerThanOneIterationWithOneDayIp() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, LocalDate.parse("2027-01-18"), TWO_WEEKS);

        assertEquals(
                List.of(
                        iteration(1, "Iteration 1", "2027-01-04", "2027-01-17", false),
                        iteration(2, "IP", "2027-01-18", "2027-01-18", true)),
                pi.iterations());
    }

    // Gleicher Zeitraum wie rejectsPeriodOfExactlyOneIteration, nur die Länge weicht ab: Die Mindestdauer hängt
    // an der Länge, nicht an einer festen Zahl von Tagen.
    @Test
    void minimumPeriodDependsOnIterationLength() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, LocalDate.parse("2027-01-17"), 1);

        assertEquals(
                List.of(
                        iteration(1, "Iteration 1", "2027-01-04", "2027-01-10", false),
                        iteration(2, "IP", "2027-01-11", "2027-01-17", true)),
                pi.iterations());
    }

    @Test
    void endDateBeforeStartDateKeepsMessageOfUs1() {
        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", END, START, TWO_WEEKS));

        assertEquals("Das Enddatum muss nach dem Startdatum liegen.", exception.getMessage());
    }

    // US-8

    private static final String TOO_LONG_TEXT_MESSAGE = "Der Text darf höchstens 10 000 Zeichen lang sein.";
    private static final String VISION_URL = "https://wiki.example.ch/vision";

    @Test
    void planStartsWithoutBusinessContextObjectivesAndLinks() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, null);

        assertEquals("", pi.businessContext());
        assertEquals("", pi.artObjectives());
        assertEquals(List.of(), pi.links());
    }

    @Test
    void withBusinessContextReplacesBothTextsAndKeepsTheRest() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        ProgramIncrement pi = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.1", START, END, 3, "Alter Kontext", "Alte Ziele", List.of(vision));

        ProgramIncrement changed = pi.withBusinessContext("Fokus auf Self-Service", "Checkout in 2 Schritten");

        assertEquals("Fokus auf Self-Service", changed.businessContext());
        assertEquals("Checkout in 2 Schritten", changed.artObjectives());
        assertEquals(pi.id(), changed.id());
        assertEquals("PI 27.1", changed.name());
        assertEquals(START, changed.startDate());
        assertEquals(END, changed.endDate());
        assertEquals(3, changed.iterationLengthWeeks());
        assertEquals(List.of(vision), changed.links());
    }

    @Test
    void withBusinessContextKeepsLineBreaks() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, null);

        ProgramIncrement changed = pi.withBusinessContext("Zeile 1\nZeile 2", "Ziel 1\n\nZiel 2");

        assertEquals("Zeile 1\nZeile 2", changed.businessContext());
        assertEquals("Ziel 1\n\nZiel 2", changed.artObjectives());
    }

    @Test
    void withBusinessContextTreatsNullAsEmptyText() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, null)
                .withBusinessContext("Fokus auf Self-Service", "Checkout in 2 Schritten");

        ProgramIncrement cleared = pi.withBusinessContext(null, null);

        assertEquals("", cleared.businessContext());
        assertEquals("", cleared.artObjectives());
    }

    @Test
    void acceptsTextsOfMaximumLength() {
        String text = "x".repeat(ProgramIncrement.MAX_TEXT_LENGTH);

        ProgramIncrement changed =
                ProgramIncrement.plan("PI 27.1", START, END, null).withBusinessContext(text, text);

        assertEquals(text, changed.businessContext());
        assertEquals(text, changed.artObjectives());
    }

    @Test
    void rejectsBusinessContextLongerThan10000Characters() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, null);
        String text = "x".repeat(10_001);

        InvalidProgramIncrementException exception =
                assertThrows(InvalidProgramIncrementException.class, () -> pi.withBusinessContext(text, ""));

        assertEquals(TOO_LONG_TEXT_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsArtObjectivesLongerThan10000Characters() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, null);
        String text = "x".repeat(10_001);

        InvalidProgramIncrementException exception =
                assertThrows(InvalidProgramIncrementException.class, () -> pi.withBusinessContext("", text));

        assertEquals(TOO_LONG_TEXT_MESSAGE, exception.getMessage());
    }

    // Ein PI aus der Datenbank geht durch denselben Konstruktor; ein ungültiges darf auch dort nicht entstehen.
    @Test
    void constructorRejectsBusinessContextLongerThan10000Characters() {
        UUID id = UUID.randomUUID();
        String text = "x".repeat(10_001);

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class,
                () -> new ProgramIncrement(id, "PI 27.1", START, END, 2, text, "", List.of()));

        assertEquals(TOO_LONG_TEXT_MESSAGE, exception.getMessage());
    }

    @Test
    void constructorRejectsArtObjectivesLongerThan10000Characters() {
        UUID id = UUID.randomUUID();
        String text = "x".repeat(10_001);

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class,
                () -> new ProgramIncrement(id, "PI 27.1", START, END, 2, "", text, List.of()));

        assertEquals(TOO_LONG_TEXT_MESSAGE, exception.getMessage());
    }

    @Test
    void withLinkAppendsLinkAtTheEndAndKeepsTexts() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        PiLink roadmap = new PiLink(UUID.randomUUID(), "Roadmap", "http://wiki.example.ch/roadmap");
        ProgramIncrement pi =
                new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2, "Kontext", "Ziele", List.of(vision));

        ProgramIncrement changed = pi.withLink(roadmap);

        assertEquals(List.of(vision, roadmap), changed.links());
        assertEquals("Kontext", changed.businessContext());
        assertEquals("Ziele", changed.artObjectives());
        assertEquals(pi.id(), changed.id());
    }

    @Test
    void withoutLinkRemovesOnlyThatLinkAndKeepsOrder() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        PiLink roadmap = new PiLink(UUID.randomUUID(), "Roadmap", "http://wiki.example.ch/roadmap");
        PiLink board = new PiLink(UUID.randomUUID(), "Board", "https://wiki.example.ch/board");
        ProgramIncrement pi = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.1", START, END, 2, "Kontext", "Ziele", List.of(vision, roadmap, board));

        ProgramIncrement changed = pi.withoutLink(roadmap.id());

        assertEquals(List.of(vision, board), changed.links());
        assertEquals("Kontext", changed.businessContext());
        assertEquals("Ziele", changed.artObjectives());
    }

    @Test
    void withoutLinkRejectsUnknownLink() {
        PiLink vision = new PiLink(UUID.randomUUID(), "Vision", VISION_URL);
        ProgramIncrement pi =
                new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2, "", "", List.of(vision));
        UUID unknown = UUID.randomUUID();

        PiLinkNotFoundException exception = assertThrows(PiLinkNotFoundException.class, () -> pi.withoutLink(unknown));

        assertEquals("Den Link gibt es nicht.", exception.getMessage());
    }

    // US-15: Position eines Milestones. PI 27.1 zu 2 Wochen: Iteration 1 ab 2027-01-04, Iteration 2 ab 01-18,
    // Iteration 3 ab 02-01, Iteration 4 ab 02-15, Iteration 5 ab 03-01, IP 2027-03-15 bis 03-26.

    @ParameterizedTest
    @CsvSource({
        "2027-01-04, 1, Iteration 1",
        "2027-01-17, 1, Iteration 1",
        "2027-01-18, 2, Iteration 2",
        "2027-02-10, 3, Iteration 3",
        "2027-02-14, 3, Iteration 3",
        "2027-03-14, 5, Iteration 5",
        "2027-03-15, 6, IP",
        "2027-03-26, 6, IP"
    })
    void iterationOfFindsIterationWhosePeriodContainsTheDate(String date, int number, String name) {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);

        Iteration iteration = pi.iterationOf(LocalDate.parse(date));

        assertEquals(number, iteration.number());
        assertEquals(name, iteration.name());
    }

    @Test
    void iterationOfReturnsTheWholeIteration() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);

        assertEquals(
                iteration(3, "Iteration 3", "2027-02-01", "2027-02-14", false),
                pi.iterationOf(LocalDate.parse("2027-02-10")));
    }

    // Gleiches Datum wie beim Szenario, nur die Länge weicht ab: zu 4 Wochen liegt der 10. Februar in Iteration 2.
    @Test
    void iterationOfDependsOnIterationLength() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, 4);

        assertEquals(
                iteration(2, "Iteration 2", "2027-02-01", "2027-02-28", false),
                pi.iterationOf(LocalDate.parse("2027-02-10")));
    }

    // Die kürzere IP-Iteration bei einem Rest bis zum Enddatum.
    @Test
    void iterationOfFindsShorterIpIterationUpToEndDate() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, LocalDate.parse("2027-02-07"), TWO_WEEKS);

        assertEquals(
                iteration(3, "IP", "2027-02-01", "2027-02-07", true), pi.iterationOf(LocalDate.parse("2027-02-07")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2027-01-03", "2027-03-27", "2027-04-01"})
    void iterationOfRejectsDateOutsideProgramIncrement(String date) {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);
        LocalDate outside = LocalDate.parse(date);

        assertThrows(IllegalArgumentException.class, () -> pi.iterationOf(outside));
    }

    // US-13: Die Einplanung nennt die Iteration über ihre Nummer im PI (1 bis Anzahl).

    @Test
    void iterationByNumberFindsFirstIteration() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);

        assertEquals(Optional.of(iteration(1, "Iteration 1", "2027-01-04", "2027-01-17", false)), pi.iteration(1));
    }

    @Test
    void iterationByNumberFindsMiddleIteration() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);

        assertEquals(Optional.of(iteration(3, "Iteration 3", "2027-02-01", "2027-02-14", false)), pi.iteration(3));
    }

    @Test
    void iterationByNumberFindsIpIterationAsLast() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);

        assertEquals(Optional.of(iteration(6, "IP", "2027-03-15", "2027-03-26", true)), pi.iteration(6));
    }

    // Die Anzahl hängt von der Länge ab: zu 4 Wochen ist 3 die IP-Iteration und 4 gibt es nicht.
    @Test
    void iterationByNumberDependsOnIterationLength() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, 4);

        assertEquals(Optional.of(iteration(3, "IP", "2027-03-01", "2027-03-26", true)), pi.iteration(3));
        assertEquals(Optional.empty(), pi.iteration(4));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void iterationByNumberIsEmptyOutsideProgramIncrement(int number) {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END, TWO_WEEKS);

        assertEquals(Optional.empty(), pi.iteration(number));
    }

    private static Iteration iteration(int number, String name, String start, String end, boolean ip) {
        return new Iteration(number, name, LocalDate.parse(start), LocalDate.parse(end), ip);
    }
}
