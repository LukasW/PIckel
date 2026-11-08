package ch.grudligstrasse.pikel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PiLinkTest {

    private static final String URL = "https://wiki.example.ch/vision";
    private static final String SCHEME_MESSAGE = "Die URL muss mit http:// oder https:// beginnen.";
    private static final String BLANK_TITLE_MESSAGE = "Der Titel darf nicht leer sein.";
    private static final String TOO_LONG_TITLE_MESSAGE = "Der Titel darf höchstens 100 Zeichen lang sein.";
    private static final String BLANK_URL_MESSAGE = "Die URL darf nicht leer sein.";

    @Test
    void createKeepsTitleAndUrlAndAssignsId() {
        PiLink link = PiLink.create("Vision", URL);

        assertNotNull(link.id());
        assertEquals("Vision", link.title());
        assertEquals(URL, link.url());
    }

    @Test
    void createAssignsFreshIdForEachLink() {
        assertNotEquals(
                PiLink.create("Vision", URL).id(), PiLink.create("Vision", URL).id());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://wiki.example.ch/vision", "http://wiki.example.ch/vision"})
    void acceptsHttpAndHttps(String url) {
        assertEquals(url, PiLink.create("Vision", url).url());
    }

    // Annahme US-8: Das Schema ist nach RFC 3986 unabhängig von Gross- und Kleinschreibung; der Browser öffnet
    // «HTTPS://…» wie «https://…», ein Skript entsteht daraus nicht.
    @ParameterizedTest
    @ValueSource(strings = {"HTTPS://wiki.example.ch/vision", "Http://wiki.example.ch/vision"})
    void acceptsSchemeInUpperCase(String url) {
        assertEquals(url, PiLink.create("Vision", url).url());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "javascript:alert(1)",
                "JAVASCRIPT:alert(1)",
                "ftp://wiki.example.ch/vision",
                "data:text/html,<script>alert(1)</script>",
                "wiki.example.ch/vision",
                "https:wiki.example.ch/vision",
                "httpsx://wiki.example.ch/vision",
                "//wiki.example.ch/vision",
                " javascript:alert(1)"
            })
    void rejectsUrlWithoutHttpOrHttps(String url) {
        InvalidProgramIncrementException exception =
                assertThrows(InvalidProgramIncrementException.class, () -> PiLink.create("Vision", url));

        assertEquals(SCHEME_MESSAGE, exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingUrl(String url) {
        InvalidProgramIncrementException exception =
                assertThrows(InvalidProgramIncrementException.class, () -> PiLink.create("Vision", url));

        assertEquals(BLANK_URL_MESSAGE, exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingTitle(String title) {
        InvalidProgramIncrementException exception =
                assertThrows(InvalidProgramIncrementException.class, () -> PiLink.create(title, URL));

        assertEquals(BLANK_TITLE_MESSAGE, exception.getMessage());
    }

    @Test
    void acceptsTitleOfMaximumLength() {
        String title = "x".repeat(PiLink.MAX_TITLE_LENGTH);

        assertEquals(title, PiLink.create(title, URL).title());
    }

    @Test
    void rejectsTitleLongerThan100Characters() {
        String title = "x".repeat(101);

        InvalidProgramIncrementException exception =
                assertThrows(InvalidProgramIncrementException.class, () -> PiLink.create(title, URL));

        assertEquals(TOO_LONG_TITLE_MESSAGE, exception.getMessage());
    }

    // Annahme US-8: Der Titel wird wie der Name des PI ohne Leerzeichen am Rand gespeichert und gemessen.
    @Test
    void titleIsStrippedLikeTheNameOfAProgramIncrement() {
        PiLink link = PiLink.create("  " + "x".repeat(PiLink.MAX_TITLE_LENGTH) + "  ", URL);

        assertEquals("x".repeat(PiLink.MAX_TITLE_LENGTH), link.title());
    }

    // Ein Link aus der Datenbank geht durch denselben Konstruktor; ein ungültiger darf auch dort nicht entstehen.
    @Test
    void constructorRejectsUrlWithoutHttpOrHttps() {
        UUID id = UUID.randomUUID();

        InvalidProgramIncrementException exception = assertThrows(
                InvalidProgramIncrementException.class, () -> new PiLink(id, "Vision", "javascript:alert(1)"));

        assertEquals(SCHEME_MESSAGE, exception.getMessage());
    }

    @Test
    void constructorKeepsGivenId() {
        UUID id = UUID.randomUUID();

        assertEquals(id, new PiLink(id, "Vision", URL).id());
    }
}
