package com.collegeerp.erp.user.service;

import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.user.dto.CreatePrincipalRequest;
import com.collegeerp.erp.user.dto.UpdatePrincipalRequest;
import com.collegeerp.erp.user.dto.UserResponse;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.UserStatus;
import java.util.List;

public interface UserService {
    UserResponse createPrincipal(CreatePrincipalRequest request);
    UserResponse updatePrincipal(Long id, UpdatePrincipalRequest request);
    List<UserResponse> getUsersByCollege(Long collegeId);
    UserResponse getUserById(Long id);
    UserResponse activateUser(Long id);
    UserResponse deactivateUser(Long id);
    PageResponse<UserResponse> searchUsers(
            String keyword, Long collegeId, RoleName role, UserStatus status,
            int page, int size, String sortBy, String sortDir);
}
