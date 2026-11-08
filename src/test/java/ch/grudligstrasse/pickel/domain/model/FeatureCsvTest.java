package ch.grudligstrasse.pickel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// US-11: Spezifikation spec/changes/11-csv-import/delta.md (AK 1–3 und 5, Entscheide «Format», «Regeln», «Grösse»).
class FeatureCsvTest {

    private static final UUID PI = UUID.randomUUID();

    private static final String MISSING_TITLE_COLUMN = "Die Kopfzeile braucht die Spalte «Titel».";
    private static final String TOO_LARGE = "Die Datei darf höchstens 1 MB gross sein.";
    private static final String TOO_MANY_ROWS = "Die Datei darf höchstens 1000 Datenzeilen haben.";
    private static final String INVALID_RANK = "Der WSJF-Rang muss eine ganze Zahl ab 1 sein.";
    private static final String INVALID_ESTIMATE = "Die Schätzung muss eine ganze Zahl ab 0 sein.";
    private static final String BLANK_TITLE = "Der Titel darf nicht leer sein.";
    private static final String TITLE_TOO_LONG = "Der Titel darf höchstens 200 Zeichen lang sein.";
    private static final String UNCLOSED_QUOTE = "Das Anführungszeichen ist nicht geschlossen.";

    private static final int MAX_BYTES = 1_000_000;
    private static final int MAX_ROWS = 1000;
    private static final int MAX_TITLE = 200;

    private static List<Feature> parse(String csv) {
        return FeatureCsv.parse(PI, csv);
    }

    private static String rejected(String csv) {
        return assertThrows(InvalidFeatureException.class, () -> FeatureCsv.parse(PI, csv))
                .getMessage();
    }

    private static List<String> titles(List<Feature> features) {
        return features.stream().map(Feature::title).toList();
    }

    private static Feature only(String csv) {
        List<Feature> features = parse(csv);
        assertEquals(1, features.size());
        return features.getFirst();
    }

    // --- AK 1: jede Datenzeile wird ein Feature (Szenario «CSV mit Semikolon wird importiert») ---

    @Test
    void eachDataRowBecomesFeatureInFileOrder() {
        List<Feature> features = parse("Titel;WSJF-Rang;Owner;Schätzung\nPortal;1;Anna;21\nSuche;2;Beat;13\n");

        assertEquals(List.of("Portal", "Suche"), titles(features));
        Feature portal = features.get(0);
        assertEquals(OptionalInt.of(1), portal.wsjfRank());
        assertEquals("Anna", portal.owner());
        assertEquals(OptionalInt.of(21), portal.estimate());
        Feature suche = features.get(1);
        assertEquals(OptionalInt.of(2), suche.wsjfRank());
        assertEquals("Beat", suche.owner());
        assertEquals(OptionalInt.of(13), suche.estimate());
    }

    // Dateireihenfolge, nicht Rang: Sortiert wird erst im Backlog (US-10).
    @Test
    void keepsFileOrderEvenWhenRanksDescend() {
        assertEquals(List.of("Suche", "Portal"), titles(parse("Titel;WSJF-Rang\nSuche;2\nPortal;1")));
    }

    @Test
    void readsAllFiveColumns() {
        Feature feature = only("Titel;Beschreibung;WSJF-Rang;Owner;Schätzung\nPortal;Adresse selbst ändern;3;Anna;8");

        assertEquals("Portal", feature.title());
        assertEquals("Adresse selbst ändern", feature.description());
        assertEquals(OptionalInt.of(3), feature.wsjfRank());
        assertEquals("Anna", feature.owner());
        assertEquals(OptionalInt.of(8), feature.estimate());
    }

    // Entscheid «Format»: Reihenfolge der Spalten frei.
    @Test
    void columnOrderIsFree() {
        Feature feature = only("Schätzung;Owner;Beschreibung;WSJF-Rang;Titel\n8;Anna;Adresse ändern;3;Portal");

        assertEquals("Portal", feature.title());
        assertEquals("Adresse ändern", feature.description());
        assertEquals(OptionalInt.of(3), feature.wsjfRank());
        assertEquals("Anna", feature.owner());
        assertEquals(OptionalInt.of(8), feature.estimate());
    }

