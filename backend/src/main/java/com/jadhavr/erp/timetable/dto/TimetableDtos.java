package com.jadhavr.erp.timetable.dto;
import jakarta.validation.constraints.*; import java.time.*; import java.util.*;
public final class TimetableDtos {private TimetableDtos(){}
 public record CreateTimetableRequest(Long academicYearId,Long academicTermId,@NotNull Long classId,@NotNull Long sectionId,@NotNull LocalDate weekStart){}
 public record EntryRequest(@NotNull DayOfWeek dayOfWeek,@NotNull Long periodId,Long subjectId,Long teacherId,Long roomId,@NotBlank String type,@Size(max=500) String remarks){}
 public record UpdateEntryRequest(Long subjectId,Long teacherId,Long roomId,String type,@Size(max=500) String remarks){}
 public record CopyRequest(@NotNull Long sourceTimetableId,@NotNull LocalDate targetWeekStart,Long targetTermId){}
 public record CopyDayRequest(@NotNull DayOfWeek sourceDay,@NotNull DayOfWeek targetDay){}
 public record CopyWeekRequest(@NotNull Long sourceTimetableId,@NotNull Long targetTimetableId){}
 public record SearchRequest(String query,String subject,String teacher,String room,DayOfWeek dayOfWeek,Long divisionId){}
 public record EntryResponse(Long id,DayOfWeek dayOfWeek,Integer periodNumber,LocalTime startTime,LocalTime endTime,String type,Long subjectId,String subject,Long teacherId,String teacher,Long roomId,String room,String remarks,Long createdBy,String createdByName){}
 public record TimetableResponse(Long id,String academicYear,String term,Long classId,String className,Long sectionId,String section,LocalDate weekStart,String status,List<EntryResponse> entries){}
}
