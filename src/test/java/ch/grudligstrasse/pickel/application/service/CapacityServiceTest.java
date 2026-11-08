package ch.grudligstrasse.pickel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pickel.domain.model.Capacity;
import ch.grudligstrasse.pickel.domain.model.InvalidCapacityException;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pickel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pickel.domain.model.Team;
import ch.grudligstrasse.pickel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pickel.domain.port.in.TeamUseCase;
import ch.grudligstrasse.pickel.domain.port.out.CapacityRepository;
import ch.grudligstrasse.pickel.domain.port.out.ProgramIncrementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CapacityServiceTest {

    private static final String INVALID_POINTS_MESSAGE = "Die Kapazität muss eine ganze Zahl ab 0 sein.";
    private static final String IP_MESSAGE = "In der IP-Iteration wird keine Kapazität geplant.";

    // US-5: 2027-01-04 bis 2027-02-07 zu 2 Wochen hat 3 Iterationen; 1 und 2 sind Entwicklungs-Iterationen, 3 ist IP.
    private static final ProgramIncrement PI =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 2, 7), 2);
    private static final int IP = 3;

    private static final Team RAKETE = new Team(UUID.randomUUID(), "Rakete");
    private static final Team KOMET = new Team(UUID.randomUUID(), "Komet");

    @Mock
    CapacityRepository capacities;

    @Mock
    ProgramIncrementRepository programIncrements;

    @Mock
    TeamUseCase teams;

    @InjectMocks
    CapacityService service;

    private void piExists(ProgramIncrement programIncrement) {
        when(programIncrements.findOne(programIncrement.id())).thenReturn(Optional.of(programIncrement));
    }

    // lenient: Ob der Service die Teams vor oder nach der Prüfung von PI und Punkten liest, ist seine Sache.
    private void teamsAre(Team... all) {
        lenient().when(teams.listAll()).thenReturn(List.of(all));
    }

    // --- AK 1 und 2: Erfassen ---

    @Test
    void setSavesAndReturnsCapacityOfTeamInIteration() {
        piExists(PI);
        teamsAre(RAKETE, KOMET);

        Capacity saved = service.set(PI.id(), RAKETE.id(), 1, 40);

        assertEquals(new Capacity(RAKETE.id(), 1, 40), saved);
        verify(capacities).save(PI.id(), new Capacity(RAKETE.id(), 1, 40));
    }

    // Zurück auf 0 ist ein gültiger Eintrag, kein Löschen und kein Fehler.
    @Test
    void setSavesZeroPoints() {
        piExists(PI);
        teamsAre(RAKETE);

        Capacity saved = service.set(PI.id(), RAKETE.id(), 2, 0);

        assertEquals(new Capacity(RAKETE.id(), 2, 0), saved);
        verify(capacities).save(PI.id(), new Capacity(RAKETE.id(), 2, 0));
    }

    @Test
    void setRejectsNegativePointsWithoutSaving() {
        piExists(PI);
        teamsAre(RAKETE);

        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> service.set(PI.id(), RAKETE.id(), 1, -5));

        assertEquals(INVALID_POINTS_MESSAGE, exception.getMessage());
        verify(capacities, never()).save(any(), any());
    }

    @Test
    void setRejectsMissingPointsWithoutSaving() {
        piExists(PI);
        teamsAre(RAKETE);

        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> service.set(PI.id(), RAKETE.id(), 1, null));

        assertEquals(INVALID_POINTS_MESSAGE, exception.getMessage());
        verify(capacities, never()).save(any(), any());
    }

    @Test
    void setRejectsIpIterationWithoutSaving() {
        piExists(PI);
        teamsAre(RAKETE);

        InvalidCapacityException exception =
                assertThrows(InvalidCapacityException.class, () -> service.set(PI.id(), RAKETE.id(), IP, 10));

        assertEquals(IP_MESSAGE, exception.getMessage());
        verify(capacities, never()).save(any(), any());
    }

    @Test
    void setRejectsUnknownProgramIncrement() {
        UUID unknown = UUID.randomUUID();
        when(programIncrements.findOne(unknown)).thenReturn(Optional.empty());
        teamsAre(RAKETE);

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.set(unknown, RAKETE.id(), 1, 40));
        verify(capacities, never()).save(any(), any());
    }

    // Gegenstück zum gültigen Fall: Nur das Team fehlt in der Teamliste, PI, Iteration und Punkte sind gültig.
    @Test
    void setRejectsTeamNotInTeamList() {
        piExists(PI);
        teamsAre(KOMET);

        assertThrows(TeamNotFoundException.class, () -> service.set(PI.id(), RAKETE.id(), 1, 40));
        verify(capacities, never()).save(any(), any());
    }

    // --- AK 3: ohne Eintrag 0; Matrix Teams × Entwicklungs-Iterationen ---

    // Teamliste bewusst nicht alphabetisch: Massgebend ist die Reihenfolge von TeamUseCase.listAll.
    @Test
    void listReturnsEveryTeamInEveryDevelopmentIterationWithZeroWithoutEntry() {
        piExists(PI);
        teamsAre(RAKETE, KOMET);
        when(capacities.findByProgramIncrement(PI.id())).thenReturn(List.of(new Capacity(KOMET.id(), 2, 13)));

        assertEquals(
                List.of(
                        new Capacity(RAKETE.id(), 1, 0),
                        new Capacity(KOMET.id(), 1, 0),
                        new Capacity(RAKETE.id(), 2, 0),
                        new Capacity(KOMET.id(), 2, 13)),
                service.list(PI.id()));
    }

    // Die Reihenfolge der gespeicherten Einträge ändert die Reihenfolge der Matrix nicht.
    @Test
    void listOrdersByIterationThenTeamListRegardlessOfStoredOrder() {
        piExists(PI);
        teamsAre(KOMET, RAKETE);
        when(capacities.findByProgramIncrement(PI.id()))
                .thenReturn(List.of(
                        new Capacity(RAKETE.id(), 2, 20),
                        new Capacity(KOMET.id(), 1, 11),
                        new Capacity(RAKETE.id(), 1, 21),
                        new Capacity(KOMET.id(), 2, 12)));

        assertEquals(
                List.of(
                        new Capacity(KOMET.id(), 1, 11),
                        new Capacity(RAKETE.id(), 1, 21),
                        new Capacity(KOMET.id(), 2, 12),
                        new Capacity(RAKETE.id(), 2, 20)),
                service.list(PI.id()));
    }

    @Test
    void listWithoutTeamsIsEmpty() {
        piExists(PI);
        teamsAre();
        lenient().when(capacities.findByProgramIncrement(PI.id())).thenReturn(List.of());

        assertEquals(List.of(), service.list(PI.id()));
    }

    @Test
    void listRejectsUnknownProgramIncrement() {
        UUID unknown = UUID.randomUUID();
        when(programIncrements.findOne(unknown)).thenReturn(Optional.empty());
        teamsAre(RAKETE);
        lenient().when(capacities.findByProgramIncrement(unknown)).thenReturn(List.of());

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.list(unknown));
    }

    // --- AK 4: pro PI getrennt ---

    @Test
    void setSavesUnderGivenProgramIncrementOnly() {
        ProgramIncrement other = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.2", LocalDate.of(2027, 4, 5), LocalDate.of(2027, 5, 9), 2);
        piExists(PI);
        lenient().when(programIncrements.findOne(other.id())).thenReturn(Optional.of(other));
        teamsAre(RAKETE);

        service.set(PI.id(), RAKETE.id(), 1, 40);

        verify(capacities).save(PI.id(), new Capacity(RAKETE.id(), 1, 40));
        verify(capacities, never()).save(eq(other.id()), any());
    }

    @Test
    void listShowsOnlyEntriesOfRequestedProgramIncrement() {
        ProgramIncrement other = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.2", LocalDate.of(2027, 4, 5), LocalDate.of(2027, 5, 9), 2);
        piExists(other);
        teamsAre(RAKETE);
        lenient()
                .when(capacities.findByProgramIncrement(PI.id()))
                .thenReturn(List.of(new Capacity(RAKETE.id(), 1, 40)));
        when(capacities.findByProgramIncrement(other.id())).thenReturn(List.of());

        assertEquals(
                List.of(new Capacity(RAKETE.id(), 1, 0), new Capacity(RAKETE.id(), 2, 0)), service.list(other.id()));
    }
}
