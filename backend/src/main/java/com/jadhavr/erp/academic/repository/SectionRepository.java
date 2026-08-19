package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.enums.SectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long>, JpaSpecificationExecutor<Section> {
    long countByCollegeId(Long collegeId);
    long countByDepartmentId(Long departmentId);
    boolean existsByAcademicClassIdAndAcademicYearAndCodeIgnoreCase(Long classId, String academicYear, String code);
    boolean existsByClassTeacherIdAndAcademicYearAndStatus(Long staffId, String academicYear, SectionStatus status);
    long countByAcademicClassId(Long classId);
    @Query("select s.academicClass.id, count(s.id) from Section s "
            + "where s.academicClass.id in :ids group by s.academicClass.id")
    List<Object[]> countByAcademicClassIds(@Param("ids") Collection<Long> ids);
    long countById(Long id);
    @EntityGraph(attributePaths = {"college", "department", "academicClass", "classTeacher"})
    List<Section> findByStatus(SectionStatus status);
    @EntityGraph(attributePaths = {"college", "department", "academicClass", "classTeacher"})
    List<Section> findByCollegeIdAndStatus(Long collegeId, SectionStatus status);
    @EntityGraph(attributePaths = {"college", "department", "academicClass", "classTeacher"})
    List<Section> findByCollegeIdAndDepartmentIdInAndStatus(
            Long collegeId, Collection<Long> departmentIds, SectionStatus status);
    @EntityGraph(attributePaths = {"college", "department", "academicClass", "classTeacher"})
    @Query("""
            select section
            from Section section
            where section.college.id = :collegeId
              and (:allDepartments = true or section.department.id in :departmentIds)
              and (:academicClassId is null or section.academicClass.id = :academicClassId)
            order by section.name
            """)
    List<Section> findScoped(
            @Param("collegeId") Long collegeId,
            @Param("departmentIds") Collection<Long> departmentIds,
            @Param("allDepartments") boolean allDepartments,
            @Param("academicClassId") Long academicClassId);
    @EntityGraph(attributePaths = {"college", "department", "academicClass", "classTeacher"})
    List<Section> findByDepartmentIdAndStatus(Long departmentId, SectionStatus status);
    @EntityGraph(attributePaths = {"college", "department", "academicClass", "classTeacher"})
    List<Section> findByClassTeacherIdAndStatus(Long staffId, SectionStatus status);
    @Query("""
            select s from Section s
            where s.college.id = :collegeId and s.department.id = :departmentId
              and s.academicYear = :academicYear and s.academicClass.yearName = :yearName
              and lower(s.code) = lower(:code) and s.status = :status
            """)
    Optional<Section> findRolloverTarget(@Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId, @Param("academicYear") String academicYear,
            @Param("yearName") com.jadhavr.erp.academic.enums.CourseYearName yearName,
            @Param("code") String code, @Param("status") SectionStatus status);
}
