package com.collegeerp.erp.timetable.dto;

import com.collegeerp.erp.timetable.dto.WeeklyTimetableDtos.SaveEntryRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyTimetableDtosValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsOnlyTheSupportedLectureTypes() {
        for (String type : new String[]{"THEORY", "LAB", "OTHER"}) {
            assertTrue(validator.validate(request(type)).isEmpty(), type + " should be accepted");
        }
        for (String type : new String[]{"PRACTICAL", "TUTORIAL", "LECTURE", "BREAK"}) {
            assertFalse(validator.validate(request(type)).isEmpty(), type + " should be rejected");
        }
    }

    private SaveEntryRequest request(String lectureType) {
        return new SaveEntryRequest(1L, 2L, "A-101", lectureType, null);
    }
}
