package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long>, JpaSpecificationExecutor<AcademicClass> {
    List<AcademicClass> findByCollegeIdAndDepartmentIdAndStatus(
            Long collegeId, Long departmentId, AcademicStatus status);
    boolean existsByCollegeIdAndDepartmentIdAndAcademicYearAndCodeIgnoreCase(Long collegeId, Long departmentId, String academicYear, String code);
    boolean existsByCollegeIdAndDepartmentIdAndAcademicYearAndYearName(Long collegeId, Long departmentId, String academicYear, CourseYearName yearName);
}