    // Entscheid «Format»: nur «Titel» muss vorkommen.
    @Test
    void titleColumnAloneSuffices() {
        Feature feature = only("Titel\nPortal");

        assertEquals("Portal", feature.title());
        assertEquals("", feature.description());
        assertEquals("", feature.owner());
        assertEquals(OptionalInt.empty(), feature.wsjfRank());
        assertEquals(OptionalInt.empty(), feature.estimate());
    }

    @Test
    void emptyOptionalFieldsAreMissing() {
        Feature feature = only("Titel;Beschreibung;WSJF-Rang;Owner;Schätzung\nPortal;;;;");

        assertEquals("", feature.description());
        assertEquals("", feature.owner());
        assertEquals(OptionalInt.empty(), feature.wsjfRank());
        assertEquals(OptionalInt.empty(), feature.estimate());
    }

    // Entscheid «Regeln»: unbekannte Spalten werden ignoriert, auch zwischen bekannten.
    @Test
    void ignoresUnknownColumns() {
        Feature feature = only("Status;Titel;Jira-Key;Owner\noffen;Portal;PIK-1;Anna");

        assertEquals("Portal", feature.title());
        assertEquals("Anna", feature.owner());
        assertEquals("", feature.description());
    }

    // Entscheid «Regeln»: leere Zeilen übersprungen, auch mehrere und am Ende.
    @Test
    void skipsEmptyLines() {
        assertEquals(List.of("Portal", "Suche"), titles(parse("Titel;Owner\n\nPortal;Anna\n\n\nSuche;Beat\n\n")));
    }

    @Test
    void trailingLineBreakAddsNoFeature() {
        assertEquals(List.of("Portal"), titles(parse("Titel\nPortal\n")));
    }

    // Werte werden getrimmt; beim Rang muss das vor der Zahl geschehen.
    @Test
    void trimsValues() {
        Feature feature = only("Titel;Beschreibung;WSJF-Rang;Owner;Schätzung\n  Portal ; Text ; 3 ; Anna ; 8 ");

        assertEquals("Portal", feature.title());
        assertEquals("Text", feature.description());
        assertEquals(OptionalInt.of(3), feature.wsjfRank());
        assertEquals("Anna", feature.owner());
        assertEquals(OptionalInt.of(8), feature.estimate());
    }

    @Test
    void eachFeatureBelongsToProgramIncrementAndIsNotScheduled() {
        List<Feature> features = parse("Titel\nPortal\nSuche");

        assertEquals(2, features.size());
        for (Feature feature : features) {
            assertEquals(PI, feature.programIncrementId());
            assertFalse(feature.scheduled());
        }
    }

    // Kein Abgleich (Entscheid «Anhängen»): Gleiche Titel bleiben zwei Features mit eigener ID.
    @Test
    void sameTitleTwiceGivesTwoFeaturesWithOwnIds() {
        List<Feature> features = parse("Titel;Owner\nPortal;Anna\nPortal;Beat");

        assertEquals(List.of("Portal", "Portal"), titles(features));
        assertEquals("Anna", features.get(0).owner());
        assertEquals("Beat", features.get(1).owner());
        assertFalse(features.get(0).id().equals(features.get(1).id()));
    }

    // --- AK 2: Trennzeichen an der Kopfzeile erkannt ---

    // Semikolon-Kopfzeile: das Komma im Titel ist Text, kein Trennzeichen.
    @Test
    void semicolonHeaderMakesCommaPartOfValue() {
        Feature feature = only("Titel;Owner\nExport, CSV;Anna");

        assertEquals("Export, CSV", feature.title());
        assertEquals("Anna", feature.owner());
    }

    // Komma-Kopfzeile: das Semikolon im Titel ist Text, kein Trennzeichen.
    @Test
    void commaHeaderMakesSemicolonPartOfValue() {
        Feature feature = only("Titel,Owner\nExport; CSV,Anna");

        assertEquals("Export; CSV", feature.title());
        assertEquals("Anna", feature.owner());
    }

