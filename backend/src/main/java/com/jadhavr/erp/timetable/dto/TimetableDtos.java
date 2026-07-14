package com.jadhavr.erp.timetable.dto;
import jakarta.validation.constraints.*; import java.time.*; import java.util.*;
public final class TimetableDtos {private TimetableDtos(){}
 public record CreateTimetableRequest(@NotNull Long academicYearId,@NotNull Long academicTermId,@NotNull Long classId,@NotNull Long sectionId,@NotNull LocalDate weekStart){}
 public record EntryRequest(@NotNull DayOfWeek dayOfWeek,@NotNull Long periodId,Long subjectId,Long teacherId,Long roomId,@NotBlank String type){}
 public record CopyRequest(@NotNull Long sourceTimetableId,@NotNull LocalDate targetWeekStart,Long targetTermId){}
 public record EntryResponse(Long id,DayOfWeek dayOfWeek,Integer periodNumber,LocalTime startTime,LocalTime endTime,String type,Long subjectId,String subject,Long teacherId,String teacher,Long roomId,String room){}
 public record TimetableResponse(Long id,String academicYear,String term,Long classId,String className,Long sectionId,String section,LocalDate weekStart,String status,List<EntryResponse> entries){}
}
