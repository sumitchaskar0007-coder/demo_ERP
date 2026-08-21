package com.collegeerp.erp.config;

import com.collegeerp.erp.attendance.dto.WeeklyAttendanceDtos.StudentAttendanceResponse;
import com.collegeerp.erp.attendance.dto.WeeklyAttendanceDtos.StudentHistoryRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CacheConfigTest {
    @Test
    void redisSerializerRoundTripsAttendanceResponsesContainingJavaTimeValues() {
        StudentAttendanceResponse response = new StudentAttendanceResponse(
                7L, "Student", "12", 100.0, "GOOD", List.of(), List.of(),
                List.of(new StudentHistoryRow(
                        LocalDate.of(2026, 8, 18), "08:30 - 09:20", "Java", "Teacher",
                        "PRESENT", null)));

        byte[] encoded = CacheConfig.redisValueSerializer().serialize(response);
        Object decoded = CacheConfig.redisValueSerializer().deserialize(encoded);

        StudentAttendanceResponse restored = assertInstanceOf(StudentAttendanceResponse.class, decoded);
        assertEquals(LocalDate.of(2026, 8, 18), restored.history().get(0).date());
    }
}
