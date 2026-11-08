package ch.grudligstrasse.pikel.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Liest die CSV-Datei eines Feature-Imports (US-11): UTF-8, Kopfzeile mit mindestens der Spalte «Titel»,
 * Semikolon oder Komma als Trennzeichen (erkannt an der Kopfzeile), Felder nach RFC 4180. Alles oder nichts: Eine
 * ungültige Zeile verwirft den ganzen Import.
 *
 * <p>Die Zeilennummer in Meldungen zählt Datensätze ab der Kopfzeile (= Zeile 1), leere Zeilen eingeschlossen; ein
 * Zeilenumbruch in Anführungszeichen beginnt keine neue Zeile.
 */
public final class FeatureCsv {

    public static final int MAX_BYTES = 1_000_000;
    public static final int MAX_ROWS = 1000;

    private static final String TITLE = "Titel";
    private static final String DESCRIPTION = "Beschreibung";
    private static final String WSJF_RANK = "WSJF-Rang";
    private static final String OWNER = "Owner";
    private static final String ESTIMATE = "Schätzung";

    private static final char QUOTE = '"';
    // Excel schreibt UTF-8 mit Byte Order Mark; es gehört nicht zum ersten Spaltennamen.
    private static final char BYTE_ORDER_MARK = '﻿';

    private FeatureCsv() {}

    /**
     * Die Features der Datenzeilen in Dateireihenfolge, alle im PI.
     *
     * @throws InvalidFeatureException mit Zeilennummer und Grund, wenn die Datei oder eine Zeile ungültig ist
     */
    public static List<Feature> parse(UUID programIncrementId, String csv) {
        if (csv.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidFeatureException("Die Datei darf höchstens 1 MB gross sein.");
        }
        String text = !csv.isEmpty() && csv.charAt(0) == BYTE_ORDER_MARK ? csv.substring(1) : csv;
        List<Row> rows = rows(text, separator(text));
        if (rows.isEmpty()) {
            throw missingTitleColumn();
        }
        Map<String, Integer> columns = columns(rows.getFirst());
        if (!columns.containsKey(TITLE)) {
            throw missingTitleColumn();
        }
        List<Row> data = rows.subList(1, rows.size()).stream()
                .filter(row -> !row.blank())
                .toList();
        if (data.size() > MAX_ROWS) {
            throw new InvalidFeatureException("Die Datei darf höchstens " + MAX_ROWS + " Datenzeilen haben.");
        }
        return data.stream()
                .map(row -> feature(programIncrementId, columns, row))
                .toList();
    }

    private static Feature feature(UUID programIncrementId, Map<String, Integer> columns, Row row) {
        try {
            return Feature.create(
                    programIncrementId,
                    row.field(columns.get(TITLE)),
                    row.field(columns.get(DESCRIPTION)),
                    integer(row.field(columns.get(WSJF_RANK)), Feature.INVALID_WSJF_RANK),
                    row.field(columns.get(OWNER)),
                    integer(row.field(columns.get(ESTIMATE)), Feature.INVALID_ESTIMATE));
        } catch (InvalidFeatureException e) {
            throw new InvalidFeatureException(atLine(row.line()) + e.getMessage());
        }
    }

    // Leer heisst «fehlt»; alles andere muss eine ganze Zahl sein, die Grenzen prüft Feature.
    private static Integer integer(String value, String invalid) {
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(stripped);
        } catch (NumberFormatException e) {
            throw new InvalidFeatureException(invalid);
        }
    }

    // Name → erste Spalte mit diesem Namen; unbekannte Spalten stehen auch darin und werden nie gelesen.
    private static Map<String, Integer> columns(Row header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.fields().size(); i++) {
            columns.putIfAbsent(header.fields().get(i).strip(), i);
        }
        return columns;
    }

    // Excel in der Schweiz schreibt Semikolon; kommt es in der Kopfzeile nicht vor, gilt das Komma.
    private static char separator(String text) {
        int end = 0;
        while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
            end++;
        }
        return text.substring(0, end).indexOf(';') >= 0 ? ';' : ',';
    }

    /** Zerlegt den Text nach RFC 4180 in Datensätze; ein Anführungszeichen öffnet nur am Anfang eines Felds. */
    private static List<Row> rows(String text, char separator) {
        List<Row> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (quoted) {
                if (c != QUOTE) {
                    field.append(c);
                } else if (i + 1 < text.length() && text.charAt(i + 1) == QUOTE) {
                    field.append(QUOTE);
                    i++;
                } else {
                    quoted = false;
                }
            } else if (c == QUOTE && field.toString().isBlank()) {
                quoted = true;
                field.setLength(0);
            } else if (c == separator) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                fields.add(field.toString());
                field.setLength(0);
                rows.add(new Row(line++, List.copyOf(fields)));
                fields.clear();
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                field.append(c);
            }
            i++;
        }
        if (quoted) {
            throw new InvalidFeatureException(atLine(line) + "Das Anführungszeichen ist nicht geschlossen.");
        }
        if (!fields.isEmpty() || !field.isEmpty()) {
            fields.add(field.toString());
            rows.add(new Row(line, List.copyOf(fields)));
        }
        return rows;
    }

    private static String atLine(int line) {
        return "Zeile " + line + ": ";
    }

    private static InvalidFeatureException missingTitleColumn() {
        return new InvalidFeatureException("Die Kopfzeile braucht die Spalte «Titel».");
    }

    private record Row(int line, List<String> fields) {

        boolean blank() {
            return fields.stream().allMatch(String::isBlank);
        }

        /** Der Wert der Spalte; leer, wenn die Spalte fehlt oder die Zeile kürzer ist. */
        String field(Integer column) {
            return column == null || column >= fields.size() ? "" : fields.get(column);
        }
    }
}
