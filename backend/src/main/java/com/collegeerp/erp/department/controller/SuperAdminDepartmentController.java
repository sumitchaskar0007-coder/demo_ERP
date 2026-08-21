
package com.collegeerp.erp.department.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.department.dto.DepartmentResponse;
import com.collegeerp.erp.department.service.DepartmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/departments")
public class SuperAdminDepartmentController {

    private final DepartmentService departmentService;

    public SuperAdminDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/college/{collegeId}/active")
    public ApiResponse<List<DepartmentResponse>> getActiveDepartmentsByCollege(
            @PathVariable Long collegeId) {
        return ApiResponse.success(
                "Active departments retrieved successfully",
                departmentService.getActiveDepartmentsByCollege(collegeId));
    }
}
