package com.jadhavr.erp.timetable.service;

import com.jadhavr.erp.academic.entity.Subject;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.timetable.entity.LectureSubstitution;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.entity.WeeklyTimetableEntry;
import com.jadhavr.erp.timetable.repository.LectureSubstitutionRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveLectureServiceTest {
    @Mock WeeklyTimetableEntryRepository entries;
    @Mock LectureSubstitutionRepository substitutions;
    @Mock WeeklyTimetableEntry entry;
    @Mock LectureSubstitution substitution;
    @Mock Subject substituteSubject;

    @Test
    void forwardedLectureIsRemovedFromOriginalTeacherAndAddedToSubstitute() {
        LocalDate date = LocalDate.of(2026, 8, 19);
        StaffProfile original = teacher(1L, "Original Teacher");
        StaffProfile substitute = teacher(2L, "Substitute Teacher");
        when(entry.getId()).thenReturn(10L);
        when(substitution.getId()).thenReturn(99L);
        when(substitution.getTimetableEntry()).thenReturn(entry);
        when(substitution.getLectureDate()).thenReturn(date);
        when(substitution.getOriginalTeacher()).thenReturn(original);
        when(substitution.getSubstituteTeacher()).thenReturn(substitute);
        when(substitution.getSubstituteSubject()).thenReturn(substituteSubject);

        when(entries.findByTeacherIdAndTimetableStatusAndTimetableReviewStatusAndDayOfWeekOrderByPeriodStartTime(
                1L, WeeklyTimetable.Status.ACTIVE, WeeklyTimetable.ReviewStatus.APPROVED,
                DayOfWeek.WEDNESDAY)).thenReturn(List.of(entry));
        when(entries.findByTeacherIdAndTimetableStatusAndTimetableReviewStatusAndDayOfWeekOrderByPeriodStartTime(
                2L, WeeklyTimetable.Status.ACTIVE, WeeklyTimetable.ReviewStatus.APPROVED,
                DayOfWeek.WEDNESDAY)).thenReturn(List.of());
        when(substitutions.findByTimetableEntryIdInAndLectureDateAndStatus(
                List.of(10L), date, LectureSubstitution.Status.ACTIVE)).thenReturn(List.of(substitution));
        when(substitutions.findBySubstituteTeacherIdAndLectureDateAndStatus(
                1L, date, LectureSubstitution.Status.ACTIVE)).thenReturn(List.of());
        when(substitutions.findBySubstituteTeacherIdAndLectureDateAndStatus(
                2L, date, LectureSubstitution.Status.ACTIVE)).thenReturn(List.of(substitution));

        EffectiveLectureService service = new EffectiveLectureService(entries, substitutions);

        assertTrue(service.forTeacher(original, date).isEmpty());
        var effective = service.forTeacher(substitute, date);
        assertEquals(1, effective.size());
        assertEquals(substitute, effective.get(0).teacher());
        assertEquals(substituteSubject, effective.get(0).subject());
        assertEquals(original, effective.get(0).originalTeacher());
        assertTrue(effective.get(0).substituted());
    }

    private StaffProfile teacher(Long id, String name) {
        StaffProfile profile = new StaffProfile();
        profile.setId(id);
        profile.setFullName(name);
        return profile;
    }
}
