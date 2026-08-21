package com.collegeerp.erp.academic.controller;

import com.collegeerp.erp.academic.dto.AdminCourseYearOption;
import com.collegeerp.erp.academic.entity.AcademicClass;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.repository.AcademicClassRepository;
import com.collegeerp.erp.common.api.ApiResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@Transactional(readOnly = true)
@RestController
@RequestMapping("/api/super-admin/academic-options")
public class SuperAdminAcademicOptionsController {
    private final AcademicClassRepository courseYears;

    public SuperAdminAcademicOptionsController(AcademicClassRepository courseYears) {
        this.courseYears = courseYears;
    }

    @GetMapping("/course-years")
    @Cacheable(cacheNames = "activeCourseYears", key = "#collegeId+':'+#departmentId", sync = true)
    public ApiResponse<List<AdminCourseYearOption>> courseYears(
            @RequestParam Long collegeId,
            @RequestParam Long departmentId) {
        List<AdminCourseYearOption> options = courseYears
                .findByCollegeIdAndDepartmentIdAndStatus(collegeId, departmentId, AcademicStatus.ACTIVE)
                .stream()
                .sorted(Comparator
                        .comparingInt((AcademicClass courseYear) -> courseYear.getYearName().ordinal())
                        .thenComparing(AcademicClass::getName))
                .map(courseYear -> new AdminCourseYearOption(
                        courseYear.getId(),
                        courseYear.getCollege().getId(),
                        courseYear.getDepartment().getId(),
                        courseYear.getName(),
                        courseYear.getYearName()))
                .toList();
        return ApiResponse.success("Active course years", options);
    }
}
