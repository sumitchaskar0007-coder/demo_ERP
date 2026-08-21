package com.collegeerp.erp.student.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.student.dto.StudentProfileResponse;
import com.collegeerp.erp.student.service.StudentProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {
    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/me")
    public ApiResponse<StudentProfileResponse> getMyProfile() {
        return ApiResponse.success(
                "Student profile retrieved successfully",
                studentProfileService.getMyProfile()
        );
    }
}
