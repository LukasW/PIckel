package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.Iteration;
import ch.grudligstrasse.pikel.domain.model.ProgramBoard;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.model.Team;
import ch.grudligstrasse.pikel.domain.port.in.FeatureUseCase;
import ch.grudligstrasse.pikel.domain.port.in.ProgramIncrementUseCase;
import ch.grudligstrasse.pikel.domain.port.in.TeamUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    // US-5: 2027-01-04 bis 2027-03-26 zu 2 Wochen hat 6 Iterationen, die letzte ist die IP-Iteration.
    private static final ProgramIncrement PI =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 2);

    private static final Team ADLER = new Team(UUID.randomUUID(), "Adler");
    private static final Team KOMET = new Team(UUID.randomUUID(), "Komet");
    private static final Team ZEBRA = new Team(UUID.randomUUID(), "Zebra");

    // Backlog-Reihenfolge nach #10: Rang aufsteigend, ohne Rang am Ende.
    private static final Feature PORTAL = Feature.create(PI.id(), "Portal", "", 1, "", 8);
    private static final Feature LOGIN = Feature.create(PI.id(), "Login", "", 2, "", 3);
    private static final Feature SUCHE = Feature.create(PI.id(), "Suche", "", null, "", null);

    @Mock
    ProgramIncrementUseCase programIncrements;

    @Mock
    TeamUseCase teams;

    @Mock
    FeatureUseCase features;

    @InjectMocks
    BoardService service;

    private void piExists() {
        when(programIncrements.get(PI.id())).thenReturn(PI);
    }

    // lenient: Ob der Service Teams und Backlog vor oder nach dem PI liest, ist seine Sache.
    private void teamsAre(Team... all) {
        lenient().when(teams.listAll()).thenReturn(List.of(all));
    }

    private void backlogIs(Feature... all) {
        lenient().when(features.listBacklog(PI.id())).thenReturn(List.of(all));
    }

    /** Ein Feature, das auf dem Board eingeplant ist; das Einplanen selbst kommt erst mit #13. */
    private static Feature scheduled(String title, int wsjfRank) {
        Feature feature = spy(Feature.create(PI.id(), title, "", wsjfRank, "", 5));
        doReturn(true).when(feature).scheduled();
        return feature;
    }

    // --- AK 1: Raster ---

    @Test
    void boardBelongsToRequestedProgramIncrement() {
        piExists();
        teamsAre(ADLER);
        backlogIs();

        assertSame(PI, service.get(PI.id()).programIncrement());
    }

    @Test
    void rowsAreTheTeamsInAlphabeticalOrder() {
        piExists();
        teamsAre(ADLER, KOMET, ZEBRA);
        backlogIs();

        assertEquals(List.of(ADLER, KOMET, ZEBRA), service.get(PI.id()).teams());
    }

    @Test
    void columnsAreTheIterationsInTimeOrderWithIpLast() {
        piExists();
        teamsAre(ADLER);
        backlogIs();

        List<Iteration> iterations = service.get(PI.id()).iterations();

        assertEquals(
                List.of("Iteration 1", "Iteration 2", "Iteration 3", "Iteration 4", "Iteration 5", "IP"),
                iterations.stream().map(Iteration::name).toList());
        assertEquals(LocalDate.of(2027, 1, 4), iterations.getFirst().startDate());
        assertEquals(LocalDate.of(2027, 3, 15), iterations.getLast().startDate());
        assertEquals(LocalDate.of(2027, 3, 26), iterations.getLast().endDate());
        assertTrue(iterations.getLast().innovationAndPlanning(), "die letzte Spalte ist die IP-Iteration");
        assertEquals(
                List.of(),
                iterations.subList(0, 5).stream()
                        .filter(Iteration::innovationAndPlanning)
                        .toList());
    }

    // AK 4 zeigt ohne Teams einen Hinweis; das Board selbst bleibt gültig, mit Spalten und Backlog.
    @Test
    void boardWithoutTeamsHasNoRowsButColumnsAndBacklog() {
        piExists();
        teamsAre();
        backlogIs(PORTAL);

        ProgramBoard board = service.get(PI.id());

        assertEquals(List.of(), board.teams());
        assertEquals(6, board.iterations().size());
        assertEquals(List.of(PORTAL), board.backlog());
    }

    // --- AK 3: Backlog-Spalte ---

    @Test
    void backlogKeepsBacklogOrder() {
        piExists();
        teamsAre(ADLER);
        backlogIs(PORTAL, LOGIN, SUCHE);

        assertEquals(List.of(PORTAL, LOGIN, SUCHE), service.get(PI.id()).backlog());
    }

    // Gegenstück zu backlogKeepsBacklogOrder: Nur «eingeplant» weicht ab, Rang und Reihenfolge bleiben gleich.
    @Test
    void backlogLeavesOutScheduledFeaturesAndKeepsOrderOfTheRest() {
        piExists();
        teamsAre(ADLER);
        Feature scheduledLogin = scheduled("Login", 2);
        backlogIs(PORTAL, scheduledLogin, SUCHE);

        assertEquals(List.of(PORTAL, SUCHE), service.get(PI.id()).backlog());
    }

    @Test
    void backlogIsEmptyWhenEveryFeatureIsScheduled() {
        piExists();
        teamsAre(ADLER);
        backlogIs(scheduled("Portal", 1), scheduled("Login", 2));

        assertEquals(List.of(), service.get(PI.id()).backlog());
    }

    // --- Unbekanntes PI → 404 ---

    @Test
    void getRejectsUnknownProgramIncrement() {
        UUID unknown = UUID.randomUUID();
        when(programIncrements.get(unknown)).thenThrow(new ProgramIncrementNotFoundException());
        teamsAre(ADLER);
        lenient().when(features.listBacklog(unknown)).thenThrow(new ProgramIncrementNotFoundException());

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.get(unknown));
    }
}
