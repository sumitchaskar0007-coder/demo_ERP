package com.collegeerp.erp.user.mapper;

import com.collegeerp.erp.auth.dto.AuthUserResponse;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.user.dto.UserResponse;
import com.collegeerp.erp.user.entity.User;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        College college = user.getCollege();
        return new UserResponse(user.getId(), college == null ? null : college.getId(),
                college == null ? null : college.getName(), college == null ? null : college.getCode(),
                user.getFullName(), user.getEmail(), user.getPhone(), profileImageUrl(user),
                user.getAddress(), user.getBio(), user.getStatus(), roles(user),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public AuthUserResponse toAuthResponse(User user) {
        College college = user.getCollege();
        return new AuthUserResponse(user.getId(), college == null ? null : college.getId(), college == null ? null : college.getId(),
                college == null ? null : college.getName(), college == null ? null : college.getCode(),
                user.getFullName(), user.getEmail(), user.getPhone(), profileImageUrl(user),
                user.getAddress(), user.getBio(), user.getStatus(), roles(user),
                user.isMustChangePassword(), user.isEmailVerified());
    }

    private List<String> roles(User user) {
        return user.getRoles().stream().map(role -> role.getName().name()).sorted().toList();
    }

    private String profileImageUrl(User user) {
        return user.getProfileImageUrl() == null ? null : "/api/auth/profile/photo";
    }
}
