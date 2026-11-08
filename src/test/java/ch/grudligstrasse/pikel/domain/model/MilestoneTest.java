package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** US-15: Name (Pflicht, getrimmt, höchstens 100 Zeichen) und Datum im Zeitraum des PI. */
class MilestoneTest {

    // Zeitraum aus den Szenarien des Deltas; beide Grenzen gehören zum PI.
    private static final LocalDate START = LocalDate.of(2027, 1, 4);
    private static final LocalDate END = LocalDate.of(2027, 3, 26);
    private static final ProgramIncrement PI = new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2);
    private static final LocalDate RELEASE_DATE = LocalDate.of(2027, 2, 10);

    private static final String BLANK_NAME = "Der Name darf nicht leer sein.";
    private static final String TOO_LONG_NAME = "Der Name darf höchstens 100 Zeichen lang sein.";
    private static final String DATE_MISSING = "Das Datum ist Pflicht.";
    private static final String DATE_OUTSIDE = "Das Datum muss im Zeitraum des PI liegen.";

    // --- AK 1: Name und Datum ---

    @Test
    void createKeepsNameDateAndProgramIncrementAndAssignsId() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);

        assertNotNull(milestone.id());
        assertEquals(PI.id(), milestone.programIncrementId());
        assertEquals("Release 4.0", milestone.name());
        assertEquals(RELEASE_DATE, milestone.date());
    }

    @Test
    void createAssignsFreshIdEachTime() {
        Milestone first = Milestone.create(PI, "Release 4.0", RELEASE_DATE);
        Milestone second = Milestone.create(PI, "Release 4.0", RELEASE_DATE);

        assertNotEquals(first.id(), second.id());
    }

    @Test
    void createTrimsName() {
        assertEquals(
                "Release 4.0",
                Milestone.create(PI, "  Release 4.0 ", RELEASE_DATE).name());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  \t "})
    void createRejectsMissingOrBlankName(String name) {
        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> Milestone.create(PI, name, RELEASE_DATE));

        assertEquals(BLANK_NAME, exception.getMessage());
    }

    @Test
    void createAcceptsNameOfExactly100Characters() {
        String name = "x".repeat(100);

        assertEquals(name, Milestone.create(PI, name, RELEASE_DATE).name());
    }

    @Test
    void createRejectsNameOf101Characters() {
        String name = "x".repeat(101);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> Milestone.create(PI, name, RELEASE_DATE));

        assertEquals(TOO_LONG_NAME, exception.getMessage());
    }

    // Die Länge zählt nach dem Trimmen: 100 Zeichen plus Leerzeichen am Rand sind erlaubt.
    @Test
    void createCountsLengthAfterTrimming() {
        String name = "x".repeat(100);

        assertEquals(
                name, Milestone.create(PI, "  " + name + "  ", RELEASE_DATE).name());
    }

    @Test
    void createRejectsMissingDate() {
        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> Milestone.create(PI, "Release 4.0", null));

        assertEquals(DATE_MISSING, exception.getMessage());
    }

    // --- AK 1/2: Datum im Zeitraum des PI, Grenzen inklusive ---

    @Test
    void createAcceptsStartDateOfProgramIncrement() {
        assertEquals(START, Milestone.create(PI, "Kick-off", START).date());
    }

    @Test
    void createAcceptsEndDateOfProgramIncrement() {
        assertEquals(END, Milestone.create(PI, "Abschluss", END).date());
    }

    @Test
    void createRejectsDayBeforeStartOfProgramIncrement() {
        LocalDate dayBefore = START.minusDays(1);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> Milestone.create(PI, "Release 4.0", dayBefore));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
    }

    @Test
    void createRejectsDayAfterEndOfProgramIncrement() {
        LocalDate dayAfter = END.plusDays(1);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> Milestone.create(PI, "Release 4.0", dayAfter));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
    }

    // Szenario «Milestone ausserhalb des PI wird abgelehnt».
    @Test
    void createRejectsDateOfScenarioAfterProgramIncrement() {
        LocalDate april = LocalDate.of(2027, 4, 1);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> Milestone.create(PI, "Release 4.0", april));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
    }

    // Gleiches Datum, anderes PI: Die Grenze kommt aus dem übergebenen PI, nicht aus einem festen Zeitraum.
    @Test
    void createChecksDateAgainstTheGivenProgramIncrement() {
        ProgramIncrement later = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.2", LocalDate.of(2027, 3, 29), LocalDate.of(2027, 6, 18), 2);

        InvalidMilestoneException exception = assertThrows(
                InvalidMilestoneException.class, () -> Milestone.create(later, "Release 4.0", RELEASE_DATE));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
    }

    // --- AK 3: ändern ---

    @Test
    void updateChangesNameAndDateAndKeepsIdAndProgramIncrement() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);
        LocalDate moved = LocalDate.of(2027, 1, 5);

        Milestone updated = milestone.update(PI, "Release 4.1", moved);

        assertEquals(milestone.id(), updated.id());
        assertEquals(PI.id(), updated.programIncrementId());
        assertEquals("Release 4.1", updated.name());
        assertEquals(moved, updated.date());
    }

    @Test
    void updateTrimsName() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);

        assertEquals(
                "Release 4.1",
                milestone.update(PI, " Release 4.1  ", RELEASE_DATE).name());
    }

    @Test
    void updateRejectsBlankName() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> milestone.update(PI, " ", RELEASE_DATE));

        assertEquals(BLANK_NAME, exception.getMessage());
    }

    @Test
    void updateRejectsNameOf101Characters() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);
        String name = "x".repeat(101);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> milestone.update(PI, name, RELEASE_DATE));

        assertEquals(TOO_LONG_NAME, exception.getMessage());
    }

    @Test
    void updateRejectsMissingDate() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> milestone.update(PI, "Release 4.0", null));

        assertEquals(DATE_MISSING, exception.getMessage());
    }

    @Test
    void updateAcceptsEndDateOfProgramIncrement() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);

        assertEquals(END, milestone.update(PI, "Release 4.0", END).date());
    }

    @Test
    void updateRejectsDayAfterEndOfProgramIncrement() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);
        LocalDate dayAfter = END.plusDays(1);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> milestone.update(PI, "Release 4.0", dayAfter));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
    }

    @Test
    void updateRejectsDayBeforeStartOfProgramIncrement() {
        Milestone milestone = Milestone.create(PI, "Release 4.0", RELEASE_DATE);
        LocalDate dayBefore = START.minusDays(1);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> milestone.update(PI, "Release 4.0", dayBefore));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
    }

    // --- Ein Milestone aus der Datenbank geht durch denselben Konstruktor (.claude/rules/java.md) ---

    @Test
    void constructorTrimsName() {
        Milestone milestone = new Milestone(UUID.randomUUID(), PI.id(), " Release 4.0 ", RELEASE_DATE);

        assertEquals("Release 4.0", milestone.name());
    }

    @Test
    void constructorRejectsBlankName() {
        UUID id = UUID.randomUUID();

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> new Milestone(id, PI.id(), " ", RELEASE_DATE));

        assertEquals(BLANK_NAME, exception.getMessage());
    }

    @Test
    void constructorRejectsNameOf101Characters() {
        UUID id = UUID.randomUUID();
        String name = "x".repeat(101);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> new Milestone(id, PI.id(), name, RELEASE_DATE));

        assertEquals(TOO_LONG_NAME, exception.getMessage());
    }

    @Test
    void constructorRejectsMissingDate() {
        UUID id = UUID.randomUUID();

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> new Milestone(id, PI.id(), "Release 4.0", null));

        assertEquals(DATE_MISSING, exception.getMessage());
    }
}
