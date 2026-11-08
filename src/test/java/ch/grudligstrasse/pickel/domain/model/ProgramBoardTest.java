package ch.grudligstrasse.pickel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** US-15 AK 4: Der Marker eines Milestones steht über der Iteration, in die sein Datum fällt. */
class ProgramBoardTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 4);
    private static final LocalDate END = LocalDate.of(2027, 3, 26);

    private static ProgramBoard board(ProgramIncrement pi, Milestone milestone) {
        return new ProgramBoard(
                pi, List.of(), pi.iterations(), List.of(), List.of(), List.of(milestone), List.of(), List.of());
    }

    // PI 27.1 zu 2 Wochen: Iteration 3 vom 2027-02-01 bis 02-14, IP ab 03-15.
    @ParameterizedTest
    @CsvSource({"2027-01-04, 1", "2027-01-17, 1", "2027-01-18, 2", "2027-02-10, 3", "2027-03-15, 6", "2027-03-26, 6"})
    void milestoneStandsOverIterationContainingItsDate(String date, int expectedNumber) {
        ProgramIncrement pi = new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 2);
        Milestone milestone = new Milestone(UUID.randomUUID(), pi.id(), "Release 4.0", LocalDate.parse(date));

        assertEquals(expectedNumber, board(pi, milestone).iterationNumberOf(milestone));
    }

    // Gleiches Datum, nur die Iterationslänge weicht ab: zu 4 Wochen ist der 10. Februar in Iteration 2.
    @Test
    void positionFollowsIterationLengthOfTheProgramIncrement() {
        ProgramIncrement pi = new ProgramIncrement(UUID.randomUUID(), "PI 27.1", START, END, 4);
        Milestone milestone = new Milestone(UUID.randomUUID(), pi.id(), "Release 4.0", LocalDate.of(2027, 2, 10));

        assertEquals(2, board(pi, milestone).iterationNumberOf(milestone));
    }
}