    // Szenario «CSV mit Komma und Anführungszeichen wird importiert»
    @Test
    void commaSeparatedWithQuotedSeparator() {
        Feature feature = only("Titel,Beschreibung\n\"Export, CSV\",\"Für den Issue-Tracker\"");

        assertEquals("Export, CSV", feature.title());
        assertEquals("Für den Issue-Tracker", feature.description());
    }

    @Test
    void quotedFieldMayContainSemicolon() {
        Feature feature = only("Titel;Owner\n\"Export; CSV\";Anna");

        assertEquals("Export; CSV", feature.title());
        assertEquals("Anna", feature.owner());
    }

    @Test
    void quotedFieldMayContainLineBreak() {
        List<Feature> features =
                parse("Titel;Beschreibung;Owner\nPortal;\"Erste Zeile\nZweite Zeile\";Anna\nSuche;;Beat");

        assertEquals(List.of("Portal", "Suche"), titles(features));
        assertEquals("Erste Zeile\nZweite Zeile", features.get(0).description());
        assertEquals("Anna", features.get(0).owner());
    }

    // RFC 4180: "" im Feld in Anführungszeichen ist ein Anführungszeichen.
    @Test
    void doubledQuoteInQuotedFieldIsOneQuote() {
        Feature feature = only("Titel;Beschreibung\n\"Export \"\"CSV\"\"\";\"Er sagt \"\"ja\"\"; gut\"");

        assertEquals("Export \"CSV\"", feature.title());
        assertEquals("Er sagt \"ja\"; gut", feature.description());
    }

    @Test
    void quotedFieldIsTrimmed() {
        assertEquals("Portal", only("Titel\n\"  Portal  \"").title());
    }

    @Test
    void headerMayBeQuoted() {
        Feature feature = only("\"Titel\";\"Owner\"\nPortal;Anna");

        assertEquals("Portal", feature.title());
        assertEquals("Anna", feature.owner());
    }

    // Windows-Zeilenenden: «1\r» wäre keine ganze Zahl.
    @Test
    void acceptsCrLfLineEndings() {
        List<Feature> features = parse("Titel;WSJF-Rang\r\nPortal;1\r\nSuche;2\r\n");

        assertEquals(List.of("Portal", "Suche"), titles(features));
        assertEquals(OptionalInt.of(1), features.get(0).wsjfRank());
        assertEquals(OptionalInt.of(2), features.get(1).wsjfRank());
    }

    @Test
    void crLfInsideQuotedFieldStaysInValue() {
        Feature feature = only("Titel;Beschreibung\r\nPortal;\"Erste\r\nZweite\"\r\n");

        assertEquals("Portal", feature.title());
        assertEquals("Erste\r\nZweite", feature.description());
    }

    // Excel schreibt UTF-8 mit BOM; sonst hiesse die erste Spalte «﻿Titel».
    @Test
    void ignoresByteOrderMark() {
        Feature feature = only("﻿Titel;Owner\nPortal;Anna");

        assertEquals("Portal", feature.title());
        assertEquals("Anna", feature.owner());
    }

    // --- AK 3: fehlende Spalte «Titel» (Szenario «CSV ohne Spalte Titel wird abgelehnt») ---

    @Test
    void rejectsHeaderWithoutTitleColumn() {
        assertEquals(MISSING_TITLE_COLUMN, rejected("Name;Owner\nPortal;Anna"));
    }

    @Test
    void rejectsEmptyFile() {
        assertEquals(MISSING_TITLE_COLUMN, rejected(""));
    }

    // Ohne Kopfzeile ist die erste Datenzeile die Kopfzeile.
    @Test
    void rejectsFileWithoutHeader() {
        assertEquals(MISSING_TITLE_COLUMN, rejected("Portal;1;Anna;21\nSuche;2;Beat;13"));
    }

    // --- AK 3: ungültige Zeile verhindert den ganzen Import, Meldung mit Zeilennummer ---

