package com.jadhavr.erp.user.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.user.dto.CreatePrincipalRequest;
import com.jadhavr.erp.user.dto.UpdatePrincipalRequest;
import com.jadhavr.erp.user.dto.UserResponse;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/super-admin/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @PostMapping("/principals")
    public ResponseEntity<ApiResponse<UserResponse>> createPrincipal(
            @Valid @RequestBody CreatePrincipalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Principal created successfully", service.createPrincipal(request)));
    }
    @PutMapping("/principals/{id}")
    public ApiResponse<UserResponse> updatePrincipal(
            @PathVariable Long id, @Valid @RequestBody UpdatePrincipalRequest request) {
        return ApiResponse.success("Principal profile updated successfully",
                service.updatePrincipal(id, request));
    }
    @GetMapping("/search")
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success("Users searched successfully",
                service.searchUsers(keyword, collegeId, role, status, page, size, sortBy, sortDir));
    }
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> byId(@PathVariable Long id) {
        return ApiResponse.success("User retrieved successfully", service.getUserById(id));
    }
    @GetMapping("/college/{collegeId}")
    public ApiResponse<List<UserResponse>> byCollege(@PathVariable Long collegeId) {
        return ApiResponse.success("College users retrieved successfully",
                service.getUsersByCollege(collegeId));
    }
    @PatchMapping("/{id}/activate")
    public ApiResponse<UserResponse> activate(@PathVariable Long id) {
        return ApiResponse.success("User activated successfully", service.activateUser(id));
    }
    @PatchMapping("/{id}/deactivate")
    public ApiResponse<UserResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success("User deactivated successfully", service.deactivateUser(id));
    }
}
