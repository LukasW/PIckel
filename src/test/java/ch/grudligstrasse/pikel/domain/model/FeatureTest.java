package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
}
