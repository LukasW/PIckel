package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.FeatureDependency;
import ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException;
import ch.grudligstrasse.pikel.domain.model.InvalidFeatureException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.port.out.FeatureDependencyRepository;
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

/** US-16: Abhängigkeiten zwischen Features über den Use Case (Delta, Entscheide und Szenarien). */
@ExtendWith(MockitoExtension.class)
class FeatureDependencyServiceTest {

    private static final ProgramIncrement PI_27_1 =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 2);
    private static final ProgramIncrement PI_27_2 =
            new ProgramIncrement(UUID.randomUUID(), "PI 27.2", LocalDate.of(2027, 4, 5), LocalDate.of(2027, 6, 25), 2);

    private static final String PI_NOT_FOUND = "Das PI gibt es nicht.";
    private static final String FEATURE_NOT_FOUND = "Das Feature gibt es nicht.";
    private static final String SELF = "Ein Feature kann nicht von sich selbst abhängen.";
    private static final String OTHER_PI = "Beide Features müssen zum selben PI gehören.";
    private static final String DUPLICATE = "Diese Abhängigkeit gibt es schon.";
    private static final String REVERSE = "Die beiden Features hängen schon umgekehrt voneinander ab.";

    @Mock
    FeatureDependencyRepository dependencies;

    @Mock
    FeatureRepository features;

    @Mock
    ProgramIncrementRepository programIncrements;

    @InjectMocks
    FeatureDependencyService service;

    private static Feature feature(ProgramIncrement programIncrement, String title) {
        return new Feature(
                UUID.randomUUID(), programIncrement.id(), title, "", "", OptionalInt.empty(), OptionalInt.empty());
    }

    // lenient: Eine Umsetzung, die vorher ablehnt, fragt nicht mehr nach jedem Feature.
    private Feature stored(Feature feature) {
        lenient().when(features.findOne(feature.id())).thenReturn(Optional.of(feature));
        return feature;
    }

    private UUID unknownFeature() {
        UUID unknown = UUID.randomUUID();
        lenient().when(features.findOne(unknown)).thenReturn(Optional.empty());
        return unknown;
    }

    private void programIncrementExists(ProgramIncrement programIncrement) {
        when(programIncrements.findOne(programIncrement.id())).thenReturn(Optional.of(programIncrement));
    }

    private UUID unknownProgramIncrement() {
        UUID unknown = UUID.randomUUID();
        when(programIncrements.findOne(unknown)).thenReturn(Optional.empty());
        return unknown;
    }

    private void existingDependencies(FeatureDependency... existing) {
        lenient().when(dependencies.findByProgramIncrement(PI_27_1.id())).thenReturn(List.of(existing));
    }

    private static FeatureDependency needs(Feature needing, Feature needed) {
        return new FeatureDependency(needing.id(), needed.id());
    }

    // --- AK 3: Liste des PI (Grundlage für «braucht» und «wird gebraucht von») ---

    @Test
    void listReturnsDependenciesOfProgramIncrement() {
        programIncrementExists(PI_27_1);
        Feature portal = feature(PI_27_1, "Portal");
        Feature login = feature(PI_27_1, "Login");
        Feature search = feature(PI_27_1, "Suche");
        List<FeatureDependency> stored = List.of(needs(portal, login), needs(search, portal));
        when(dependencies.findByProgramIncrement(PI_27_1.id())).thenReturn(stored);

        assertEquals(stored, service.list(PI_27_1.id()));
    }

    @Test
    void listRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        lenient().when(dependencies.findByProgramIncrement(unknown)).thenReturn(List.of());

        ProgramIncrementNotFoundException exception =
                assertThrows(ProgramIncrementNotFoundException.class, () -> service.list(unknown));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
    }

    // --- AK 1: Erfassen (Szenario «Abhängigkeit wird erfasst») ---

    @Test
    void addSavesAndReturnsDependencyInRequestedDirection() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_1, "Portal"));
        Feature login = stored(feature(PI_27_1, "Login"));
        existingDependencies();

        FeatureDependency created = service.add(PI_27_1.id(), portal.id(), login.id());

        assertEquals(new FeatureDependency(portal.id(), login.id()), created);
        verify(dependencies).save(new FeatureDependency(portal.id(), login.id()));
    }

    // Bestehende Abhängigkeiten mit nur einem der beiden Features verhindern nichts.
    @Test
    void addSavesDependencyBesideOthersOfSameFeatures() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_1, "Portal"));
        Feature login = stored(feature(PI_27_1, "Login"));
        Feature search = feature(PI_27_1, "Suche");
        existingDependencies(needs(portal, search), needs(search, login));

        FeatureDependency created = service.add(PI_27_1.id(), portal.id(), login.id());

        assertEquals(needs(portal, login), created);
        verify(dependencies).save(needs(portal, login));
    }

    // --- AK 2: Ablehnungen, gespeichert wird nichts ---

    @Test
    void addRejectsFeatureNeedingItself() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_1, "Portal"));
        existingDependencies();

        InvalidFeatureException exception =
                assertThrows(InvalidFeatureException.class, () -> service.add(PI_27_1.id(), portal.id(), portal.id()));

        assertEquals(SELF, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    // Die Prüfgrundlage sind die gespeicherten Abhängigkeiten des PI.
    @Test
    void addRejectsDuplicateOfStoredDependency() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_1, "Portal"));
        Feature login = stored(feature(PI_27_1, "Login"));
        existingDependencies(needs(portal, login));

        InvalidFeatureException exception =
                assertThrows(InvalidFeatureException.class, () -> service.add(PI_27_1.id(), portal.id(), login.id()));

        assertEquals(DUPLICATE, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    @Test
    void addRejectsReverseOfStoredDependency() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_1, "Portal"));
        Feature login = stored(feature(PI_27_1, "Login"));
        existingDependencies(needs(portal, login));

        InvalidFeatureException exception =
                assertThrows(InvalidFeatureException.class, () -> service.add(PI_27_1.id(), login.id(), portal.id()));

        assertEquals(REVERSE, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    // Szenario «… aus einem anderen PI wird abgelehnt»: Das gebrauchte Feature gibt es, nur im anderen PI → 400.
    @Test
    void addRejectsNeededFeatureOfOtherProgramIncrement() {
        programIncrementExists(PI_27_1);
        lenient().when(programIncrements.findOne(PI_27_2.id())).thenReturn(Optional.of(PI_27_2));
        Feature portal = stored(feature(PI_27_1, "Portal"));
        Feature login = stored(feature(PI_27_2, "Login"));
        existingDependencies();
        lenient().when(dependencies.findByProgramIncrement(PI_27_2.id())).thenReturn(List.of());

        InvalidFeatureException exception =
                assertThrows(InvalidFeatureException.class, () -> service.add(PI_27_1.id(), portal.id(), login.id()));

        assertEquals(OTHER_PI, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    // --- Unbekanntes PI und unbekannte Features (Gerüst, FeatureDependencyUseCase) ---

    // Beide Features gehören zum unbekannten PI: Nur die Prüfung des PI kann ablehnen.
    @Test
    void addRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        ProgramIncrement ghost =
                new ProgramIncrement(unknown, "PI 99.1", LocalDate.of(2099, 1, 5), LocalDate.of(2099, 3, 27), 2);
        Feature portal = stored(feature(ghost, "Portal"));
        Feature login = stored(feature(ghost, "Login"));
        lenient().when(dependencies.findByProgramIncrement(unknown)).thenReturn(List.of());

        ProgramIncrementNotFoundException exception = assertThrows(
                ProgramIncrementNotFoundException.class, () -> service.add(unknown, portal.id(), login.id()));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    @Test
    void addRejectsUnknownNeedingFeature() {
        programIncrementExists(PI_27_1);
        UUID unknown = unknownFeature();
        Feature login = stored(feature(PI_27_1, "Login"));
        existingDependencies();

        FeatureNotFoundException exception =
                assertThrows(FeatureNotFoundException.class, () -> service.add(PI_27_1.id(), unknown, login.id()));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    // Das brauchende Feature gehört nicht zum PI der URL: Es gibt es dort nicht (404), auch wenn das gebrauchte passt.
    @Test
    void addRejectsNeedingFeatureOfOtherProgramIncrementAsNotFound() {
        programIncrementExists(PI_27_1);
        lenient().when(programIncrements.findOne(PI_27_2.id())).thenReturn(Optional.of(PI_27_2));
        Feature portal = stored(feature(PI_27_2, "Portal"));
        Feature login = stored(feature(PI_27_1, "Login"));
        existingDependencies();
        lenient().when(dependencies.findByProgramIncrement(PI_27_2.id())).thenReturn(List.of());

        FeatureNotFoundException exception =
                assertThrows(FeatureNotFoundException.class, () -> service.add(PI_27_1.id(), portal.id(), login.id()));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    @Test
    void addRejectsUnknownNeededFeature() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_1, "Portal"));
        UUID unknown = unknownFeature();
        existingDependencies();

        FeatureNotFoundException exception =
                assertThrows(FeatureNotFoundException.class, () -> service.add(PI_27_1.id(), portal.id(), unknown));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(dependencies, never()).save(any());
    }

    // --- AK 4: Entfernen (Szenario «Abhängigkeit wird entfernt») ---

    @Test
    void removeDeletesDependency() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_1, "Portal"));
        Feature login = stored(feature(PI_27_1, "Login"));
        existingDependencies(needs(portal, login));

        service.remove(PI_27_1.id(), portal.id(), login.id());

        verify(dependencies).remove(portal.id(), login.id());
    }

    // Idempotent: Auch eine Abhängigkeit zwischen unbekannten Features «entfernt» der Use Case ohne Fehler.
    @Test
    void removeOfUnknownDependencyIsNoError() {
        programIncrementExists(PI_27_1);
        UUID unknownNeeding = unknownFeature();
        UUID unknownNeeded = unknownFeature();
        existingDependencies();

        service.remove(PI_27_1.id(), unknownNeeding, unknownNeeded);

        verify(dependencies).remove(unknownNeeding, unknownNeeded);
    }

    // Über die Adresse eines anderen PI lässt sich keine Abhängigkeit entfernen, wie beim Erfassen.
    @Test
    void removeRejectsNeedingFeatureOfOtherProgramIncrementAsNotFound() {
        programIncrementExists(PI_27_1);
        Feature portal = stored(feature(PI_27_2, "Portal"));
        Feature login = stored(feature(PI_27_2, "Login"));

        FeatureNotFoundException exception = assertThrows(
                FeatureNotFoundException.class, () -> service.remove(PI_27_1.id(), portal.id(), login.id()));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(dependencies, never()).remove(any(), any());
    }

    @Test
    void removeRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        UUID needing = UUID.randomUUID();
        UUID needed = UUID.randomUUID();

        ProgramIncrementNotFoundException exception =
                assertThrows(ProgramIncrementNotFoundException.class, () -> service.remove(unknown, needing, needed));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(dependencies, never()).remove(any(), any());
    }
}
