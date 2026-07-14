package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.enums.CourseYearName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long>, JpaSpecificationExecutor<AcademicClass> {
    boolean existsByCollegeIdAndDepartmentIdAndAcademicYearAndCodeIgnoreCase(Long collegeId, Long departmentId, String academicYear, String code);
    boolean existsByCollegeIdAndDepartmentIdAndAcademicYearAndYearName(Long collegeId, Long departmentId, String academicYear, CourseYearName yearName);
}
