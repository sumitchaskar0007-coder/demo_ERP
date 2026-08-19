package com.jadhavr.erp.academic.dto;
import com.jadhavr.erp.academic.enums.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.time.*; import java.util.List;
public final class AcademicDtos { private AcademicDtos(){}
 public record CreateClass(@NotNull Long collegeId,@NotNull Long departmentId,@NotBlank @Size(max=20) String academicYear,@NotBlank @Size(min=2,max=150) String name,@NotBlank @Size(min=2,max=30) String code,@Size(max=500) String description){}
 public record CreateSection(@NotNull Long academicClassId,@NotBlank @Size(max=20) String academicYear,@NotBlank @Size(min=2,max=150) String name,@NotBlank @Size(min=2,max=30) String code,@NotNull @Positive Integer capacity){}
 public record CreateSubject(@NotNull Long academicClassId,@NotNull @Min(1) @Max(10) Integer semesterNumber,@NotBlank @Size(max=20) String academicYear,@NotBlank @Size(min=2,max=150) String name,@NotBlank @Size(min=2,max=30) String code,@Size(max=500) String description,@PositiveOrZero Integer credits,SubjectType subjectType){}
 public record UpdateSubject(@NotBlank @Size(min=2,max=150) String name,@NotBlank @Size(min=2,max=30) String code,@Size(max=500) String description,@PositiveOrZero Integer credits,SubjectType subjectType){}
 public record Assign(@NotNull Long staffProfileId){} public record AssignStudent(@NotNull Long studentProfileId){} public record StudentRosterItem(Long studentProfileId,String admissionNumber,String rollNumber,String fullName,String email,String phone){}
 public record CreateTimetable(@NotNull Long sectionId,@NotNull Long subjectId,@NotNull Long teacherId,@NotNull TimetableDay dayOfWeek,@NotNull LocalTime startTime,@NotNull LocalTime endTime,@Size(max=50) String roomNumber){}
 public record CreateAttendance(@NotNull Long sectionId,@NotNull Long subjectId,Long timetableEntryId,@NotNull @PastOrPresent LocalDate attendanceDate,@Size(max=200) String topic){}
 public record Mark(@NotEmpty List<@Valid MarkItem> records){} public record MarkItem(@NotNull Long studentProfileId,@NotNull AttendanceStatus status,@Size(max=500) String remarks){}
}
