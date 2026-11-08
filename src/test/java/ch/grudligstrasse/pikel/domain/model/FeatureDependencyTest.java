package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** US-16: Regeln für eine Abhängigkeit «Feature A braucht Feature B» (Delta, Abschnitt «Entscheide»). */
class FeatureDependencyTest {

    private static final UUID PI_27_1 = UUID.randomUUID();
    private static final UUID PI_27_2 = UUID.randomUUID();

    private static final String SELF = "Ein Feature kann nicht von sich selbst abhängen.";
    private static final String OTHER_PI = "Beide Features müssen zum selben PI gehören.";
    private static final String DUPLICATE = "Diese Abhängigkeit gibt es schon.";
    private static final String REVERSE = "Die beiden Features hängen schon umgekehrt voneinander ab.";

    private static final Feature PORTAL = feature(UUID.randomUUID(), PI_27_1, "Portal");
    private static final Feature LOGIN = feature(UUID.randomUUID(), PI_27_1, "Login");
    private static final Feature SEARCH = feature(UUID.randomUUID(), PI_27_1, "Suche");
    private static final Feature LOGIN_IN_27_2 = feature(UUID.randomUUID(), PI_27_2, "Login");

    private static Feature feature(UUID id, UUID programIncrementId, String title) {
        return new Feature(id, programIncrementId, title, "", "", OptionalInt.empty(), OptionalInt.empty());
    }

    private static FeatureDependency needs(Feature needing, Feature needed) {
        return new FeatureDependency(needing.id(), needed.id());
    }

    private static String rejection(Feature needing, Feature needed, List<FeatureDependency> existing) {
        return assertThrows(InvalidFeatureException.class, () -> FeatureDependency.between(needing, needed, existing))
                .getMessage();
    }

    // --- AK 1: Ein Feature braucht ein anderes Feature desselben PI ---

    // Richtung: Das erste Feature ist der Bedarf, das zweite die Lieferung; vertauscht wäre die Aussage falsch.
    @Test
    void featureCanNeedAnotherFeatureOfSameProgramIncrement() {
        FeatureDependency dependency = FeatureDependency.between(PORTAL, LOGIN, List.of());

        assertEquals(PORTAL.id(), dependency.needingFeatureId());
        assertEquals(LOGIN.id(), dependency.neededFeatureId());
    }

    // Andere Abhängigkeiten mit einem der beiden Features sind weder doppelt noch umgekehrt.
    @Test
    void dependenciesSharingOnlyOneFeatureDoNotBlock() {
        List<FeatureDependency> existing = List.of(needs(PORTAL, SEARCH), needs(SEARCH, LOGIN), needs(LOGIN, SEARCH));

        FeatureDependency dependency = FeatureDependency.between(PORTAL, LOGIN, existing);

        assertEquals(needs(PORTAL, LOGIN), dependency);
    }

    // Ein Feature darf mehrere Features brauchen und von mehreren gebraucht werden.
    @Test
    void featureCanNeedSeveralFeatures() {
        FeatureDependency dependency = FeatureDependency.between(PORTAL, SEARCH, List.of(needs(PORTAL, LOGIN)));

        assertEquals(needs(PORTAL, SEARCH), dependency);
    }

    // AK 2 / Delta: Längere Zyklen erkennt PIkel in M1 nicht; Portal → Login → Suche, dann Suche → Portal ist erlaubt.
    @Test
    void longerCycleIsNotRejected() {
        List<FeatureDependency> existing = List.of(needs(PORTAL, LOGIN), needs(LOGIN, SEARCH));

        FeatureDependency dependency = FeatureDependency.between(SEARCH, PORTAL, existing);

        assertEquals(needs(SEARCH, PORTAL), dependency);
    }

    // Szenario «Abhängigkeit zu einem Feature aus einem anderen PI wird abgelehnt»
    @Test
    void featureOfOtherProgramIncrementIsRejected() {
        assertEquals(OTHER_PI, rejection(PORTAL, LOGIN_IN_27_2, List.of()));
    }

    // Auch umgekehrt: Das brauchende Feature liegt im anderen PI.
    @Test
    void needingFeatureOfOtherProgramIncrementIsRejected() {
        assertEquals(OTHER_PI, rejection(LOGIN_IN_27_2, PORTAL, List.of()));
    }

    // --- AK 2: Selbstbezug, doppelte und direkt umgekehrte Abhängigkeit ---

    // Szenario «Feature, das sich selbst braucht, wird abgelehnt»
    @Test
    void featureCannotNeedItself() {
        assertEquals(SELF, rejection(PORTAL, PORTAL, List.of()));
    }

    // Selbstbezug heisst gleiche ID, nicht gleiches Objekt: eine zweite Fassung desselben Features mit anderem Titel.
    @Test
    void featureCannotNeedAnotherVersionOfItself() {
        Feature renamedPortal = feature(PORTAL.id(), PI_27_1, "Self-Service-Portal");

        assertEquals(SELF, rejection(PORTAL, renamedPortal, List.of()));
    }

    // Gleicher Titel ist kein Selbstbezug: «Login» in zwei Fassungen mit verschiedener ID im selben PI.
    @Test
    void featuresWithSameTitleButDifferentIdsAreNotSelfReference() {
        Feature secondLogin = feature(UUID.randomUUID(), PI_27_1, "Login");

        FeatureDependency dependency = FeatureDependency.between(LOGIN, secondLogin, List.of());

        assertEquals(new FeatureDependency(LOGIN.id(), secondLogin.id()), dependency);
    }

    // Szenario «Doppelte Abhängigkeit wird abgelehnt»
    @Test
    void duplicateDependencyIsRejected() {
        assertEquals(DUPLICATE, rejection(PORTAL, LOGIN, List.of(needs(SEARCH, PORTAL), needs(PORTAL, LOGIN))));
    }

    // Szenario «Umgekehrte Abhängigkeit wird abgelehnt»
    @Test
    void directReverseDependencyIsRejected() {
        assertEquals(REVERSE, rejection(LOGIN, PORTAL, List.of(needs(SEARCH, PORTAL), needs(PORTAL, LOGIN))));
    }

    // --- Prüfreihenfolge (Gerüst): Selbstbezug → anderes PI → doppelt → umgekehrt ---

    // Gleiche ID, aber die zweite Fassung nennt ein anderes PI: Der Selbstbezug geht vor.
    @Test
    void selfReferenceIsReportedBeforeOtherProgramIncrement() {
        Feature portalIn27_2 = feature(PORTAL.id(), PI_27_2, "Portal");

        assertEquals(SELF, rejection(PORTAL, portalIn27_2, List.of(needs(PORTAL, PORTAL))));
    }

    @Test
    void otherProgramIncrementIsReportedBeforeDuplicate() {
        assertEquals(OTHER_PI, rejection(PORTAL, LOGIN_IN_27_2, List.of(needs(PORTAL, LOGIN_IN_27_2))));
    }

    @Test
    void otherProgramIncrementIsReportedBeforeReverse() {
        assertEquals(OTHER_PI, rejection(PORTAL, LOGIN_IN_27_2, List.of(needs(LOGIN_IN_27_2, PORTAL))));
    }

    // Bestehen beide Richtungen schon (Altbestand), meldet PIkel die doppelte.
    @Test
    void duplicateIsReportedBeforeReverse() {
        assertEquals(DUPLICATE, rejection(PORTAL, LOGIN, List.of(needs(LOGIN, PORTAL), needs(PORTAL, LOGIN))));
    }
}
