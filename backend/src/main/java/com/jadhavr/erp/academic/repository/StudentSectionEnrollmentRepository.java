package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.StudentSectionEnrollment;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.student.entity.StudentProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentSectionEnrollmentRepository
        extends JpaRepository<StudentSectionEnrollment, Long> {

    boolean existsByStudentIdAndAcademicYearAndStatus(
            Long id, String academicYear, AcademicStatus status);

    long countBySectionIdAndStatus(Long id, AcademicStatus status);

    List<StudentSectionEnrollment> findBySectionIdAndStatus(Long id, AcademicStatus status);

    @EntityGraph(attributePaths = {"academicClass", "section"})
    Optional<StudentSectionEnrollment> findFirstByStudentAndStatus(
            StudentProfile student, AcademicStatus status);

    Optional<StudentSectionEnrollment> findByStudentIdAndAcademicYearAndStatus(
            Long id, String academicYear, AcademicStatus status);

    List<StudentSectionEnrollment> findByStudentIdInAndStatus(
            Collection<Long> ids, AcademicStatus status);

    List<StudentSectionEnrollment> findBySectionDepartmentIdAndStatus(
            Long departmentId, AcademicStatus status);

    @EntityGraph(attributePaths = {"student", "section", "academicClass", "semesterOffering",
            "semesterOffering.academicTerm", "semesterOffering.curriculumSemester"})
    List<StudentSectionEnrollment> findBySemesterOfferingAcademicTermIdAndStatus(
            Long termId, AcademicStatus status);

    boolean existsBySemesterOfferingAcademicTermIdAndStatus(
            Long termId, AcademicStatus status);

    boolean existsByStudentIdAndSemesterOfferingId(Long studentId, Long offeringId);

    long countBySectionDepartmentIdAndStatus(
            Long departmentId, AcademicStatus status);

    @EntityGraph(attributePaths = {
            "student", "section", "section.college", "section.department",
            "section.academicClass", "section.classTeacher"
    })
    @Query("""
            select enrollment
            from StudentSectionEnrollment enrollment
            where enrollment.status = :status
              and (:collegeId is null or enrollment.section.college.id = :collegeId)
              and (:departmentId is null or enrollment.section.department.id = :departmentId)
              and (:sectionId is null or enrollment.section.id = :sectionId)
              and (:classTeacherId is null or enrollment.section.classTeacher.id = :classTeacherId)
            """)
    List<StudentSectionEnrollment> findForAttendanceReport(
            @Param("status") AcademicStatus status,
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("sectionId") Long sectionId,
            @Param("classTeacherId") Long classTeacherId);

    boolean existsBySectionIdAndRollNumberIgnoreCaseAndStatusAndStudentIdNot(
            Long sectionId, String rollNumber, AcademicStatus status, Long studentId);
}
