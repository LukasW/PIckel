package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

    private static Iteration iteration(int number, String name, String start, String end, boolean ip) {
        return new Iteration(number, name, LocalDate.parse(start), LocalDate.parse(end), ip);
    }
}
