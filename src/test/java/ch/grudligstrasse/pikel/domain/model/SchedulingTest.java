package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchedulingTest {

    private static final UUID TEAM = UUID.fromString("6f1c2b7e-0d4a-4c1e-9b7a-2f3e4d5c6b7a");

    @Test
    void ofKeepsTeamAndIterationNumber() {
        assertEquals(new Scheduling(TEAM, 2), Scheduling.of(TEAM, 2));
    }

    // Fehlende Angaben im Body: 400 mit dieser Meldung (Plan: «400 bei fehlendem Feld»).
    @Test
    void ofRejectsMissingTeam() {
        InvalidFeatureException exception = assertThrows(InvalidFeatureException.class, () -> Scheduling.of(null, 2));

        assertEquals("Team und Iteration sind Pflicht.", exception.getMessage());
    }

    @Test
    void ofRejectsMissingIterationNumber() {
        InvalidFeatureException exception =
                assertThrows(InvalidFeatureException.class, () -> Scheduling.of(TEAM, null));

        assertEquals("Team und Iteration sind Pflicht.", exception.getMessage());
    }
}
