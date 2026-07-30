package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionDocumentRequirementRequest;
import com.jadhavr.erp.admission.entity.AdmissionDocumentRequirement;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRequirementRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentRequirementServiceTest {
    @Mock private AdmissionDocumentRequirementRepository requirements;
    @Mock private CollegeRepository colleges;
    @Mock private AdmissionFormRepository admissions;

    private AdmissionDocumentRequirementService service;
    private College college;

    @BeforeEach
    void setUp() {
        service = new AdmissionDocumentRequirementService(requirements, colleges, admissions);
        college = new College();
        college.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.PRINCIPAL, 1L, 7L));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void principalCreatesCollegeScopedCustomRequirement() {
        when(colleges.findById(7L)).thenReturn(Optional.of(college));
        when(requirements.findByCollegeIdOrderByDisplayOrderAscIdAsc(7L))
                .thenReturn(List.of());
        when(requirements.save(any(AdmissionDocumentRequirement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(null,
                new AdmissionDocumentRequirementRequest("  Anti-ragging   Undertaking ", true));

        ArgumentCaptor<AdmissionDocumentRequirement> saved =
                ArgumentCaptor.forClass(AdmissionDocumentRequirement.class);
        org.mockito.Mockito.verify(requirements).save(saved.capture());
        assertEquals("Anti-ragging Undertaking", response.documentName());
        assertTrue(response.documentKey().matches("CUSTOM_[A-F0-9]{24}"));
        assertTrue(response.required());
        assertEquals(7L, saved.getValue().getCollege().getId());
    }

    @Test
    void uploadKeyMustBeActiveForAdmissionCollege() {
        AdmissionForm admission = new AdmissionForm();
        admission.setCollege(college);
        when(requirements.findByCollegeIdAndDocumentKeyAndActiveTrue(
                7L, "CUSTOM_DISABLED")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.requireActive(admission, "custom_disabled"));
    }
}
