package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.StudentSectionEnrollment;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.student.entity.StudentProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
