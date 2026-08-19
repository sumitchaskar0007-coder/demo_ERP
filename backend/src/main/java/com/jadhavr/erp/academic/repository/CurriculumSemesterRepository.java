package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.CurriculumSemester;
import com.jadhavr.erp.academic.enums.CourseYearName;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumSemesterRepository extends JpaRepository<CurriculumSemester, Long> {
    List<CurriculumSemester> findByDepartmentIdOrderBySemesterNumber(Long departmentId);
    List<CurriculumSemester> findByCollegeIdAndActiveTrueOrderByDepartmentIdAscSemesterNumberAsc(Long collegeId);
    Optional<CurriculumSemester> findByDepartmentIdAndSemesterNumber(Long departmentId, Integer number);
    Optional<CurriculumSemester> findByDepartmentIdAndYearNameAndTermType(
            Long departmentId, CourseYearName yearName,
            com.jadhavr.erp.academic.enums.AcademicTermType termType);
}
