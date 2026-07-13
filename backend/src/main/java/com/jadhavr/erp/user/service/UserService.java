package com.jadhavr.erp.user.service;

import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.user.dto.CreatePrincipalRequest;
import com.jadhavr.erp.user.dto.UpdatePrincipalRequest;
import com.jadhavr.erp.user.dto.UserResponse;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.UserStatus;
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
