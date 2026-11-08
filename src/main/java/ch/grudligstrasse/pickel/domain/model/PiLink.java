package ch.grudligstrasse.pickel.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Ein Link am PI (etwa auf Confluence); nur {@code http} und {@code https}, damit ein Link kein Skript ausführt. */
public record PiLink(UUID id, String title, String url) {

    public static final int MAX_TITLE_LENGTH = 100;

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    public PiLink {
        Objects.requireNonNull(id, "id");
        if (title == null || title.isBlank()) {
            throw new InvalidProgramIncrementException("Der Titel darf nicht leer sein.");
        }
        title = title.strip();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new InvalidProgramIncrementException(
                    "Der Titel darf höchstens " + MAX_TITLE_LENGTH + " Zeichen lang sein.");
        }
        if (url == null || url.isBlank()) {
            throw new InvalidProgramIncrementException("Die URL darf nicht leer sein.");
        }
        // Das Schema ist laut RFC 3986 unabhängig von Gross- und Kleinschreibung; geprüft wird die URL wie gespeichert.
        String scheme = url.toLowerCase(Locale.ROOT);
        if (!scheme.startsWith(HTTP) && !scheme.startsWith(HTTPS)) {
            throw new InvalidProgramIncrementException("Die URL muss mit http:// oder https:// beginnen.");
        }
    }

    /** Neuer Link mit frischer ID. */
    public static PiLink create(String title, String url) {
        return new PiLink(UUID.randomUUID(), title, url);
    }
}
