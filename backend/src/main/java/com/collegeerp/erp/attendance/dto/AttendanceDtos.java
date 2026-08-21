package com.collegeerp.erp.attendance.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.time.*; import java.util.*;
public final class AttendanceDtos {private AttendanceDtos(){}
 public record MarkItem(@NotNull @Positive Long studentId,@NotBlank @Pattern(regexp="^(PRESENT|ABSENT)$") String status,@Size(max=500) String remarks){}
 public record SubmitRequest(@NotEmpty @Size(max=500) List<@Valid MarkItem> attendance){}
 public record CorrectionRequest(@NotBlank @Pattern(regexp="^(PRESENT|ABSENT)$") String status,@NotBlank @Size(min=5,max=500) String reason){}
 public record DailySessionRequest(@NotNull LocalDate date,@NotNull @Positive Long classId,@NotNull @Positive Long sectionId,@NotNull @Positive Long teacherId,@Min(1) @Max(168) Integer lockAfterHours){}
 public record AttendanceItem(Long id,Long studentId,String admissionNumber,String studentName,String status,String remarks){}
 public record SessionResponse(Long id,LocalDate date,String type,String status,LocalDateTime lockAt,Long teacherId,String teacher,String className,String section,String subject,List<AttendanceItem> attendance){}
 public record ReportResponse(Long studentId,String studentName,LocalDate from,LocalDate to,long total,long present,long absent,long late,long excused,long halfDay,long leave,double percentage,boolean belowThreshold){}
}
