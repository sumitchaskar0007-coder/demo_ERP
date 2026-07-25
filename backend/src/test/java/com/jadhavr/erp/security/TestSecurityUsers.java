package com.jadhavr.erp.security;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Set;

public final class TestSecurityUsers {
    private TestSecurityUsers() {}

    public static CustomUserDetails details(RoleName roleName, Long userId, Long collegeId) {
        User user = new User();
        user.setId(userId);
        user.setEmail(roleName.name().toLowerCase() + "@example.test");
        user.setFullName(roleName.name());
        user.setPasswordHash("test-password-hash");
        user.setStatus(UserStatus.ACTIVE);
        if (collegeId != null) {
            College college = new College();
            college.setId(collegeId);
            user.setCollege(college);
        }
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return new CustomUserDetails(user);
    }

    public static UsernamePasswordAuthenticationToken authentication(
            RoleName roleName, Long userId, Long collegeId) {
        CustomUserDetails details = details(roleName, userId, collegeId);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
