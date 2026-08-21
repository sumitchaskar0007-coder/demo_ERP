package com.collegeerp.erp.academic.repository;

import com.collegeerp.erp.academic.entity.AcademicClass;
import com.collegeerp.erp.academic.enums.CourseYearName;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long>, JpaSpecificationExecutor<AcademicClass> {
    long countByCollegeId(Long collegeId);
    long countByDepartmentId(Long departmentId);
    List<AcademicClass> findByCollegeIdAndDepartmentIdAndStatus(
            Long collegeId, Long departmentId, AcademicStatus status);
    @EntityGraph(attributePaths = {"college", "department"})
    @Query("""
            select academicClass
            from AcademicClass academicClass
            where academicClass.college.id = :collegeId
              and (:allDepartments = true or academicClass.department.id in :departmentIds)
              and (:departmentId is null or academicClass.department.id = :departmentId)
            order by academicClass.name
            """)
    List<AcademicClass> findScoped(
            @Param("collegeId") Long collegeId,
            @Param("departmentIds") Collection<Long> departmentIds,
            @Param("allDepartments") boolean allDepartments,
            @Param("departmentId") Long departmentId);
    boolean existsByCollegeIdAndDepartmentIdAndAcademicYearAndCodeIgnoreCase(Long collegeId, Long departmentId, String academicYear, String code);
    boolean existsByCollegeIdAndDepartmentIdAndAcademicYearAndYearName(Long collegeId, Long departmentId, String academicYear, CourseYearName yearName);
}
