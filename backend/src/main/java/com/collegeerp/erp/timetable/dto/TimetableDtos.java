package com.collegeerp.erp.timetable.dto;
import jakarta.validation.constraints.*; import java.time.*; import java.util.*;
public final class TimetableDtos {private TimetableDtos(){}
 public record CreateTimetableRequest(@Positive Long academicYearId,@Positive Long academicTermId,@NotNull @Positive Long classId,@NotNull @Positive Long sectionId,@NotNull LocalDate weekStart){}
 public record EntryRequest(@NotNull DayOfWeek dayOfWeek,@NotNull @Positive Long periodId,@Positive Long subjectId,@Positive Long teacherId,@Positive Long roomId,@NotBlank @Pattern(regexp="^(LECTURE|PRACTICAL|LAB|BREAK|THEORY|TUTORIAL)$") String type,@Size(max=500) String remarks){}
 public record UpdateEntryRequest(@Positive Long subjectId,@Positive Long teacherId,@Positive Long roomId,@Pattern(regexp="^(LECTURE|PRACTICAL|LAB|BREAK|THEORY|TUTORIAL)$") String type,@Size(max=500) String remarks){}
 public record CopyRequest(@NotNull @Positive Long sourceTimetableId,@NotNull LocalDate targetWeekStart,@Positive Long targetTermId){}
 public record CopyDayRequest(@NotNull DayOfWeek sourceDay,@NotNull DayOfWeek targetDay){}
 public record CopyWeekRequest(@NotNull @Positive Long sourceTimetableId,@NotNull @Positive Long targetTimetableId){}
 public record SearchRequest(@Size(max=100) String query,@Size(max=100) String subject,@Size(max=150) String teacher,@Size(max=80) String room,DayOfWeek dayOfWeek,@Positive Long divisionId){}
 public record EntryResponse(Long id,DayOfWeek dayOfWeek,Integer periodNumber,LocalTime startTime,LocalTime endTime,String type,Long subjectId,String subject,Long teacherId,String teacher,Long roomId,String room,String remarks,Long createdBy,String createdByName){}
 public record TimetableResponse(Long id,String academicYear,String term,Long classId,String className,Long sectionId,String section,LocalDate weekStart,String status,List<EntryResponse> entries){}
}
