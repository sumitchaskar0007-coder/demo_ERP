package com.jadhavr.erp.reports.service;

import java.util.Objects;

final class CsvCell {
    private static final String FORMULA_PREFIXES = "=+-@";

    private CsvCell() {}

    static String row(Object... values) {
        StringBuilder row = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) row.append(',');
            row.append(value(values[index]));
        }
        return row.append('\n').toString();
    }

    static String value(Object value) {
        String text = Objects.toString(value, "");
        int firstVisible = 0;
        while (firstVisible < text.length() && Character.isWhitespace(text.charAt(firstVisible))) {
            firstVisible++;
        }
        if (firstVisible < text.length()
                && FORMULA_PREFIXES.indexOf(text.charAt(firstVisible)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
