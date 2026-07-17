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
    public record EntryResponse(Long id,String dayOfWeek,Long periodId,Long subjectId,String subject,Long teacherId,String teacher,String room,String lectureType,String remarks){}
    public record TimetableResponse(Long id,Long sectionId,String college,String department,String year,String division,String classTeacher,String academicYear,String status,boolean editable,List<PeriodResponse> periods,List<EntryResponse> entries,List<Option> subjects,List<Option> teachers,List<SubjectTeacherOption> subjectTeachers,List<String> rooms){}
    public record SaveEntryRequest(@NotNull Long subjectId,@NotNull Long teacherId,@Size(max=80) String room,@NotBlank String lectureType,@Size(max=500) String remarks){}
    public record MoveEntryRequest(@NotNull String fromDay,@NotNull Long fromPeriodId,@NotNull String toDay,@NotNull Long toPeriodId){}
    public record CopyDayRequest(@NotNull String sourceDay,@NotNull String targetDay,boolean overwrite){}
    public record CopyTimetableRequest(@NotNull Long sourceTimetableId,boolean overwrite){}
    public record EntryItem(@NotNull String dayOfWeek,@NotNull Long periodId,@NotNull Long subjectId,@NotNull Long teacherId,@Size(max=80) String room,@NotBlank String lectureType,@Size(max=500) String remarks){}
    public record ReplaceEntriesRequest(@NotNull List<@Valid EntryItem> entries){}
    /** An id is supplied for an existing row; omit it to add a new period. */
    public record PeriodItem(Long id,@NotBlank @Size(max=40) String label,@NotNull LocalTime startTime,@NotNull LocalTime endTime,@NotNull WeeklyPeriod.Kind kind){}
    public record UpdatePeriodsRequest(@NotEmpty List<@Valid PeriodItem> periods){}
}
