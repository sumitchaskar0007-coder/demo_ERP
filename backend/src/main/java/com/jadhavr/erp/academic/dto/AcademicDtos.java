package com.jadhavr.erp.academic.dto;
import jakarta.validation.constraints.*;
import java.time.*;
public final class AcademicDtos {
 private AcademicDtos(){}
 public record MasterRequest(@NotBlank String type,@NotBlank String name,String code,Long parentId,Long secondaryParentId,Integer number,Integer capacity,String category,LocalDate startDate,LocalDate endDate,LocalTime startTime,LocalTime endTime,Boolean active){}
 public record AssignmentRequest(@NotBlank String type,@NotNull Long teacherId,Long subjectId,@NotNull Long classId,@NotNull Long sectionId,Long academicYearId){}
 public record EnrollmentRequest(@NotNull Long academicYearId,@NotNull Long studentId,@NotNull Long classId,@NotNull Long sectionId){}
 public record MasterResponse(Long id,String type,String name,String code,String details){}
}
