package com.jadhavr.erp.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class LectureSubstitutionDtos {
    private LectureSubstitutionDtos() {}

    public record DailySchedule(
            LocalDate date, Long teacherId, String teacherName, List<TodayLecture> lectures) {}

    public record TodayLecture(
            Long timetableEntryId, String period, LocalTime startTime, LocalTime endTime,
            Long subjectId, String subjectCode, String subject, String department,
            String year, String division, String room, String lectureType,
            SubstitutionView substitution) {}

    public record SubstitutionView(
            Long id, Long teacherId, String teacherName, Long subjectId,
            String subjectCode, String subject, String reason) {}

    public record SubjectOption(Long id, String code, String name) {}

    public record AvailableTeacher(
            Long id, String employeeCode, String name, List<SubjectOption> subjects) {}

    public record CreateSubstitutionRequest(
            @NotNull @Positive Long timetableEntryId,
            @NotNull @Positive Long substituteTeacherId,
            @NotNull @Positive Long substituteSubjectId,
            @NotBlank @Size(max = 300) String reason) {}
}
