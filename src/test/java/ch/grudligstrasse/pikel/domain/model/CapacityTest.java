package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CapacityTest {

    private static final String INVALID_POINTS_MESSAGE = "Die Kapazität muss eine ganze Zahl ab 0 sein.";
    private static final String IP_MESSAGE = "In der IP-Iteration wird keine Kapazität geplant.";
    // Annahme aus plan.md → «Unbekannte Iterationsnummer», nicht im Delta.
    private static final String UNKNOWN_ITERATION_MESSAGE = "Die Iteration gibt es nicht.";

    private static final UUID TEAM = UUID.randomUUID();

    // US-5: 2027-01-04 bis 2027-03-26 zu 2 Wochen hat 6 Iterationen, «IP» ist die sechste.
    private static final ProgramIncrement PI_27_1 =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 2);
    private static final int PI_27_1_IP = 6;
    private static final int PI_27_1_LAST_DEVELOPMENT = 5;

    // US-5: 2027-01-04 bis 2027-02-07 zu 2 Wochen hat 3 Iterationen, «IP» ist die dritte.
    private static final ProgramIncrement SHORT_PI =
            new ProgramIncrement(UUID.randomUUID(), "PI kurz", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 2, 7), 2);

    // --- AK 1: für jedes Team und jede Iteration ausser der IP-Iteration ---

    @Test
    void plansCapacityOfTeamInDevelopmentIteration() {
        assertEquals(new Capacity(TEAM, 1, 40), Capacity.plan(PI_27_1, TEAM, 1, 40));
    }

    @Test
    void plansCapacityInLastDevelopmentIteration() {
        assertEquals(
                new Capacity(TEAM, PI_27_1_LAST_DEVELOPMENT, 8),
                Capacity.plan(PI_27_1, TEAM, PI_27_1_LAST_DEVELOPMENT, 8));
    }

    @Test
    void rejectsCapacityInIpIteration() {
        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> Capacity.plan(PI_27_1, TEAM, PI_27_1_IP, 10));

        assertEquals(IP_MESSAGE, exception.getMessage());
    }

    // Welche Nummer die IP-Iteration ist, bestimmt das PI: Nummer 3 ist hier IP, in PI 27.1 eine
    // Entwicklungs-Iteration.
    @Test
    void ipIterationDependsOnProgramIncrement() {
        assertEquals(new Capacity(TEAM, 3, 10), Capacity.plan(PI_27_1, TEAM, 3, 10));

        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> Capacity.plan(SHORT_PI, TEAM, 3, 10));

        assertEquals(IP_MESSAGE, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, PI_27_1_IP + 1})
    void rejectsIterationNumberThePiDoesNotHave(int iterationNumber) {
        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> Capacity.plan(PI_27_1, TEAM, iterationNumber, 10));

        assertEquals(UNKNOWN_ITERATION_MESSAGE, exception.getMessage());
    }

    // --- AK 2: ganze Zahl ab 0 ---

    @Test
    void acceptsZeroPoints() {
        assertEquals(new Capacity(TEAM, 1, 0), Capacity.plan(PI_27_1, TEAM, 1, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
    void rejectsNegativePoints(int points) {
        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> Capacity.plan(PI_27_1, TEAM, 1, points));

        assertEquals(INVALID_POINTS_MESSAGE, exception.getMessage());
    }

    // Ein fehlender Wert (JSON ohne «points») ist keine ganze Zahl ab 0.
    @Test
    void rejectsMissingPoints() {
        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> Capacity.plan(PI_27_1, TEAM, 1, null));

        assertEquals(INVALID_POINTS_MESSAGE, exception.getMessage());
    }

    // Keine Obergrenze in der Spezifikation: auch ein sehr grosser Wert ist eine ganze Zahl ab 0.
    @Test
    void acceptsLargePoints() {
        assertEquals(new Capacity(TEAM, 1, Integer.MAX_VALUE), Capacity.plan(PI_27_1, TEAM, 1, Integer.MAX_VALUE));
    }
}
