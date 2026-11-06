package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertThrows(
                InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", START, START));
    }

    @Test
    void rejectsMissingDates() {
        assertThrows(InvalidProgramIncrementException.class, () -> ProgramIncrement.plan("PI 27.1", null, END));
    }
}
