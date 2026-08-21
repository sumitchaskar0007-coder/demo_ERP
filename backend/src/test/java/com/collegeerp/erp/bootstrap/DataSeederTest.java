package com.collegeerp.erp.bootstrap;

import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.repository.RoleRepository;
import com.collegeerp.erp.user.repository.UserRepository;
import com.collegeerp.erp.auth.repository.RefreshTokenRepository;
import com.collegeerp.erp.auth.security.AuthorizationSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {
    @Mock private RoleRepository roles;
    @Mock private UserRepository users;
    @Mock private PasswordEncoder encoder;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private AuthorizationSnapshotService authorizationSnapshots;

    @Test
    void repeatedBootstrapDoesNotCreateDuplicateAdministrator() {
        when(users.existsByEmail("owner@example.com")).thenReturn(true);
        DataSeeder bootstrap = bootstrap("a-secure-bootstrap-value");

        bootstrap.run();

        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void bootstrapRequiresExecutionTimeSecret() {
        assertThrows(IllegalStateException.class, () -> bootstrap("").run());
    }

    @Test
    void createsOneAdministratorWhenRoleExists() {
        Role role = new Role();
        role.setName(RoleName.SUPER_ADMIN);
        when(users.existsByEmail("owner@example.com")).thenReturn(false);
        when(roles.findByName(RoleName.SUPER_ADMIN)).thenReturn(Optional.of(role));
        when(encoder.encode("a-secure-bootstrap-value")).thenReturn("encoded");

        bootstrap("a-secure-bootstrap-value").run();

        ArgumentCaptor<com.collegeerp.erp.user.entity.User> user =
                ArgumentCaptor.forClass(com.collegeerp.erp.user.entity.User.class);
        verify(users).save(user.capture());
        assertEquals("encoded", user.getValue().getPasswordHash());
        assertEquals(1, user.getValue().getRoles().size());
    }

    private DataSeeder bootstrap(String password) {
        return new DataSeeder(
                roles,
                users,
                encoder,
                refreshTokens,
                authorizationSnapshots,
                "Platform Owner",
                "owner@example.com",
                "",
                password,
                false);
    }
}
