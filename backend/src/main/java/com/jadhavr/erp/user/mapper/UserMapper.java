package com.jadhavr.erp.user.mapper;

import com.jadhavr.erp.auth.dto.AuthUserResponse;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.user.dto.UserResponse;
import com.jadhavr.erp.user.entity.User;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        College college = user.getCollege();
        return new UserResponse(user.getId(), college == null ? null : college.getId(),
                college == null ? null : college.getName(), college == null ? null : college.getCode(),
                user.getFullName(), user.getEmail(), user.getPhone(), user.getProfileImageUrl(),
                user.getAddress(), user.getBio(), user.getStatus(), roles(user),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public AuthUserResponse toAuthResponse(User user) {
        College college = user.getCollege();
        return new AuthUserResponse(user.getId(), college == null ? null : college.getId(),
                college == null ? null : college.getName(), college == null ? null : college.getCode(),
                user.getFullName(), user.getEmail(), user.getPhone(), user.getProfileImageUrl(),
                user.getAddress(), user.getBio(), user.getStatus(), roles(user));
    }

    private List<String> roles(User user) {
        return user.getRoles().stream().map(role -> role.getName().name()).sorted().toList();
    }
}
