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

public interface SectionRepository extends JpaRepository<Section, Long>, JpaSpecificationExecutor<Section> {
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
    List<Section> findByDepartmentIdAndStatus(Long departmentId, SectionStatus status);
    @EntityGraph(attributePaths = {"college", "department", "academicClass", "classTeacher"})
    List<Section> findByClassTeacherIdAndStatus(Long staffId, SectionStatus status);
}
