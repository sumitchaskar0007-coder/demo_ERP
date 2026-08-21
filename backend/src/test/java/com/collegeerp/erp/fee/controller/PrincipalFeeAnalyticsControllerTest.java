package com.collegeerp.erp.fee.controller;

import com.collegeerp.erp.analytics.repository.AdminAnalyticsReadRepository;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.fee.dto.FeeCollectionRow;
import com.collegeerp.erp.fee.repository.FeePaymentRepository;
import com.collegeerp.erp.fee.repository.StudentFeeAccountRepository;
import com.collegeerp.erp.security.TestSecurityUsers;
import com.collegeerp.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrincipalFeeAnalyticsControllerTest {
    @Mock private FeePaymentRepository payments;
    @Mock private StudentFeeAccountRepository accounts;
    @Mock private AdminAnalyticsReadRepository analytics;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void collectionScopeComesOnlyFromAuthenticatedPrincipal() {
        authenticatePrincipal(25L, 7L);
        FeeCollectionRow row = new FeeCollectionRow(
                31L, "Student", "College", "Department", null, null, null,
                new BigDecimal("3000.00"), null, "UTR-31");
        when(payments.findVerifiedCollections(
                eq(7L), isNull(), isNull(), isNull(), eq(""), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(payments.sumVerifiedCollections(
                eq(7L), isNull(), isNull(), isNull(), eq(""), isNull(), isNull()))
                .thenReturn(new BigDecimal("83000.00"));

        var response = controller().collections(null, null, null, null, null, null, 0, 20);

        assertEquals(1, response.data().totalElements());
        assertEquals(new BigDecimal("83000.00"), response.data().totalAmount());
        verify(payments).findVerifiedCollections(
                eq(7L), isNull(), isNull(), isNull(), eq(""), isNull(), isNull(), any(Pageable.class));
    }

    private PrincipalFeeAnalyticsController controller() {
        return new PrincipalFeeAnalyticsController(payments, accounts, analytics);
    }

    private void authenticatePrincipal(Long userId, Long collegeId) {
        CustomUserDetails details = TestSecurityUsers.details(RoleName.PRINCIPAL, userId, collegeId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
