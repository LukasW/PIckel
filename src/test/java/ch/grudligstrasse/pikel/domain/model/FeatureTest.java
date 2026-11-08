package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FeatureTest {

    private static final UUID PI = UUID.randomUUID();

    private static final String BLANK_TITLE = "Der Titel darf nicht leer sein.";
    private static final String TITLE_TOO_LONG = "Der Titel darf höchstens 200 Zeichen lang sein.";
    private static final String DESCRIPTION_TOO_LONG = "Die Beschreibung darf höchstens 5000 Zeichen lang sein.";
    private static final String OWNER_TOO_LONG = "Der Owner darf höchstens 100 Zeichen lang sein.";
    private static final String INVALID_RANK = "Der WSJF-Rang muss eine ganze Zahl ab 1 sein.";
    private static final String INVALID_ESTIMATE = "Die Schätzung muss eine ganze Zahl ab 0 sein.";

    private static final int MAX_TITLE = 200;
    private static final int MAX_DESCRIPTION = 5000;
    private static final int MAX_OWNER = 100;

    private static Feature titleOnly(String title) {
        return Feature.create(PI, title, null, null, null, null);
    }

    private static InvalidFeatureException rejected(
            String title, String description, Integer wsjfRank, String owner, Integer estimate) {
        return assertThrows(
                InvalidFeatureException.class, () -> Feature.create(PI, title, description, wsjfRank, owner, estimate));
    }

    // --- AK 1: Titel Pflicht, getrimmt, höchstens 200 Zeichen ---

    @Test
    void createTrimsTitle() {
        assertEquals("Self-Service-Portal", titleOnly("  Self-Service-Portal ").title());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t"})
    void rejectsBlankTitle(String title) {
        assertEquals(BLANK_TITLE, rejected(title, null, null, null, null).getMessage());
    }

    // «Pflicht»: ein fehlender Titel (JSON ohne Feld) ist wie ein leerer Titel.
    @Test
    void rejectsMissingTitle() {
        assertEquals(BLANK_TITLE, rejected(null, null, null, null, null).getMessage());
    }

    @Test
    void acceptsTitleOfExactly200Characters() {
        String title = "x".repeat(MAX_TITLE);

        assertEquals(title, titleOnly(title).title());
    }

    @Test
    void rejectsTitleOf201Characters() {
        String title = "x".repeat(MAX_TITLE + 1);

        assertEquals(TITLE_TOO_LONG, rejected(title, null, null, null, null).getMessage());
    }

    // Die Länge zählt nach dem Trimmen: Leerzeichen am Rand machen einen Titel nicht zu lang.
    @Test
    void acceptsTitleOf200CharactersSurroundedByBlanks() {
        String title = "x".repeat(MAX_TITLE);

        assertEquals(title, titleOnly("  " + title + "  ").title());
    }

    // --- AK 2: Beschreibung optional, höchstens 5000 Zeichen ---

    @Test
    void missingDescriptionIsEmptyText() {
        assertEquals("", titleOnly("Login").description());
    }

    @Test
    void blankDescriptionIsEmptyText() {
        assertEquals("", Feature.create(PI, "Login", "   ", null, null, null).description());
    }

    @Test
    void createTrimsDescription() {
        assertEquals(
                "Kunden ändern ihre Adresse selbst",
                Feature.create(PI, "Login", "  Kunden ändern ihre Adresse selbst \n", null, null, null)
                        .description());
    }

    @Test
    void acceptsDescriptionOfExactly5000Characters() {
        String description = "d".repeat(MAX_DESCRIPTION);

        assertEquals(
                description,
                Feature.create(PI, "Login", description, null, null, null).description());
    }

    @Test
    void acceptsDescriptionOf5000CharactersSurroundedByBlanks() {
        String description = "d".repeat(MAX_DESCRIPTION);

        assertEquals(
                description,
                Feature.create(PI, "Login", " " + description + " ", null, null, null)
                        .description());
    }

    @Test
    void rejectsDescriptionOf5001Characters() {
        String description = "d".repeat(MAX_DESCRIPTION + 1);

        assertEquals(
                DESCRIPTION_TOO_LONG,
                rejected("Login", description, null, null, null).getMessage());
    }

    // --- AK 2: Owner optional, höchstens 100 Zeichen ---

    @Test
    void missingOwnerIsEmptyText() {
        assertEquals("", titleOnly("Login").owner());
    }

    @Test
    void blankOwnerIsEmptyText() {
        assertEquals("", Feature.create(PI, "Login", null, null, "  ", null).owner());
    }

    @Test
    void createTrimsOwner() {
        assertEquals(
                "Anna", Feature.create(PI, "Login", null, null, " Anna  ", null).owner());
    }

    @Test
    void acceptsOwnerOfExactly100Characters() {
        String owner = "o".repeat(MAX_OWNER);

        assertEquals(owner, Feature.create(PI, "Login", null, null, owner, null).owner());
    }

    @Test
    void acceptsOwnerOf100CharactersSurroundedByBlanks() {
        String owner = "o".repeat(MAX_OWNER);

        assertEquals(
                owner,
                Feature.create(PI, "Login", null, null, "  " + owner + " ", null)
                        .owner());
    }

    @Test
    void rejectsOwnerOf101Characters() {
        String owner = "o".repeat(MAX_OWNER + 1);

        assertEquals(OWNER_TOO_LONG, rejected("Login", null, null, owner, null).getMessage());
    }

    // --- AK 2: WSJF-Rang optional, ganze Zahl ab 1 ---

    @Test
    void missingRankIsEmpty() {
        assertEquals(OptionalInt.empty(), titleOnly("Login").wsjfRank());
    }

    @Test
    void acceptsRankOne() {
        assertEquals(
                OptionalInt.of(1),
                Feature.create(PI, "Login", null, 1, null, null).wsjfRank());
    }

    // Szenario «WSJF-Rang 0 wird abgelehnt»
    @Test
    void rejectsRankZero() {
        assertEquals(INVALID_RANK, rejected("Login", null, 0, null, null).getMessage());
    }

    @Test
    void rejectsNegativeRank() {
        assertEquals(INVALID_RANK, rejected("Login", null, -3, null, null).getMessage());
    }

    // --- AK 2: Schätzung optional, ganze Zahl ab 0 ---

    @Test
    void missingEstimateIsEmpty() {
        assertEquals(OptionalInt.empty(), titleOnly("Login").estimate());
    }

    @Test
    void acceptsEstimateZero() {
        assertEquals(
                OptionalInt.of(0),
                Feature.create(PI, "Login", null, null, null, 0).estimate());
    }

    // Szenario «Negative Schätzung wird abgelehnt»
    @Test
    void rejectsEstimateMinusOne() {
        assertEquals(INVALID_ESTIMATE, rejected("Login", null, null, null, -1).getMessage());
    }

    // Die Regeln gelten auch für den kanonischen Konstruktor (Compact Constructor laut Plan).
    @Test
    void constructorRejectsRankZero() {
        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class,
                () -> new Feature(UUID.randomUUID(), PI, "Login", "", "", OptionalInt.of(0), OptionalInt.empty()));

        assertEquals(INVALID_RANK, exception.getMessage());
    }

    @Test
    void constructorRejectsNegativeEstimate() {
        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class,
                () -> new Feature(UUID.randomUUID(), PI, "Login", "", "", OptionalInt.empty(), OptionalInt.of(-1)));

        assertEquals(INVALID_ESTIMATE, exception.getMessage());
    }

    @Test
    void constructorRejectsBlankTitle() {
        InvalidFeatureException exception = assertThrows(
                InvalidFeatureException.class,
                () -> new Feature(UUID.randomUUID(), PI, " ", "", "", OptionalInt.empty(), OptionalInt.empty()));

        assertEquals(BLANK_TITLE, exception.getMessage());
    }

    // --- AK 2 und Szenario «Feature wird erfasst»: alle Angaben werden übernommen ---

    @Test
    void createKeepsAllGivenValues() {
        Feature feature = Feature.create(PI, "Self-Service-Portal", "Adresse selbst ändern", 1, "Anna", 21);

        assertEquals("Self-Service-Portal", feature.title());
        assertEquals("Adresse selbst ändern", feature.description());
        assertEquals(OptionalInt.of(1), feature.wsjfRank());
        assertEquals("Anna", feature.owner());
        assertEquals(OptionalInt.of(21), feature.estimate());
    }

    // --- AK 3: gehört zum PI, in dem es erfasst wird, und ist nicht eingeplant ---

    @Test
    void createBelongsToGivenProgramIncrement() {
        assertEquals(PI, titleOnly("Login").programIncrementId());
    }

    @Test
    void createAssignsOwnIdToEachFeature() {
        Feature first = titleOnly("Login");
        Feature second = titleOnly("Login");

        assertNotNull(first.id());
        assertNotEquals(first.id(), second.id());
    }

    @Test
    void newFeatureIsNotScheduled() {
        assertFalse(
                Feature.create(PI, "Self-Service-Portal", null, 1, "Anna", 21).scheduled());
    }

    // --- US-10: Bearbeiten mit denselben Regeln wie beim Erfassen ---

    private static final Feature PORTAL = Feature.create(PI, "Portal", "Alt", 5, "Anna", 13);

    private static InvalidFeatureException revisionRejected(
            String title, String description, Integer wsjfRank, String owner, Integer estimate) {
        return assertThrows(
                InvalidFeatureException.class, () -> PORTAL.revise(title, description, wsjfRank, owner, estimate));
    }

    // Bearbeiten ändert das Feature, es entsteht kein neues: ID und PI bleiben.
    @Test
    void reviseKeepsIdAndProgramIncrement() {
        Feature revised = PORTAL.revise("Self-Service-Portal", "Neu", 1, "Beat", 21);

        assertEquals(PORTAL.id(), revised.id());
        assertEquals(PI, revised.programIncrementId());
    }

    // Jeder Wert weicht von der alten Fassung ab, damit sichtbar ist, dass die neue gewinnt.
    @Test
    void reviseTakesAllNewValues() {
        Feature revised = PORTAL.revise("Self-Service-Portal", "Adresse selbst ändern", 1, "Beat", 21);

        assertEquals("Self-Service-Portal", revised.title());
        assertEquals("Adresse selbst ändern", revised.description());
        assertEquals(OptionalInt.of(1), revised.wsjfRank());
        assertEquals("Beat", revised.owner());
        assertEquals(OptionalInt.of(21), revised.estimate());
    }

    // Szenario «Feature wird bearbeitet»: WSJF-Rang auf 3, Schätzung auf 8, der Rest bleibt wie gesendet.
    @Test
    void reviseSetsRankAndEstimateOfFeatureWithoutRank() {
        Feature login = titleOnly("Login");

        Feature revised = login.revise("Login", null, 3, null, 8);

        assertEquals("Login", revised.title());
        assertEquals(OptionalInt.of(3), revised.wsjfRank());
        assertEquals(OptionalInt.of(8), revised.estimate());
    }

    // Wie beim Erfassen heisst null «fehlt»: Optionale Angaben lassen sich wieder leeren.
    @Test
    void reviseWithoutOptionalValuesClearsThem() {
        Feature revised = PORTAL.revise("Portal", null, null, null, null);

        assertEquals("", revised.description());
        assertEquals("", revised.owner());
        assertEquals(OptionalInt.empty(), revised.wsjfRank());
        assertEquals(OptionalInt.empty(), revised.estimate());
    }

    @Test
    void reviseTrimsTitleDescriptionAndOwner() {
        Feature revised = PORTAL.revise("  Portal 2 ", " Neu \n", null, " Beat  ", null);

        assertEquals("Portal 2", revised.title());
        assertEquals("Neu", revised.description());
        assertEquals("Beat", revised.owner());
    }

    // Szenario «Bearbeiten mit leerem Titel wird abgelehnt»
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void reviseRejectsBlankTitle(String title) {
        assertEquals(
                BLANK_TITLE, revisionRejected(title, null, null, null, null).getMessage());
    }

    @Test
    void reviseRejectsMissingTitle() {
        assertEquals(BLANK_TITLE, revisionRejected(null, null, null, null, null).getMessage());
    }

    @Test
    void reviseAcceptsLimitsOfCreate() {
        Feature revised =
                PORTAL.revise("x".repeat(MAX_TITLE), "d".repeat(MAX_DESCRIPTION), 1, "o".repeat(MAX_OWNER), 0);

        assertEquals(MAX_TITLE, revised.title().length());
        assertEquals(MAX_DESCRIPTION, revised.description().length());
        assertEquals(MAX_OWNER, revised.owner().length());
        assertEquals(OptionalInt.of(1), revised.wsjfRank());
        assertEquals(OptionalInt.of(0), revised.estimate());
    }

    @Test
    void reviseRejectsTitleOf201Characters() {
        assertEquals(
                TITLE_TOO_LONG,
                revisionRejected("x".repeat(MAX_TITLE + 1), null, null, null, null)
                        .getMessage());
    }

    @Test
    void reviseRejectsDescriptionOf5001Characters() {
        assertEquals(
                DESCRIPTION_TOO_LONG,
                revisionRejected("Portal", "d".repeat(MAX_DESCRIPTION + 1), null, null, null)
                        .getMessage());
    }

    @Test
    void reviseRejectsOwnerOf101Characters() {
        assertEquals(
                OWNER_TOO_LONG,
                revisionRejected("Portal", null, null, "o".repeat(MAX_OWNER + 1), null)
                        .getMessage());
    }

    @Test
    void reviseRejectsRankZero() {
        assertEquals(
                INVALID_RANK, revisionRejected("Portal", null, 0, null, null).getMessage());
    }

    @Test
    void reviseRejectsEstimateMinusOne() {
        assertEquals(
                INVALID_ESTIMATE,
                revisionRejected("Portal", null, null, null, -1).getMessage());
    }

    // --- US-13: Einplanen in die Zelle Team × Iteration ---

    private static final String IP_NOT_SCHEDULABLE = "In der IP-Iteration wird keine Feature-Arbeit geplant.";
    private static final String NOT_IN_PROGRAM_INCREMENT = "Die Iteration gehört nicht zu diesem PI.";

    // US-5: 2027-01-04 bis 2027-03-26 zu 2 Wochen hat 6 Iterationen, die 6. ist die IP-Iteration.
    private static final ProgramIncrement PI_TWO_WEEKS =
            new ProgramIncrement(PI, "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 2);
    // Gleicher Zeitraum zu 4 Wochen: 3 Iterationen, die 3. ist die IP-Iteration.
    private static final ProgramIncrement PI_FOUR_WEEKS =
            new ProgramIncrement(PI, "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 4);

    private static final UUID ADLER = UUID.randomUUID();
    private static final UUID ZEBRA = UUID.randomUUID();

    private static InvalidFeatureException schedulingRejected(ProgramIncrement pi, int iterationNumber) {
        return assertThrows(InvalidFeatureException.class, () -> PORTAL.schedule(pi, ADLER, iterationNumber));
    }

    // AK 1: danach liegt das Feature in genau dieser Zelle.
    @Test
    void scheduleIntoDevelopmentIterationSetsTeamAndIteration() {
        Feature scheduled = PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2);

        assertEquals(Optional.of(new Scheduling(ADLER, 2)), scheduled.scheduling());
        assertTrue(scheduled.scheduled());
    }

    // Einplanen ändert nur die Zelle: Es entsteht kein neues Feature, die Angaben bleiben.
    @Test
    void scheduleKeepsIdProgramIncrementAndAllValues() {
        Feature scheduled = PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2);

        assertEquals(PORTAL.id(), scheduled.id());
        assertEquals(PI, scheduled.programIncrementId());
        assertEquals("Portal", scheduled.title());
        assertEquals("Alt", scheduled.description());
        assertEquals("Anna", scheduled.owner());
        assertEquals(OptionalInt.of(5), scheduled.wsjfRank());
        assertEquals(OptionalInt.of(13), scheduled.estimate());
    }

    // Grenzen: die erste und die letzte Entwicklungs-Iteration (die vorletzte des PI) gehen.
    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void scheduleAcceptsFirstAndLastDevelopmentIteration(int number) {
        assertEquals(
                Optional.of(new Scheduling(ADLER, number)),
                PORTAL.schedule(PI_TWO_WEEKS, ADLER, number).scheduling());
    }

    // AK 3, Szenario «Einplanen in die IP-Iteration wird abgelehnt»
    @Test
    void scheduleIntoIpIterationIsRejected() {
        assertEquals(IP_NOT_SCHEDULABLE, schedulingRejected(PI_TWO_WEEKS, 6).getMessage());
    }

    // Die IP-Iteration ist die letzte des PI, nicht eine feste Nummer: Nur die Iterationslänge weicht ab.
    @Test
    void ipIterationDependsOnIterationLength() {
        assertEquals(IP_NOT_SCHEDULABLE, schedulingRejected(PI_FOUR_WEEKS, 3).getMessage());
        assertEquals(
                Optional.of(new Scheduling(ADLER, 3)),
                PORTAL.schedule(PI_TWO_WEEKS, ADLER, 3).scheduling());
    }

    // Szenario «Einplanen in eine Iteration, die es im PI nicht gibt, wird abgelehnt»; 7 = letzte + 1.
    @ParameterizedTest
    @ValueSource(ints = {0, 7, -1, 100})
    void scheduleIntoIterationOutsideProgramIncrementIsRejected(int number) {
        assertEquals(
                NOT_IN_PROGRAM_INCREMENT,
                schedulingRejected(PI_TWO_WEEKS, number).getMessage());
    }

    // Nummer 4 gibt es zu 2 Wochen, zu 4 Wochen nicht mehr (dort ist schon 3 die IP-Iteration).
    @Test
    void iterationAfterIpOfShorterPlanIsRejected() {
        assertEquals(
                NOT_IN_PROGRAM_INCREMENT, schedulingRejected(PI_FOUR_WEEKS, 4).getMessage());
    }

    // Plan: Die Einplanung gilt nur im eigenen PI. Gleicher Zeitraum, nur die ID weicht ab. Die Meldung legt die
    // Spezifikation nicht fest.
    @Test
    void scheduleWithOtherProgramIncrementIsRejected() {
        ProgramIncrement other = new ProgramIncrement(
                UUID.randomUUID(), "PI 27.1", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 3, 26), 2);

        assertThrows(InvalidFeatureException.class, () -> PORTAL.schedule(other, ADLER, 2));
    }

    // AK 2: Verschieben in eine andere Zelle; ein Feature liegt in höchstens einer Zelle. Je Fall weicht nur das Team
    // oder nur die Iteration ab.
    @ParameterizedTest
    @CsvSource({"true, 2", "false, 3"})
    void rescheduleReplacesTheCell(boolean otherTeam, int number) {
        UUID team = otherTeam ? ZEBRA : ADLER;
        Feature moved = PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2).schedule(PI_TWO_WEEKS, team, number);

        assertEquals(Optional.of(new Scheduling(team, number)), moved.scheduling());
    }

    @Test
    void rescheduleToOtherTeamAndIterationReplacesBoth() {
        Feature moved = PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2).schedule(PI_TWO_WEEKS, ZEBRA, 3);

        assertEquals(Optional.of(new Scheduling(ZEBRA, 3)), moved.scheduling());
    }

    // Ein abgelehntes Verschieben ändert die bisherige Einplanung nicht (Records sind unveränderlich).
    @Test
    void rejectedRescheduleKeepsFormerCell() {
        Feature scheduled = PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2);

        assertThrows(InvalidFeatureException.class, () -> scheduled.schedule(PI_TWO_WEEKS, ZEBRA, 6));
        assertEquals(Optional.of(new Scheduling(ADLER, 2)), scheduled.scheduling());
    }

    // AK 2: zurück ins Backlog.
    @Test
    void unscheduleReturnsFeatureToBacklog() {
        Feature unscheduled = PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2).unschedule();

        assertEquals(Optional.empty(), unscheduled.scheduling());
        assertFalse(unscheduled.scheduled());
    }

    // Zurücknehmen ergibt dasselbe Feature wie vor dem Einplanen: ID und alle Angaben bleiben.
    @Test
    void unscheduleOfScheduledFeatureEqualsFeatureBeforeScheduling() {
        assertEquals(PORTAL, PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2).unschedule());
    }

    // Plan: idempotent; ein Feature im Backlog bleibt unverändert.
    @Test
    void unscheduleOfBacklogFeatureLeavesItUnchanged() {
        assertEquals(PORTAL, PORTAL.unschedule());
    }

    // Szenario «Bearbeiten eines eingeplanten Features lässt die Einplanung stehen»
    @Test
    void reviseKeepsScheduling() {
        Feature scheduled = PORTAL.schedule(PI_TWO_WEEKS, ADLER, 2);

        Feature revised = scheduled.revise("Portal neu", "Alt", 5, "Anna", 13);

        assertEquals("Portal neu", revised.title());
        assertEquals(Optional.of(new Scheduling(ADLER, 2)), revised.scheduling());
        assertTrue(revised.scheduled());
    }

    @Test
    void reviseOfBacklogFeatureKeepsItInBacklog() {
        Feature revised = PORTAL.revise("Portal neu", "Alt", 5, "Anna", 13);

        assertEquals(Optional.empty(), revised.scheduling());
        assertFalse(revised.scheduled());
    }

    // «eingeplant» folgt der Einplanung, auch beim kanonischen Konstruktor (so lädt die Persistenz).
    @Test
    void featureWithSchedulingIsScheduled() {
        Feature feature = new Feature(
                UUID.randomUUID(),
                PI,
                "Portal",
                "",
                "",
                OptionalInt.empty(),
                OptionalInt.empty(),
                Optional.of(new Scheduling(ADLER, 2)));

        assertTrue(feature.scheduled());
    }

    @Test
    void featureWithoutSchedulingIsNotScheduled() {
        Feature feature = new Feature(
                UUID.randomUUID(), PI, "Portal", "", "", OptionalInt.empty(), OptionalInt.empty(), Optional.empty());

        assertFalse(feature.scheduled());
    }
}
