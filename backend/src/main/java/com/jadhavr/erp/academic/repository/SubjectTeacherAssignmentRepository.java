package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.SubjectTeacherAssignment;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectTeacherAssignmentRepository
        extends JpaRepository<SubjectTeacherAssignment, Long> {

    boolean existsBySubjectIdAndStatus(Long id, AcademicStatus status);

    boolean existsBySubjectIdAndTeacherIdAndStatus(
            Long subjectId, Long teacherId, AcademicStatus status);

    Optional<SubjectTeacherAssignment> findBySubjectIdAndTeacherIdAndStatus(
            Long subjectId, Long teacherId, AcademicStatus status);

    List<SubjectTeacherAssignment> findBySubjectIdAndStatus(
            Long subjectId, AcademicStatus status);

    @EntityGraph(attributePaths = {"subject", "subject.academicClass", "teacher", "sections"})
    List<SubjectTeacherAssignment> findBySubjectDepartmentIdAndStatus(
            Long departmentId, AcademicStatus status);

    List<SubjectTeacherAssignment> findByTeacherIdAndStatus(
            Long teacherId, AcademicStatus status);

    @EntityGraph(attributePaths = {"subject", "teacher"})
    List<SubjectTeacherAssignment> findBySubjectIdInAndStatus(
            Collection<Long> subjectIds, AcademicStatus status);

    @EntityGraph(attributePaths = {"subject", "subject.college", "subject.department", "teacher"})
    @Query("""
            select assignment
            from SubjectTeacherAssignment assignment
            where assignment.subject.college.id = :collegeId
              and assignment.status = :status
              and (:allDepartments = true or assignment.subject.department.id in :departmentIds)
              and (:teacherId is null or assignment.teacher.id = :teacherId)
              and (:subjectId is null or assignment.subject.id = :subjectId)
            order by assignment.subject.code, assignment.teacher.fullName
            """)
    List<SubjectTeacherAssignment> findScoped(
            @Param("collegeId") Long collegeId,
            @Param("departmentIds") Collection<Long> departmentIds,
            @Param("allDepartments") boolean allDepartments,
            @Param("status") AcademicStatus status,
            @Param("teacherId") Long teacherId,
            @Param("subjectId") Long subjectId);
}
