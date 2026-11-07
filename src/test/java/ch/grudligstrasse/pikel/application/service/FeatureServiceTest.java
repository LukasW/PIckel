package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.InvalidFeatureException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.out.FeatureRepository;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    private static final ProgramIncrement PI_27_1 =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 2);
    private static final String BLANK_TITLE = "Der Titel darf nicht leer sein.";
    private static final String PI_NOT_FOUND = "Das PI gibt es nicht.";

    @Mock
    FeatureRepository features;

    @Mock
    ProgramIncrementRepository programIncrements;

    @InjectMocks
    FeatureService service;

    private static Feature feature(String title, Integer rank) {
        return new Feature(
                UUID.randomUUID(),
                PI_27_1.id(),
                title,
                "",
                "",
                rank == null ? OptionalInt.empty() : OptionalInt.of(rank),
                OptionalInt.empty());
    }

    private void programIncrementExists() {
        when(programIncrements.findOne(PI_27_1.id())).thenReturn(Optional.of(PI_27_1));
    }

    private UUID unknownProgramIncrement() {
        UUID unknown = UUID.randomUUID();
        when(programIncrements.findOne(unknown)).thenReturn(Optional.empty());
        return unknown;
    }

    // --- Backlog (Szenario «erscheint im Backlog»; Reihenfolge: Annahme notes.md) ---

    @Test
    void listBacklogReturnsFeaturesOfProgramIncrement() {
        programIncrementExists();
        Feature login = feature("Login", null);
        when(features.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of(login));

        assertEquals(List.of(login), service.listBacklog(PI_27_1.id()));
    }

    @Test
    void listBacklogWithoutFeaturesIsEmpty() {
        programIncrementExists();
        when(features.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of());

        assertEquals(List.of(), service.listBacklog(PI_27_1.id()));
    }

    // Rang vor Titel: «Zeta» mit Rang 1 steht vor «Alpha» mit Rang 2; 10 nach 2 (numerisch, nicht als Text).
    @Test
    void listBacklogSortsByRankAscending() {
        programIncrementExists();
        Feature alpha2 = feature("Alpha", 2);
        Feature zeta1 = feature("Zeta", 1);
        Feature beta10 = feature("Beta", 10);
        when(features.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of(beta10, alpha2, zeta1));

        assertEquals(List.of(zeta1, alpha2, beta10), service.listBacklog(PI_27_1.id()));
    }

    // Ohne Rang ans Ende, auch wenn der Titel alphabetisch vorne stünde.
    @Test
    void listBacklogPutsFeaturesWithoutRankLast() {
        programIncrementExists();
        Feature aaaWithoutRank = feature("Aaa", null);
        Feature zeta5 = feature("Zeta", 5);
        when(features.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of(aaaWithoutRank, zeta5));

        assertEquals(List.of(zeta5, aaaWithoutRank), service.listBacklog(PI_27_1.id()));
    }

    // Natürliche String-Ordnung ergäbe «Alpha, Gamma, beta»; verlangt ist ohne Gross/Klein.
    @Test
    void listBacklogSortsSameRankByTitleIgnoringCase() {
        programIncrementExists();
        Feature gamma = feature("Gamma", 3);
        Feature beta = feature("beta", 3);
        Feature alpha = feature("Alpha", 3);
        when(features.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of(gamma, beta, alpha));

        assertEquals(List.of(alpha, beta, gamma), service.listBacklog(PI_27_1.id()));
    }

    @Test
    void listBacklogSortsFeaturesWithoutRankByTitleIgnoringCase() {
        programIncrementExists();
        Feature gamma = feature("Gamma", null);
        Feature beta = feature("beta", null);
        Feature alpha = feature("Alpha", null);
        when(features.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of(gamma, beta, alpha));

        assertEquals(List.of(alpha, beta, gamma), service.listBacklog(PI_27_1.id()));
    }

    @Test
    void listBacklogRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();

        ProgramIncrementNotFoundException exception =
                assertThrows(ProgramIncrementNotFoundException.class, () -> service.listBacklog(unknown));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
    }

    // --- Erfassen (AK 1–3, Szenarien «Feature wird erfasst», «nur mit Titel») ---

    @Test
    void addSavesAndReturnsFeatureWithAllValues() {
        programIncrementExists();

        Feature created = service.add(PI_27_1.id(), " Self-Service-Portal ", "Adresse ändern", 1, "Anna", 21);

        assertEquals(PI_27_1.id(), created.programIncrementId());
        assertEquals("Self-Service-Portal", created.title());
        assertEquals("Adresse ändern", created.description());
        assertEquals(OptionalInt.of(1), created.wsjfRank());
        assertEquals("Anna", created.owner());
        assertEquals(OptionalInt.of(21), created.estimate());
        assertFalse(created.scheduled());
        verify(features).save(created);
    }

    @Test
    void addWithTitleOnlySavesFeatureWithoutOptionalValues() {
        programIncrementExists();

        Feature created = service.add(PI_27_1.id(), "Login", null, null, null, null);

        assertEquals("Login", created.title());
        assertEquals("", created.description());
        assertEquals("", created.owner());
        assertEquals(OptionalInt.empty(), created.wsjfRank());
        assertEquals(OptionalInt.empty(), created.estimate());
        verify(features).save(created);
    }

    // AK 4 (Backend-Teil): ungültige Angaben legen nichts an.
    @Test
    void addDoesNotSaveFeatureWithBlankTitle() {
        programIncrementExists();

        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class, () -> service.add(PI_27_1.id(), " ", null, null, null, null));

        assertEquals(BLANK_TITLE, exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void addDoesNotSaveFeatureWithRankZero() {
        programIncrementExists();

        assertThrows(InvalidFeatureException.class, () -> service.add(PI_27_1.id(), "Login", null, 0, null, null));
        verify(features, never()).save(any());
    }

    @Test
    void addDoesNotSaveFeatureWithNegativeEstimate() {
        programIncrementExists();

        assertThrows(InvalidFeatureException.class, () -> service.add(PI_27_1.id(), "Login", null, null, null, -1));
        verify(features, never()).save(any());
    }

    // Szenario «Feature in einem unbekannten PI wird abgelehnt»
    @Test
    void addRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();

        ProgramIncrementNotFoundException exception = assertThrows(
                ProgramIncrementNotFoundException.class, () -> service.add(unknown, "Login", null, null, null, null));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Auftrag/Gerüst (analog Team, notes.md US-6): erst das PI, dann die Angaben; unbekanntes PI ist 404, nicht 400.
    @Test
    void addToUnknownProgramIncrementIsNotFoundEvenWithBlankTitle() {
        UUID unknown = unknownProgramIncrement();

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.add(unknown, " ", null, null, null, null));
        verify(features, never()).save(any());
    }
}
