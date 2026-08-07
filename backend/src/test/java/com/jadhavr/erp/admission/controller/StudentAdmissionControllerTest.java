package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionInformationValidationResponse;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.service.AdmissionDocumentService;
import com.jadhavr.erp.admission.service.AdmissionPhotoService;
import com.jadhavr.erp.admission.service.AdmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAdmissionControllerTest {
    @Mock AdmissionService admissions;
    @Mock AdmissionPhotoService photos;
    @Mock AdmissionDocumentService documents;

    @Test
    void informationValidationEndpointDelegatesWithoutSubmitting() {
        DetailedAdmissionRequest request = mock(DetailedAdmissionRequest.class);
        when(admissions.validateMyAdmissionDetails(request))
                .thenReturn(new AdmissionInformationValidationResponse(true));
        StudentAdmissionController controller =
                new StudentAdmissionController(admissions, photos, documents);

        var response = controller.validateDetails(request);

        assertTrue(response.success());
        assertTrue(response.data().valid());
        verify(admissions).validateMyAdmissionDetails(request);
    }
}
