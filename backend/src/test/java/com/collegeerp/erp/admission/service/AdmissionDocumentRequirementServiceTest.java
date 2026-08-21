package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.dto.AdmissionDocumentRequirementRequest;
import com.collegeerp.erp.admission.entity.AdmissionDocumentRequirement;
import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.repository.AdmissionDocumentRequirementRepository;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import com.collegeerp.erp.security.TestSecurityUsers;
import com.collegeerp.erp.user.entity.RoleName;
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
    @Mock private DepartmentRepository departments;
    @Mock private AdmissionFormRepository admissions;

    private AdmissionDocumentRequirementService service;
    private College college;
    private Department department;

    @BeforeEach
    void setUp() {
        service = new AdmissionDocumentRequirementService(
                requirements, colleges, departments, admissions);
        college = new College();
        college.setId(7L);
        department = new Department();
        department.setId(11L);
        department.setName("Computer Science");
        department.setCollege(college);
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.PRINCIPAL, 1L, 7L));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void principalCreatesDepartmentScopedCustomRequirement() {
        when(colleges.findById(7L)).thenReturn(Optional.of(college));
        when(departments.findById(11L)).thenReturn(Optional.of(department));
        when(requirements.findByDepartmentIdOrderByDisplayOrderAscIdAsc(11L))
                .thenReturn(List.of());
        when(requirements.save(any(AdmissionDocumentRequirement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(null, 11L,
                new AdmissionDocumentRequirementRequest("  Anti-ragging   Undertaking ", true));

        ArgumentCaptor<AdmissionDocumentRequirement> saved =
                ArgumentCaptor.forClass(AdmissionDocumentRequirement.class);
        org.mockito.Mockito.verify(requirements).save(saved.capture());
        assertEquals("Anti-ragging Undertaking", response.documentName());
        assertTrue(response.documentKey().matches("CUSTOM_[A-F0-9]{24}"));
        assertTrue(response.required());
        assertEquals(7L, saved.getValue().getCollege().getId());
        assertEquals(11L, saved.getValue().getDepartment().getId());
    }

    @Test
    void uploadKeyMustBeActiveForAdmissionCollege() {
        AdmissionForm admission = new AdmissionForm();
        admission.setCollege(college);
        admission.setDepartment(department);
        when(requirements.findByDepartmentIdOrderByDisplayOrderAscIdAsc(11L))
                .thenReturn(List.of(new AdmissionDocumentRequirement()));
        when(requirements.findByDepartmentIdAndDocumentKeyAndActiveTrue(
                11L, "CUSTOM_DISABLED")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.requireActive(admission, "custom_disabled"));
    }
}
