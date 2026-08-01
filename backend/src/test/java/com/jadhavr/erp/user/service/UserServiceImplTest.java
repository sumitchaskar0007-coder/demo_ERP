package com.jadhavr.erp.user.service;

import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.user.dto.CreatePrincipalRequest;
import com.jadhavr.erp.user.entity.*;
import com.jadhavr.erp.user.mapper.UserMapper;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import com.jadhavr.erp.auth.repository.RefreshTokenRepository;
import com.jadhavr.erp.auth.security.AuthorizationSnapshotService;
import com.jadhavr.erp.auth.security.AuthorizationStateUnavailableException;
import com.jadhavr.erp.email.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock CollegeRepository colleges;
    @Mock PasswordEncoder encoder;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock AuthorizationSnapshotService authorizationSnapshots;
    @Mock EmailNotificationService emailNotifications;
    UserServiceImpl service;
    College college;
    Role principalRole;

    @BeforeEach void setup() {
        service = new UserServiceImpl(users, roles, colleges, encoder, new UserMapper(),
                refreshTokens, authorizationSnapshots);
        service.setEmailNotifications(emailNotifications);
        college = college(CollegeStatus.ACTIVE);
        principalRole = new Role();
        principalRole.setName(RoleName.PRINCIPAL);
    }

    @Test void createPrincipalSuccessAndEncodesPassword() {
        stubValidCreation();
        when(encoder.encode(anyString())).thenReturn("$2a$encoded");
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.createPrincipal(request());
        assertEquals("principal@abc.com", result.email());
        assertEquals(List.of("PRINCIPAL"), result.roles());
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(encoder).encode(password.capture());
        assertNotEquals("9876543210", password.getValue());
        assertEquals(20, password.getValue().length());
        verify(emailNotifications).queuePrincipalCreatedEmail(
                any(User.class), eq(password.getValue()));
    }

    @Test void missingCollegeFails() {
        when(colleges.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createPrincipal(request()));
    }

    @Test void inactiveCollegeFails() {
        when(colleges.findById(1L)).thenReturn(Optional.of(college(CollegeStatus.INACTIVE)));
        assertThrows(BadRequestException.class, () -> service.createPrincipal(request()));
    }

    @Test void duplicateEmailFails() {
        when(colleges.findById(1L)).thenReturn(Optional.of(college));
        when(users.existsByEmail("principal@abc.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.createPrincipal(request()));
    }

    @Test void existingActivePrincipalFails() {
        when(colleges.findById(1L)).thenReturn(Optional.of(college));
        when(users.existsByCollegeIdAndRolesNameAndStatus(
                1L, RoleName.PRINCIPAL, UserStatus.ACTIVE)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.createPrincipal(request()));
    }

    @Test void deactivateChangesStatus() {
        User user = principal(UserStatus.ACTIVE);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        assertEquals(UserStatus.INACTIVE, service.deactivateUser(2L).status());
    }

    @Test void authorizationInvalidationFailureAbortsDeactivationPath() {
        User user = principal(UserStatus.ACTIVE);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        doThrow(new AuthorizationStateUnavailableException(
                "unavailable", new IllegalStateException("redis unavailable")))
                .when(authorizationSnapshots).invalidateOrThrow(2L);

        assertThrows(
                AuthorizationStateUnavailableException.class,
                () -> service.deactivateUser(2L));

        verify(refreshTokens).revokeAllForUser(2L);
        assertEquals(1L, user.getSessionVersion());
    }

    @Test void activatingPrincipalPreventsDuplicate() {
        User user = principal(UserStatus.INACTIVE);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(users.existsByCollegeIdAndRolesNameAndStatus(
                1L, RoleName.PRINCIPAL, UserStatus.ACTIVE)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.activateUser(2L));
    }

    @Test void activatePrincipalSucceedsWithoutDuplicate() {
        User user = principal(UserStatus.INACTIVE);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        assertEquals(UserStatus.ACTIVE, service.activateUser(2L).status());
    }

    @Test @SuppressWarnings("unchecked")
    void searchReturnsPage() {
        when(users.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(principal(UserStatus.ACTIVE))));
        var result = service.searchUsers("principal", 1L, RoleName.PRINCIPAL,
                UserStatus.ACTIVE, 0, 10, "createdAt", "desc");
        assertEquals(1, result.totalElements());
    }

    private void stubValidCreation() {
        when(colleges.findById(1L)).thenReturn(Optional.of(college));
        when(roles.findByName(RoleName.PRINCIPAL)).thenReturn(Optional.of(principalRole));
    }
    private CreatePrincipalRequest request() {
        return new CreatePrincipalRequest(1L, "Dr Principal", " Principal@ABC.com ",
                "9876543210");
    }
    private College college(CollegeStatus status) {
        College item = new College();
        item.setId(1L); item.setName("ABC"); item.setCode("ABC001"); item.setStatus(status);
        return item;
    }
    private User principal(UserStatus status) {
        User user = new User();
        user.setId(2L); user.setCollege(college); user.setFullName("Dr Principal");
        user.setEmail("principal@abc.com"); user.setPhone("9876543210");
        user.setPasswordHash("$2a$encoded"); user.setStatus(status);
        user.setRoles(Set.of(principalRole));
        return user;
    }
}
