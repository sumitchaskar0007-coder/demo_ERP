package com.jadhavr.erp.auth.security;

import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    @Test void generatesAndValidatesToken() {
        Role role = new Role();
        role.setName(RoleName.SUPER_ADMIN);
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@erp.com");
        user.setFullName("Super Admin");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role));
        CustomUserDetails details = new CustomUserDetails(user);
        JwtService service = new JwtService(
                "THIS_IS_A_TEST_SECRET_THAT_IS_LONG_ENOUGH_FOR_HS256_SIGNING", 60_000);
        String token = service.generateToken(details);
        assertFalse(token.isBlank());
        assertEquals("admin@erp.com", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, details));
    }
}
