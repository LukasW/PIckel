package ch.grudligstrasse.pikel.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.grudligstrasse.pikel.domain.model.Feature;
import ch.grudligstrasse.pikel.domain.model.FeatureNotFoundException;
import ch.grudligstrasse.pikel.domain.model.InvalidFeatureException;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrement;
import ch.grudligstrasse.pikel.domain.model.ProgramIncrementNotFoundException;
import ch.grudligstrasse.pikel.domain.model.Scheduling;
import ch.grudligstrasse.pikel.domain.model.Team;
import ch.grudligstrasse.pikel.domain.model.TeamNotFoundException;
import ch.grudligstrasse.pikel.domain.port.out.FeatureRepository;
import ch.grudligstrasse.pikel.domain.port.out.ProgramIncrementRepository;
import ch.grudligstrasse.pikel.domain.port.out.TeamRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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

    // US-13: Einplanen prüft das Team.
    @Mock
    TeamRepository teams;

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

    // --- US-10: Bearbeiten (Szenarien «Feature wird bearbeitet», «Bearbeiten mit leerem Titel wird abgelehnt») ---

    private static final String FEATURE_NOT_FOUND = "Das Feature gibt es nicht.";

    // Ein Feature, das in den Features gespeichert ist; lenient, weil eine Umsetzung, die zuerst das PI prüft,
    // nicht mehr nach dem Feature fragt.
    private Feature stored(Feature feature) {
        lenient().when(features.findOne(feature.id())).thenReturn(Optional.of(feature));
        return feature;
    }

    private static Feature inOtherProgramIncrement(String title) {
        return new Feature(
                UUID.randomUUID(), UUID.randomUUID(), title, "", "", OptionalInt.empty(), OptionalInt.empty());
    }

    @Test
    void updateSavesAndReturnsRevisedFeature() {
        programIncrementExists();
        Feature login = stored(feature("Login", null));

        Feature updated = service.update(PI_27_1.id(), login.id(), "Login", null, 3, null, 8);

        assertEquals(login.id(), updated.id());
        assertEquals(PI_27_1.id(), updated.programIncrementId());
        assertEquals("Login", updated.title());
        assertEquals(OptionalInt.of(3), updated.wsjfRank());
        assertEquals(OptionalInt.of(8), updated.estimate());
        verify(features).save(updated);
    }

    // Die gesendeten Angaben gewinnen gegen die gespeicherten, auch das Leeren optionaler Angaben.
    @Test
    void updateReplacesStoredValuesWithSentOnes() {
        programIncrementExists();
        Feature portal = stored(new Feature(
                UUID.randomUUID(), PI_27_1.id(), "Portal", "Alt", "Anna", OptionalInt.of(5), OptionalInt.of(13)));

        Feature updated = service.update(PI_27_1.id(), portal.id(), " Self-Service-Portal ", null, null, "Beat", null);

        assertEquals("Self-Service-Portal", updated.title());
        assertEquals("", updated.description());
        assertEquals("Beat", updated.owner());
        assertEquals(OptionalInt.empty(), updated.wsjfRank());
        assertEquals(OptionalInt.empty(), updated.estimate());
        verify(features).save(updated);
    }

    @Test
    void updateDoesNotSaveFeatureWithBlankTitle() {
        programIncrementExists();
        Feature login = stored(feature("Login", null));

        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class, () -> service.update(PI_27_1.id(), login.id(), " ", null, 3, null, 8));

        assertEquals(BLANK_TITLE, exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void updateDoesNotSaveFeatureWithRankZero() {
        programIncrementExists();
        Feature login = stored(feature("Login", null));

        assertThrows(
                InvalidFeatureException.class,
                () -> service.update(PI_27_1.id(), login.id(), "Login", null, 0, null, null));
        verify(features, never()).save(any());
    }

    // Das Feature gehört zum unbekannten PI: Nur die Prüfung des PI kann hier ablehnen.
    @Test
    void updateRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        Feature orphan = stored(
                new Feature(UUID.randomUUID(), unknown, "Login", "", "", OptionalInt.empty(), OptionalInt.empty()));

        ProgramIncrementNotFoundException exception = assertThrows(
                ProgramIncrementNotFoundException.class,
                () -> service.update(unknown, orphan.id(), "Login", null, 3, null, 8));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void updateRejectsUnknownFeature() {
        programIncrementExists();
        UUID unknown = UUID.randomUUID();
        lenient().when(features.findOne(unknown)).thenReturn(Optional.empty());

        FeatureNotFoundException exception = assertThrows(
                FeatureNotFoundException.class, () -> service.update(PI_27_1.id(), unknown, "Login", null, 3, null, 8));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Gerüst (FeatureUseCase): Ein Feature eines anderen PI gibt es in diesem PI nicht.
    @Test
    void updateRejectsFeatureOfOtherProgramIncrement() {
        programIncrementExists();
        Feature foreign = stored(inOtherProgramIncrement("Login"));

        FeatureNotFoundException exception = assertThrows(
                FeatureNotFoundException.class,
                () -> service.update(PI_27_1.id(), foreign.id(), "Login", null, 3, null, 8));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // --- US-10: Löschen (Szenario «Feature wird gelöscht») ---

    @Test
    void removeDeletesFeature() {
        programIncrementExists();
        Feature login = stored(feature("Login", null));
        lenient().when(features.remove(login.id())).thenReturn(true);

        service.remove(PI_27_1.id(), login.id());

        verify(features).remove(login.id());
    }

    @Test
    void removeRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        Feature orphan = stored(
                new Feature(UUID.randomUUID(), unknown, "Login", "", "", OptionalInt.empty(), OptionalInt.empty()));
        lenient().when(features.remove(orphan.id())).thenReturn(true);

        ProgramIncrementNotFoundException exception =
                assertThrows(ProgramIncrementNotFoundException.class, () -> service.remove(unknown, orphan.id()));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(features, never()).remove(any());
    }

    // Ob die Umsetzung vorher sucht oder das Ergebnis von remove auswertet, ist offen; beides ist gestubbt.
    @Test
    void removeRejectsUnknownFeature() {
        programIncrementExists();
        UUID unknown = UUID.randomUUID();
        lenient().when(features.findOne(unknown)).thenReturn(Optional.empty());
        lenient().when(features.remove(unknown)).thenReturn(false);

        FeatureNotFoundException exception =
                assertThrows(FeatureNotFoundException.class, () -> service.remove(PI_27_1.id(), unknown));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
    }

    // Über ein anderes PI darf sich ein Feature nicht löschen lassen.
    @Test
    void removeRejectsFeatureOfOtherProgramIncrement() {
        programIncrementExists();
        Feature foreign = stored(inOtherProgramIncrement("Login"));
        lenient().when(features.remove(foreign.id())).thenReturn(true);

        FeatureNotFoundException exception =
                assertThrows(FeatureNotFoundException.class, () -> service.remove(PI_27_1.id(), foreign.id()));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(features, never()).remove(any());
    }

    // --- US-11: CSV-Import (AK 1 und 3, Entscheide «Alles oder nichts» und «Anhängen») ---

    @Test
    void importCsvSavesEveryFeatureInFileOrder() {
        programIncrementExists();

        List<Feature> imported =
                service.importCsv(PI_27_1.id(), "Titel;WSJF-Rang;Owner\nSuche;2;Beat\nPortal;1;Anna\n");

        assertEquals(
                List.of("Suche", "Portal"),
                imported.stream().map(Feature::title).toList());
        assertEquals(OptionalInt.of(2), imported.get(0).wsjfRank());
        assertEquals("Anna", imported.get(1).owner());
        for (Feature feature : imported) {
            assertEquals(PI_27_1.id(), feature.programIncrementId());
        }
        InOrder saving = inOrder(features);
        saving.verify(features).save(imported.get(0));
        saving.verify(features).save(imported.get(1));
        verify(features, times(2)).save(any());
    }

    // Anhängen ohne Abgleich: Das bestehende Backlog wird weder gelesen noch geändert.
    @Test
    void importCsvDoesNotRemoveExistingFeatures() {
        programIncrementExists();

        service.importCsv(PI_27_1.id(), "Titel\nPortal");

        verify(features, never()).remove(any());
    }

    // Szenario «Ungültige Zeile verhindert den ganzen Import»: auch die gültige Zeile 2 wird nicht gespeichert.
    @Test
    void importCsvSavesNothingWhenARowIsInvalid() {
        programIncrementExists();

        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class,
                () -> service.importCsv(PI_27_1.id(), "Titel;WSJF-Rang\nPortal;1\nSuche;null"));

        assertEquals("Zeile 3: Der WSJF-Rang muss eine ganze Zahl ab 1 sein.", exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void importCsvSavesNothingWithoutTitleColumn() {
        programIncrementExists();

        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class, () -> service.importCsv(PI_27_1.id(), "Name;Owner\nPortal;Anna"));

        assertEquals("Die Kopfzeile braucht die Spalte «Titel».", exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void importCsvRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();

        ProgramIncrementNotFoundException exception = assertThrows(
                ProgramIncrementNotFoundException.class, () -> service.importCsv(unknown, "Titel\nPortal"));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Wie beim Erfassen: erst das PI, dann die Datei; unbekanntes PI ist 404, nicht 400.
    @Test
    void importCsvIntoUnknownProgramIncrementIsNotFoundEvenWithInvalidFile() {
        UUID unknown = unknownProgramIncrement();

        assertThrows(ProgramIncrementNotFoundException.class, () -> service.importCsv(unknown, "Name\nPortal"));
        verify(features, never()).save(any());
    }

    // --- US-13: Einplanen und zurück ins Backlog ---

    private static final String TEAM_NOT_FOUND = "Das Team gibt es nicht.";
    private static final String IP_NOT_SCHEDULABLE = "In der IP-Iteration wird keine Feature-Arbeit geplant.";
    private static final String NOT_IN_PROGRAM_INCREMENT = "Die Iteration gehört nicht zu diesem PI.";

    private static final Team ADLER = new Team(UUID.randomUUID(), "Adler");
    private static final Team ZEBRA = new Team(UUID.randomUUID(), "Zebra");

    // lenient: Ob der Service das Team vor oder nach PI und Feature liest, ist seine Sache.
    private void teamExists(Team team) {
        lenient().when(teams.findOne(team.id())).thenReturn(Optional.of(team));
    }

    private static Feature portal() {
        return new Feature(
                UUID.randomUUID(),
                PI_27_1.id(),
                "Portal",
                "Kundenportal",
                "Anna",
                OptionalInt.of(1),
                OptionalInt.of(8));
    }

    private static Feature scheduledIn(Feature feature, Team team, int iterationNumber) {
        return new Feature(
                feature.id(),
                feature.programIncrementId(),
                feature.title(),
                feature.description(),
                feature.owner(),
                feature.wsjfRank(),
                feature.estimate(),
                Optional.of(new Scheduling(team.id(), iterationNumber)));
    }

    // Szenario «Feature wird in eine Zelle eingeplant»
    @Test
    void scheduleSavesAndReturnsFeatureInCell() {
        programIncrementExists();
        teamExists(ADLER);
        Feature portal = stored(portal());

        Feature scheduled = service.schedule(PI_27_1.id(), portal.id(), ADLER.id(), 2);

        assertEquals(scheduledIn(portal, ADLER, 2), scheduled);
        verify(features).save(scheduled);
    }

    // Szenario «Eingeplantes Feature wird verschoben»: Die neue Zelle ersetzt die gespeicherte.
    @Test
    void scheduleMovesScheduledFeatureToNewCell() {
        programIncrementExists();
        teamExists(ZEBRA);
        Feature portal = stored(scheduledIn(portal(), ADLER, 2));

        Feature moved = service.schedule(PI_27_1.id(), portal.id(), ZEBRA.id(), 3);

        assertEquals(Optional.of(new Scheduling(ZEBRA.id(), 3)), moved.scheduling());
        assertEquals(portal.id(), moved.id());
        verify(features).save(moved);
    }

    @Test
    void scheduleRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        teamExists(ADLER);
        Feature orphan = stored(
                new Feature(UUID.randomUUID(), unknown, "Portal", "", "", OptionalInt.empty(), OptionalInt.empty()));

        ProgramIncrementNotFoundException exception = assertThrows(
                ProgramIncrementNotFoundException.class, () -> service.schedule(unknown, orphan.id(), ADLER.id(), 2));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void scheduleRejectsUnknownFeature() {
        programIncrementExists();
        teamExists(ADLER);
        UUID unknown = UUID.randomUUID();
        lenient().when(features.findOne(unknown)).thenReturn(Optional.empty());

        FeatureNotFoundException exception = assertThrows(
                FeatureNotFoundException.class, () -> service.schedule(PI_27_1.id(), unknown, ADLER.id(), 2));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Wie beim Bearbeiten: Ein Feature eines anderen PI gibt es in diesem PI nicht.
    @Test
    void scheduleRejectsFeatureOfOtherProgramIncrement() {
        programIncrementExists();
        teamExists(ADLER);
        Feature foreign = stored(inOtherProgramIncrement("Portal"));

        FeatureNotFoundException exception = assertThrows(
                FeatureNotFoundException.class, () -> service.schedule(PI_27_1.id(), foreign.id(), ADLER.id(), 2));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Szenario «Einplanen bei einem unbekannten Team wird abgelehnt»
    @Test
    void scheduleRejectsUnknownTeam() {
        programIncrementExists();
        Feature portal = stored(portal());
        UUID unknownTeam = UUID.randomUUID();
        lenient().when(teams.findOne(unknownTeam)).thenReturn(Optional.empty());

        TeamNotFoundException exception = assertThrows(
                TeamNotFoundException.class, () -> service.schedule(PI_27_1.id(), portal.id(), unknownTeam, 2));

        assertEquals(TEAM_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Szenario «Einplanen ohne Team und Iteration wird abgelehnt»: Der Body darf Angaben auslassen, das ist ein 400.
    @Test
    void scheduleRejectsMissingTeamOrIterationAndSavesNothing() {
        programIncrementExists();
        Feature portal = stored(portal());

        InvalidFeatureException withoutTeam =
                assertThrows(InvalidFeatureException.class, () -> service.schedule(PI_27_1.id(), portal.id(), null, 2));
        InvalidFeatureException withoutIteration = assertThrows(
                InvalidFeatureException.class,
                () -> service.schedule(PI_27_1.id(), portal.id(), UUID.randomUUID(), null));

        assertEquals("Team und Iteration sind Pflicht.", withoutTeam.getMessage());
        assertEquals("Team und Iteration sind Pflicht.", withoutIteration.getMessage());
        verify(features, never()).save(any());
    }

    // Szenario «Einplanen in die IP-Iteration wird abgelehnt»: PI 27.1 hat 6 Iterationen, die 6. ist die IP.
    @Test
    void scheduleIntoIpIterationSavesNothing() {
        programIncrementExists();
        teamExists(ADLER);
        Feature portal = stored(portal());

        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class, () -> service.schedule(PI_27_1.id(), portal.id(), ADLER.id(), 6));

        assertEquals(IP_NOT_SCHEDULABLE, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Szenario «Einplanen in eine Iteration, die es im PI nicht gibt, wird abgelehnt»
    @Test
    void scheduleIntoIterationOutsideProgramIncrementSavesNothing() {
        programIncrementExists();
        teamExists(ADLER);
        Feature portal = stored(portal());

        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class, () -> service.schedule(PI_27_1.id(), portal.id(), ADLER.id(), 7));

        assertEquals(NOT_IN_PROGRAM_INCREMENT, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Szenario «Eingeplantes Feature geht zurück ins Backlog»
    @Test
    void unscheduleSavesAndReturnsFeatureWithoutCell() {
        programIncrementExists();
        Feature portal = portal();
        stored(scheduledIn(portal, ADLER, 2));

        Feature unscheduled = service.unschedule(PI_27_1.id(), portal.id());

        assertEquals(portal, unscheduled);
        assertFalse(unscheduled.scheduled());
        verify(features).save(unscheduled);
    }

    // Plan: idempotent; ob dabei gespeichert wird, ist offen, das Ergebnis ist das unveränderte Feature.
    @Test
    void unscheduleOfBacklogFeatureReturnsItUnchanged() {
        programIncrementExists();
        Feature portal = stored(portal());

        assertEquals(portal, service.unschedule(PI_27_1.id(), portal.id()));
        verify(features, never()).save(any());
    }

    @Test
    void unscheduleRejectsUnknownProgramIncrement() {
        UUID unknown = unknownProgramIncrement();
        Feature orphan = stored(new Feature(
                UUID.randomUUID(),
                unknown,
                "Portal",
                "",
                "",
                OptionalInt.empty(),
                OptionalInt.empty(),
                Optional.of(new Scheduling(ADLER.id(), 2))));

        ProgramIncrementNotFoundException exception =
                assertThrows(ProgramIncrementNotFoundException.class, () -> service.unschedule(unknown, orphan.id()));

        assertEquals(PI_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void unscheduleRejectsUnknownFeature() {
        programIncrementExists();
        UUID unknown = UUID.randomUUID();
        lenient().when(features.findOne(unknown)).thenReturn(Optional.empty());

        FeatureNotFoundException exception =
                assertThrows(FeatureNotFoundException.class, () -> service.unschedule(PI_27_1.id(), unknown));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    @Test
    void unscheduleRejectsFeatureOfOtherProgramIncrement() {
        programIncrementExists();
        Feature foreign = inOtherProgramIncrement("Portal");
        stored(new Feature(
                foreign.id(),
                foreign.programIncrementId(),
                "Portal",
                "",
                "",
                OptionalInt.empty(),
                OptionalInt.empty(),
                Optional.of(new Scheduling(ADLER.id(), 2))));

        FeatureNotFoundException exception =
                assertThrows(FeatureNotFoundException.class, () -> service.unschedule(PI_27_1.id(), foreign.id()));

        assertEquals(FEATURE_NOT_FOUND, exception.getMessage());
        verify(features, never()).save(any());
    }

    // Szenario «Bearbeiten eines eingeplanten Features lässt die Einplanung stehen»
    @Test
    void updateKeepsSchedulingOfScheduledFeature() {
        programIncrementExists();
        Feature portal = stored(scheduledIn(portal(), ADLER, 2));

        Feature updated = service.update(PI_27_1.id(), portal.id(), "Portal neu", "Kundenportal", 1, "Anna", 8);

        assertEquals("Portal neu", updated.title());
        assertEquals(Optional.of(new Scheduling(ADLER.id(), 2)), updated.scheduling());
        verify(features).save(updated);
    }
}
