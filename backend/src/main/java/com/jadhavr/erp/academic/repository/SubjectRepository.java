package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.Subject;
import com.jadhavr.erp.academic.enums.SubjectStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, Long>,
        JpaSpecificationExecutor<Subject> {

    boolean existsByAcademicClassIdAndAcademicYearAndCodeIgnoreCase(
            Long academicClassId, String academicYear, String code);

    Optional<Subject> findByAcademicClassIdAndAcademicYearAndCodeIgnoreCase(
            Long academicClassId, String academicYear, String code);

    long countByCollegeId(Long collegeId);

    long countByDepartmentId(Long departmentId);

    long countByDepartmentIdAndStatus(Long departmentId, SubjectStatus status);

    @EntityGraph(attributePaths = {"college", "department", "academicClass"})
    List<Subject> findByAcademicClassIdAndStatus(Long academicClassId, SubjectStatus status);

    @EntityGraph(attributePaths = {"college", "department", "academicClass"})
    List<Subject> findByDepartmentIdAndStatus(Long departmentId, SubjectStatus status);

    @EntityGraph(attributePaths = {"college", "department", "academicClass"})
    @Query("""
            select subject
            from Subject subject
            where subject.college.id = :collegeId
              and subject.status = :status
              and (:allDepartments = true or subject.department.id in :departmentIds)
              and (:academicClassId is null or subject.academicClass.id = :academicClassId)
              and (:departmentId is null or subject.department.id = :departmentId)
            order by subject.code
            """)
    List<Subject> findScoped(
            @Param("collegeId") Long collegeId,
            @Param("departmentIds") Collection<Long> departmentIds,
            @Param("allDepartments") boolean allDepartments,
            @Param("status") SubjectStatus status,
            @Param("academicClassId") Long academicClassId,
            @Param("departmentId") Long departmentId);
}
