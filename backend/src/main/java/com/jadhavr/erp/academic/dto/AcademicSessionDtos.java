package com.jadhavr.erp.academic.dto;

import com.jadhavr.erp.academic.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AcademicSessionDtos {
    private AcademicSessionDtos() {}

    public record AcademicYearView(Long id, String name, LocalDate startDate, LocalDate endDate,
            AcademicYearStatus status, List<AcademicTermView> terms) {}
    public record AcademicTermView(Long id, Long academicYearId, String name,
            AcademicTermType termType, LocalDate startDate, LocalDate endDate,
            AcademicTermStatus status) {}
    public record SemesterView(Long id, Long departmentId, String departmentName,
            int semesterNumber, CourseYearName yearName, AcademicTermType termType,
            String name, boolean active) {}
    public record OfferingView(Long id, Long academicYearId, String academicYear,
            Long academicTermId, AcademicTermType termType, Long departmentId,
            String departmentName, Long curriculumSemesterId, int semesterNumber,
            CourseYearName yearName, SemesterOfferingStatus status) {}
    public record AcademicContext(Long academicYearId, String academicYear,
            Long academicTermId, String termName, AcademicTermType termType,
            LocalDate termStartDate, LocalDate termEndDate, boolean transitionDue) {}

    public record TermDates(@NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
    public record CreateAcademicYearRequest(
            @NotBlank @Pattern(regexp="^\\d{4}[-/]\\d{2,4}$") String name,
            @NotNull LocalDate startDate, @NotNull LocalDate endDate,
            @NotNull @Valid TermDates oddTerm, @NotNull @Valid TermDates evenTerm) {}
    public record UpdateAcademicYearRequest(
            @NotBlank @Pattern(regexp="^\\d{4}[-/]\\d{2,4}$") String name,
            @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
    public record UpdateAcademicTermRequest(@NotBlank @Size(max=80) String name,
            @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
    public record ActivateAcademicTermRequest(boolean overrideDate,
            @Size(max=500) String reason) {}
    public record ConfigureSemestersRequest(@NotNull Long departmentId,
            @Min(1) @Max(5) int durationYears) {}

    public record RolloverStudentPreview(Long enrollmentId, Long studentId, String studentName,
            String sourceDivision, int sourceSemester, Integer targetSemester,
            String decision, String reason) {}
    public record RolloverPreview(Long sourceTermId, Long targetTermId, int totalStudents,
            int promotableStudents, int graduatingStudents, int blockedStudents,
            boolean targetDivisionMappingRequired, List<RolloverStudentPreview> students) {}
    public record ExecuteRolloverRequest(@NotNull Long sourceTermId, @NotNull Long targetTermId,
            Map<Long,Long> targetSectionBySourceSection, Set<Long> holdStudentIds,
            @NotBlank String confirmation) {}
    public record RolloverResult(Long jobId, SemesterRolloverStatus status, int totalStudents,
            int promotedStudents, int heldStudents, int graduatedStudents) {}
}
