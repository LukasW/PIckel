package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.InvalidMilestoneException;
import ch.grudligstrasse.pikel.domain.model.Milestone;
import ch.grudligstrasse.pikel.domain.model.MilestoneNotFoundException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.out.MilestoneRepository;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    private static final ProgramIncrement PI_27_1 =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 2);
    private static final ProgramIncrement PI_27_2 =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.2", LocalDate.of(2027, 3, 29), LocalDate.of(2027, 6, 18), 2);
    private static final LocalDate RELEASE_DATE = LocalDate.of(2027, 2, 10);
    private static final LocalDate MOVED_DATE = LocalDate.of(2027, 1, 5);
    private static final String PI_NOT_FOUND = "Das PI gibt es nicht.";
    private static final String MILESTONE_NOT_FOUND = "Den Milestone gibt es nicht.";
    private static final String DATE_OUTSIDE = "Das Datum muss im Zeitraum des PI liegen.";

    @Mock
    MilestoneRepository milestones;

    @Mock
    ProgramIncrementRepository programIncrements;

    @InjectMocks
    MilestoneService service;

    private static Milestone milestone(String name, LocalDate date) {
        return new Milestone(UUID.randomUUID(), PI_27_1.id(), name, date);
    }

    private void programIncrementExists(ProgramIncrement pi) {
        when(programIncrements.findOne(pi.id())).thenReturn(Optional.of(pi));
    }

    private UUID unknownProgramIncrement() {
        UUID unknown = UUID.randomUUID();
        when(programIncrements.findOne(unknown)).thenReturn(Optional.empty());
        return unknown;
    }

    private Milestone storedRelease() {
        Milestone release = milestone("Release 4.0", RELEASE_DATE);
        lenient().when(milestones.findOne(PI_27_1.id(), release.id())).thenReturn(Optional.of(release));
        return release;
    }

    private Milestone savedMilestone() {
        ArgumentCaptor<Milestone> saved = ArgumentCaptor.forClass(Milestone.class);
        verify(milestones).save(saved.capture());
        return saved.getValue();
    }

    // --- Liste (Reihenfolge liefert das Repository laut Port) ---

    @Test
    void listReturnsMilestonesOfProgramIncrementInRepositoryOrder() {
        programIncrementExists(PI_27_1);
        Milestone kickOff = milestone("Kick-off", LocalDate.of(2027, 1, 4));
        Milestone release = milestone("Release 4.0", RELEASE_DATE);
        when(milestones.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of(kickOff, release));

        assertEquals(List.of(kickOff, release), service.list(PI_27_1.id()));
    }

    @Test
    void listRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        lenient().when(milestones.findByProgramIncrement(unknown)).thenReturn(List.of());

        ProgramIncrementNotFoundException exception =
                assertThrows(ProgramIncrementNotFoundException.class, () -> service.list(unknown));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
    }

    // --- AK 1: erfassen ---

    @Test
    void addSavesMilestoneWithTrimmedNameInProgramIncrementAndReturnsIt() {
        programIncrementExists(PI_27_1);

        Milestone added = service.add(PI_27_1.id(), "  Release 4.0 ", RELEASE_DATE);

        Milestone saved = savedMilestone();
        assertEquals(saved, added);
        assertEquals(PI_27_1.id(), saved.programIncrementId());
        assertEquals("Release 4.0", saved.name());
        assertEquals(RELEASE_DATE, saved.date());
    }

    // --- AK 2: ausserhalb des PI ---

    @Test
    void addRejectsDateOutsideProgramIncrementAndSavesNothing() {
        programIncrementExists(PI_27_1);
        LocalDate april = LocalDate.of(2027, 4, 1);

        InvalidMilestoneException exception =
                assertThrows(InvalidMilestoneException.class, () -> service.add(PI_27_1.id(), "Release 4.0", april));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
        verify(milestones, never()).save(any());
    }

    // Gleiches Datum, anderes PI: Der Service prüft gegen das PI aus der Anfrage.
    @Test
    void addChecksDateAgainstTheRequestedProgramIncrement() {
        programIncrementExists(PI_27_2);

        InvalidMilestoneException exception = assertThrows(
                InvalidMilestoneException.class, () -> service.add(PI_27_2.id(), "Release 4.0", RELEASE_DATE));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
        verify(milestones, never()).save(any());
    }

    @Test
    void addRejectsBlankNameAndSavesNothing() {
        programIncrementExists(PI_27_1);

        assertThrows(InvalidMilestoneException.class, () -> service.add(PI_27_1.id(), " ", RELEASE_DATE));

        verify(milestones, never()).save(any());
    }

    @Test
    void addRejectsUnknownProgramIncrementAndSavesNothing() {
        UUID unknown = unknownProgramIncrement();

        ProgramIncrementNotFoundException exception = assertThrows(
                ProgramIncrementNotFoundException.class, () -> service.add(unknown, "Release 4.0", RELEASE_DATE));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(milestones, never()).save(any());
    }

    // --- AK 3: ändern ---

    @Test
    void updateSavesNewNameAndDateUnderSameIdAndReturnsIt() {
        programIncrementExists(PI_27_1);
        Milestone release = storedRelease();

        Milestone updated = service.update(PI_27_1.id(), release.id(), " Release 4.1 ", MOVED_DATE);

        Milestone saved = savedMilestone();
        assertEquals(saved, updated);
        assertEquals(release.id(), saved.id());
        assertEquals(PI_27_1.id(), saved.programIncrementId());
        assertEquals("Release 4.1", saved.name());
        assertEquals(MOVED_DATE, saved.date());
    }

    @Test
    void updateRejectsDateOutsideProgramIncrementAndSavesNothing() {
        programIncrementExists(PI_27_1);
        Milestone release = storedRelease();
        LocalDate dayAfterEnd = LocalDate.of(2027, 3, 27);

        InvalidMilestoneException exception = assertThrows(
                InvalidMilestoneException.class,
                () -> service.update(PI_27_1.id(), release.id(), "Release 4.0", dayAfterEnd));

        assertEquals(DATE_OUTSIDE, exception.getMessage());
        verify(milestones, never()).save(any());
    }

    @Test
    void updateRejectsUnknownMilestoneAndSavesNothing() {
        programIncrementExists(PI_27_1);
        UUID unknown = UUID.randomUUID();
        when(milestones.findOne(PI_27_1.id(), unknown)).thenReturn(Optional.empty());

        MilestoneNotFoundException exception = assertThrows(
                MilestoneNotFoundException.class,
                () -> service.update(PI_27_1.id(), unknown, "Release 4.0", RELEASE_DATE));

        assertEquals(MILESTONE_NOT_FOUND, exception.getMessage());
        verify(milestones, never()).save(any());
    }

    // Der Milestone gehört zu PI 27.1; über PI 27.2 angesprochen gibt es ihn nicht.
    @Test
    void updateRejectsMilestoneOfAnotherProgramIncrement() {
        programIncrementExists(PI_27_2);
        Milestone release = storedRelease();
        when(milestones.findOne(PI_27_2.id(), release.id())).thenReturn(Optional.empty());
        LocalDate inPi272 = LocalDate.of(2027, 4, 1);

        MilestoneNotFoundException exception = assertThrows(
                MilestoneNotFoundException.class,
                () -> service.update(PI_27_2.id(), release.id(), "Release 4.0", inPi272));

        assertEquals(MILESTONE_NOT_FOUND, exception.getMessage());
        verify(milestones, never()).save(any());
    }

    @Test
    void updateRejectsUnknownProgramIncrementAndSavesNothing() {
        UUID unknown = unknownProgramIncrement();
        UUID milestoneId = UUID.randomUUID();
        lenient().when(milestones.findOne(unknown, milestoneId)).thenReturn(Optional.empty());

        ProgramIncrementNotFoundException exception = assertThrows(
                ProgramIncrementNotFoundException.class,
                () -> service.update(unknown, milestoneId, "Release 4.0", RELEASE_DATE));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(milestones, never()).save(any());
    }

    // --- AK 3: löschen ---

    @Test
    void deleteRemovesMilestone() {
        programIncrementExists(PI_27_1);
        Milestone release = storedRelease();

        service.delete(PI_27_1.id(), release.id());

        verify(milestones).delete(release.id());
    }

    @Test
    void deleteRejectsUnknownMilestoneAndDeletesNothing() {
        programIncrementExists(PI_27_1);
        UUID unknown = UUID.randomUUID();
        when(milestones.findOne(PI_27_1.id(), unknown)).thenReturn(Optional.empty());

        MilestoneNotFoundException exception =
                assertThrows(MilestoneNotFoundException.class, () -> service.delete(PI_27_1.id(), unknown));

        assertEquals(MILESTONE_NOT_FOUND, exception.getMessage());
        verify(milestones, never()).delete(any());
    }

    @Test
    void deleteRejectsMilestoneOfAnotherProgramIncrement() {
        programIncrementExists(PI_27_2);
        Milestone release = storedRelease();
        when(milestones.findOne(PI_27_2.id(), release.id())).thenReturn(Optional.empty());

        assertThrows(MilestoneNotFoundException.class, () -> service.delete(PI_27_2.id(), release.id()));

        verify(milestones, never()).delete(any());
    }

    @Test
    void deleteRejectsUnknownProgramIncrementAndDeletesNothing() {
        UUID unknown = unknownProgramIncrement();
        UUID milestoneId = UUID.randomUUID();
        lenient().when(milestones.findOne(unknown, milestoneId)).thenReturn(Optional.empty());

        ProgramIncrementNotFoundException exception =
                assertThrows(ProgramIncrementNotFoundException.class, () -> service.delete(unknown, milestoneId));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(milestones, never()).delete(any());
    }
}