    // Szenario «Ungültige Zeile verhindert den ganzen Import»: Die Kopfzeile ist Zeile 1.
    @Test
    void rejectsRankNullWithLineNumber() {
        assertEquals("Zeile 3: " + INVALID_RANK, rejected("Titel;WSJF-Rang\nPortal;1\nSuche;null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "1,5", "abc", "99999999999"})
    void rejectsRankThatIsNoWholeNumberFromOne(String rank) {
        assertEquals("Zeile 2: " + INVALID_RANK, rejected("Titel;WSJF-Rang\nPortal;" + rank));
    }

    @Test
    void acceptsRankOne() {
        assertEquals(OptionalInt.of(1), only("Titel;WSJF-Rang\nPortal;1").wsjfRank());
    }

    @Test
    void rejectsNegativeEstimateWithLineNumber() {
        assertEquals("Zeile 2: " + INVALID_ESTIMATE, rejected("Titel;Schätzung\nPortal;-1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "acht"})
    void rejectsEstimateThatIsNoWholeNumber(String estimate) {
        assertEquals("Zeile 2: " + INVALID_ESTIMATE, rejected("Titel;Schätzung\nPortal;" + estimate));
    }

    @Test
    void acceptsEstimateZero() {
        assertEquals(OptionalInt.of(0), only("Titel;Schätzung\nPortal;0").estimate());
    }

    @Test
    void rejectsBlankTitleWithLineNumber() {
        assertEquals("Zeile 3: " + BLANK_TITLE, rejected("Titel;Owner\nPortal;Anna\n  ;Beat"));
    }

    @Test
    void acceptsTitleOf200Characters() {
        String title = "x".repeat(MAX_TITLE);

        assertEquals(title, only("Titel\n" + title).title());
    }

    @Test
    void rejectsTitleOf201CharactersWithLineNumber() {
        assertEquals("Zeile 2: " + TITLE_TOO_LONG, rejected("Titel\n" + "x".repeat(MAX_TITLE + 1)));
    }

    // Leere Zeilen zählen mit: Die Nummer zeigt auf die Zeile in der Datei.
    @Test
    void lineNumberCountsSkippedEmptyLines() {
        assertEquals("Zeile 4: " + INVALID_RANK, rejected("Titel;WSJF-Rang\nPortal;1\n\nSuche;0"));
    }

    // Annahme Auftrag: Gezählt werden Datensätze; ein Zeilenumbruch in Anführungszeichen beginnt keinen neuen.
    @Test
    void lineNumberCountsRecordsNotLineBreaksInQuotes() {
        assertEquals(
                "Zeile 3: " + INVALID_RANK,
                rejected("Titel;Beschreibung;WSJF-Rang\nPortal;\"Erste\nZweite\";1\nSuche;;0"));
    }

    // Die erste ungültige Zeile wird gemeldet, auch wenn danach gültige folgen.
    @Test
    void reportsFirstInvalidLine() {
        assertEquals(
                "Zeile 2: " + INVALID_RANK, rejected("Titel;WSJF-Rang;Schätzung\nPortal;0;1\nSuche;1;-1\nLogin;1;1"));
    }

    @Test
    void rejectsUnclosedQuoteWithLineNumber() {
        assertEquals("Zeile 2: " + UNCLOSED_QUOTE, rejected("Titel;Owner\n\"Portal;Anna"));
    }

    // Die Nummer ist die der Zeile, in der das Anführungszeichen beginnt, auch wenn der Rest der Datei folgt.
    @Test
    void rejectsUnclosedQuoteInLaterLine() {
        assertEquals("Zeile 3: " + UNCLOSED_QUOTE, rejected("Titel;Owner\nPortal;Anna\nSuche;\"Beat\nLogin;Cem\n"));
    }

    // --- AK 5: höchstens 1000 Datenzeilen und 1 MB ---

    private static String csvWithRows(int rows) {
        StringBuilder csv = new StringBuilder("Titel\n");
        for (int row = 1; row <= rows; row++) {
            csv.append("Feature ").append(row).append('\n');
        }
        return csv.toString();
    }

    // Die Kopfzeile ist keine Datenzeile: 1000 Datenzeilen sind 1001 Zeilen in der Datei.
    @Test
    void accepts1000DataRows() {
        List<Feature> features = parse(csvWithRows(MAX_ROWS));

        assertEquals(MAX_ROWS, features.size());
        assertEquals("Feature 1", features.getFirst().title());
        assertEquals("Feature 1000", features.getLast().title());
    }

    @Test
    void rejects1001DataRows() {
        assertEquals(TOO_MANY_ROWS, rejected(csvWithRows(MAX_ROWS + 1)));
    }

    // Leere Zeilen sind keine Datenzeilen und zählen nicht zur Grenze.
    @Test
    void emptyLinesDoNotCountTowardsRowLimit() {
        assertEquals(MAX_ROWS, parse(csvWithRows(MAX_ROWS) + "\n\n").size());
    }

    // --- Randfälle des Formats (Annahmen im Nachtlauf, notes.md) ---

    @Test
    void shortRowLeavesMissingFieldsEmpty() {
        Feature feature = only("Titel;Owner;WSJF-Rang\nPortal");

        assertEquals("Portal", feature.title());
        assertEquals("", feature.owner());
        assertEquals(OptionalInt.empty(), feature.wsjfRank());
    }

    @Test
    void extraFieldsAreIgnored() {
        assertEquals("Anna", only("Titel;Owner\nPortal;Anna;zuviel").owner());
    }

    @Test
    void quoteInsideUnquotedFieldIsLiteral() {
        assertEquals(
                "Er sagt \"ja\"",
                only("Titel;Beschreibung\nPortal;Er sagt \"ja\"").description());
    }

    @Test
    void quoteAfterLeadingSpaceOpensQuotedField() {
        assertEquals("Export; CSV", only("Titel;Owner\n \"Export; CSV\";Anna").title());
    }

    @Test
    void semicolonInHeaderWinsOverComma() {
        Feature feature = only("Titel;Owner\nExport, CSV;Anna");

        assertEquals("Export, CSV", feature.title());
        assertEquals("Anna", feature.owner());
    }

    @Test
    void headerWithCommaAndSemicolonUsesSemicolon() {
        assertEquals("a,b", only("Titel;Owner,Team\na,b;Anna").title());
    }

    @Test
    void acceptsLoneCarriageReturnAsLineEnd() {
        assertEquals(List.of("Portal", "Suche"), titles(parse("Titel\rPortal\rSuche")));
    }

    @Test
    void firstColumnWithSameNameWins() {
        assertEquals("Portal", only("Titel;Titel\nPortal;Suche").title());
    }

    @Test
    void headerNamesAreTrimmed() {
        assertEquals("Anna", only(" Titel ; Owner \nPortal;Anna").owner());
    }

    @Test
    void skipsRowsOfOnlySeparatorsOrSpaces() {
        assertEquals(List.of("Portal"), titles(parse("Titel;Owner\n;;\n   \nPortal;Anna")));
    }

    @Test
    void headerOnlyGivesNoFeatures() {
        assertEquals(List.of(), parse("Titel;Owner\n"));
    }

    /**
     * Eine gültige Datei mit genau {@code bytes} Bytes in UTF-8 und weniger Zeichen als Bytes (Umlaute zählen
     * doppelt), höchstens 1000 Datenzeilen und Beschreibungen unter 5000 Zeichen.
     */
    private static String csvWithUtf8Bytes(int bytes) {
        String header = "Titel;Beschreibung\n";
        String umlauts = "ä".repeat(900);
        StringBuilder csv = new StringBuilder(header);
        int size = header.length();
        int row = 0;
        while (size + 4000 < bytes) {
            String line = "F" + (++row) + ";" + umlauts + "\n";
            csv.append(line);
            size += line.getBytes(StandardCharsets.UTF_8).length;
        }
        String last = "Rest;";
        csv.append(last).append("x".repeat(bytes - size - last.length()));
        String result = csv.toString();
        assertEquals(bytes, result.getBytes(StandardCharsets.UTF_8).length);
        assertTrue(result.length() < bytes);
        return result;
    }

    @Test
    void accepts1MegabyteInUtf8() {
        List<Feature> features = parse(csvWithUtf8Bytes(MAX_BYTES));

        assertEquals("Rest", features.getLast().title());
    }

    // Gezählt werden Bytes, nicht Zeichen: Die Datei hat weniger als 1 000 000 Zeichen.
    @Test
    void rejectsOneByteOver1Megabyte() {
        assertEquals(TOO_LARGE, rejected(csvWithUtf8Bytes(MAX_BYTES + 1)));
    }
}
