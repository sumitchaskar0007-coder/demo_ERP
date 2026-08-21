package com.jadhavr.erp.college.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollegePaymentQrServiceTest {
    @Mock private CollegeRepository colleges;
    @Mock private CollegeImageStorageService images;

    private CollegePaymentQrService service;

    @BeforeEach
    void setUp() {
        service = new CollegePaymentQrService(colleges, images);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void principalCannotUpdatePaymentQr() {
        authenticate(RoleName.PRINCIPAL, college());
        var file = new MockMultipartFile("file", "qr.png", "image/png", new byte[] {1});

        assertThrows(AccessDeniedException.class,
                () -> service.update(1L, "College Account", file));

        verify(colleges, never()).findById(1L);
        verify(images, never()).storePending(file, "qr-code");
    }

    @Test
    void superAdminStoresNormalizedAccountNameWithQr() {
        authenticate(RoleName.SUPER_ADMIN, null);
        College college = college();
        var file = new MockMultipartFile("file", "qr.png", "image/png", new byte[] {1});
        when(colleges.findById(1L)).thenReturn(Optional.of(college));
        when(images.storePending(file, "qr-code")).thenReturn("pending/qr.png");
        when(images.claim("pending/qr.png", 1L, "qr-code", null)).thenReturn("colleges/1/qr.png");
        when(colleges.saveAndFlush(college)).thenReturn(college);

        var result = service.update(1L, "  College ERP   Fee Account  ", file);

        assertEquals("College ERP Fee Account", college.getPaymentQrAccountName());
        assertEquals("College ERP Fee Account", result.accountName());
        assertEquals("/api/college-settings/payment-qr/image?collegeId=1", result.qrCodeUrl());
    }

    private College college() {
        College college = new College();
        college.setId(1L);
        college.setName("College ERP Senior College");
        return college;
    }

    private void authenticate(RoleName roleName, College college) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(99L);
        user.setFullName(roleName.name());
        user.setEmail(roleName.name().toLowerCase() + "@example.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        user.setCollege(college);
        user.setRoles(Set.of(role));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
