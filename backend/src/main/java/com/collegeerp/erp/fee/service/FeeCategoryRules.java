package com.collegeerp.erp.fee.service;

import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.fee.enums.StudentCategory;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class FeeCategoryRules {
    private static final Set<String> RESERVED_NAMES = Set.of(
            "OPEN", "OBC", "SC", "ST", "SBC", "VJNT", "EWS", "OTHER");

    private FeeCategoryRules() {}

    public static String normalizeCustomCategory(StudentCategory category, String value) {
        String normalized = trimToNull(value);
        if (category == StudentCategory.OTHER) {
            if (normalized == null) {
                throw new BadRequestException("Custom category name is required for OTHER");
            }
            normalized = normalized.toUpperCase(Locale.ROOT);
            if (RESERVED_NAMES.contains(normalized)) {
                throw new BadRequestException(
                        normalized + " is already a standard category and cannot be used under OTHER");
            }
            return normalized;
        }
        if (normalized != null) {
            throw new BadRequestException("Custom category is allowed only for OTHER");
        }
        return null;
    }

    public static boolean isReservedCustomCategory(String value) {
        String normalized = trimToNull(value);
        return normalized != null && RESERVED_NAMES.contains(normalized.toUpperCase(Locale.ROOT));
    }

    public static String normalizeGender(String value) {
        String normalized = Objects.toString(value, "").trim().toUpperCase(Locale.ROOT);
        if (!Set.of("MALE", "FEMALE").contains(normalized)) {
            throw new BadRequestException("Gender must be MALE or FEMALE");
        }
        return normalized;
    }

    public static String normalizeAcademicYear(String value) {
        if (value == null) throw new BadRequestException("Academic year is required");
        String year = value.trim().replace('/', '-');
        String[] parts = year.split("-");
        if (parts.length != 2 || !parts[0].matches("\\d{4}") || !parts[1].matches("\\d{2}|\\d{4}")) {
            throw new BadRequestException("Academic year must use YYYY-YYYY or YYYY-YY format");
        }
        int start = Integer.parseInt(parts[0]);
        int end;
        if (parts[1].length() == 4) {
            end = Integer.parseInt(parts[1]);
        } else {
            end = (start / 100) * 100 + Integer.parseInt(parts[1]);
            if (end < start) end += 100;
        }
        if (end != start + 1) {
            throw new BadRequestException("Academic year must represent consecutive years");
        }
        return String.format("%04d-%04d", start, end);
    }

    public static List<String> academicYearVariants(String value) {
        String full = normalizeAcademicYear(value);
        return List.of(full, full.substring(0, 5) + full.substring(7));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
