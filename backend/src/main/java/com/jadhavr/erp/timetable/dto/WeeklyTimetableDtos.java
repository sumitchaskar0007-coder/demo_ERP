package com.jadhavr.erp.timetable.dto;

import com.jadhavr.erp.timetable.entity.WeeklyPeriod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class WeeklyTimetableDtos {
    private WeeklyTimetableDtos() {}
    public record DivisionOption(Long id,Long collegeId,Long departmentId,Long courseYearId,String department,String year,String division,String classTeacher,String academicYear,boolean editable){}
    public record Option(Long id,String label){}
    public record SubjectTeacherOption(Long subjectId,List<Option> teachers){}
    public record PeriodResponse(Long id,Integer position,String label,LocalTime startTime,LocalTime endTime,String kind){}
    public record EntryResponse(Long id,String dayOfWeek,Long periodId,Long subjectId,String subject,Long teacherId,String teacher,String room,String lectureType,String remarks,boolean substituted,String originalTeacher){}
    public record TimetableResponse(Long id,Long sectionId,String college,String department,String year,String division,String classTeacher,String academicYear,Long semesterId,Integer semesterNumber,String semesterName,String status,String reviewComment,boolean editable,List<PeriodResponse> periods,List<EntryResponse> entries,List<Option> subjects,List<Option> teachers,List<SubjectTeacherOption> subjectTeachers,List<String> rooms){}
    public record ReviewRequest(
            @NotBlank @Pattern(regexp="^(APPROVE|REJECT|REQUEST_CHANGES)$") String action,
            @Size(max=500) String comment){}
    public record SaveEntryRequest(
            @NotNull @Positive Long subjectId,
            @NotNull @Positive Long teacherId,
            @Size(max=80) String room,
            @NotBlank @Pattern(regexp="^(THEORY|LAB|OTHER)$") String lectureType,
            @Size(max=500) String remarks){}
    public record MoveEntryRequest(
            @NotBlank @Pattern(regexp="^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$") String fromDay,
            @NotNull @Positive Long fromPeriodId,
            @NotBlank @Pattern(regexp="^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$") String toDay,
            @NotNull @Positive Long toPeriodId){}
    public record CopyDayRequest(
            @NotBlank @Pattern(regexp="^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$") String sourceDay,
            @NotBlank @Pattern(regexp="^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$") String targetDay,
            boolean overwrite){}
    public record CopyTimetableRequest(@NotNull @Positive Long sourceTimetableId,boolean overwrite){}
    public record EntryItem(
            @NotBlank @Pattern(regexp="^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$") String dayOfWeek,
            @NotNull @Positive Long periodId,
            @NotNull @Positive Long subjectId,
            @NotNull @Positive Long teacherId,
            @Size(max=80) String room,
            @NotBlank @Pattern(regexp="^(THEORY|LAB|OTHER)$") String lectureType,
            @Size(max=500) String remarks){}
    public record ReplaceEntriesRequest(@NotNull @Size(max=500) List<@Valid EntryItem> entries){}
    /** An id is supplied for an existing row; omit it to add a new period. */
    public record PeriodItem(Long id,@NotBlank @Size(max=40) String label,@NotNull LocalTime startTime,@NotNull LocalTime endTime,@NotNull WeeklyPeriod.Kind kind){}
    public record UpdatePeriodsRequest(@NotEmpty @Size(max=20) List<@Valid PeriodItem> periods){}
}
