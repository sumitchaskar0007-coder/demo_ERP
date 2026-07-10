package com.jadhavr.erp.student.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.student.dto.StudentProfileResponse;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.service.AdminStudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin/students")
public class AdminStudentController {
    private final AdminStudentService adminStudentService;

    public AdminStudentController(AdminStudentService adminStudentService) {
        this.adminStudentService = adminStudentService;
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<StudentProfileResponse>> searchStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(
                "Students searched successfully",
                adminStudentService.searchStudents(
                        keyword, collegeId, departmentId, status, page, size, sortBy, sortDir)
        );
    }
}
