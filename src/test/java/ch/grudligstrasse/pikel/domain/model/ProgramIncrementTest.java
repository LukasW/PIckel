package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProgramIncrementTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 4);
    private static final LocalDate END = LocalDate.of(2027, 3, 26);

    @Test
    void planTrimsName() {
        ProgramIncrement pi = ProgramIncrement.plan("  PI 27.1 ", START, END);

        assertEquals("PI 27.1", pi.name());
    }

    @Test
    void planKeepsDatesAndAssignsId() {
        ProgramIncrement pi = ProgramIncrement.plan("PI 27.1", START, END);

        assertNotNull(pi.id());
        assertEquals(START, pi.startDate());
        assertEquals(END, pi.endDate());
    }

    @Test
    void acceptsNameOfMaximumLength() {
        String name = "x".repeat(ProgramIncrement.MAX_NAME_LENGTH);

        assertEquals(name, ProgramIncrement.plan(name, START, END).name());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan(" ", START, END));
    }

    @Test
    void rejectsTooLongName() {
        String name = "x".repeat(ProgramIncrement.MAX_NAME_LENGTH + 1);

        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan(name, START, END));
    }

    @Test
    void rejectsEndDateNotAfterStartDate() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, START));
    }

    @Test
    void rejectsNullName() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan(null, START, END));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", END, START));
    }

    @Test
    void rejectsMissingEndDate() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, null));
    }

    @Test
    void rejectsMissingDates() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", null, END));
    }
}
