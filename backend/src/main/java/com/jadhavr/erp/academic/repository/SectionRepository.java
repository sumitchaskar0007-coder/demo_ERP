package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.enums.SectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long>, JpaSpecificationExecutor<Section> {
    long countByCollegeId(Long collegeId);
    boolean existsByAcademicClassIdAndAcademicYearAndCodeIgnoreCase(Long classId, String academicYear, String code);
    boolean existsByClassTeacherIdAndAcademicYearAndStatus(Long staffId, String academicYear, SectionStatus status);
    long countByAcademicClassId(Long classId);
    @Query("select s.academicClass.id, count(s.id) from Section s "
            + "where s.academicClass.id in :ids group by s.academicClass.id")
    List<Object[]> countByAcademicClassIds(@Param("ids") Collection<Long> ids);
    long countById(Long id);
    List<Section> findByClassTeacherIdAndStatus(Long staffId, SectionStatus status);
}
